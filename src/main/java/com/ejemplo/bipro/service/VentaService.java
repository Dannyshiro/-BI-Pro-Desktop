package com.ejemplo.bipro.service;

import com.ejemplo.bipro.dto.LineaVenta;
import com.ejemplo.bipro.exception.NegocioException;
import com.ejemplo.bipro.model.*;
import com.ejemplo.bipro.repository.ClienteRepository;
import com.ejemplo.bipro.repository.MovimientoStockRepository;
import com.ejemplo.bipro.repository.ProductoRepository;
import com.ejemplo.bipro.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VentaService {
    private final VentaRepository repository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final MovimientoStockRepository movimientoRepository;
    private final CajaService cajaService;
    private final Sesion sesion;

    public List<Venta> ventasDelDia(LocalDate dia) {
        return repository.findByFechaBetweenOrderByFechaDesc(dia.atStartOfDay(), dia.atTime(LocalTime.MAX));
    }

    public Venta buscarConDetalles(Long id) {
        return repository.buscarConDetalles(id).orElseThrow(() -> new NegocioException("La venta no existe."));
    }

    @Transactional
    public Venta registrar(Cliente cliente, MetodoPago metodo, List<LineaVenta> lineas) {
        TurnoCaja turno = cajaService.turnoAbierto()
                .orElseThrow(() -> new NegocioException("Abre la caja antes de registrar ventas."));
        if (metodo == null) throw new NegocioException("Seleccione el método de pago.");
        if (lineas == null || lineas.isEmpty()) throw new NegocioException("Agrega al menos un producto a la venta.");

        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (LineaVenta l : lineas) {
            if (l.producto() == null) throw new NegocioException("Hay una línea sin producto.");
            if (l.cantidad() <= 0) throw new NegocioException("La cantidad debe ser mayor que 0.");
            cantidades.merge(l.producto().getId(), l.cantidad(), Integer::sum);
        }

        List<String> errores = new ArrayList<>();
        Map<Long, Producto> productos = new HashMap<>();
        cantidades.forEach((id, cant) -> {
            Producto p = productoRepository.findById(id).orElseThrow();
            productos.put(id, p);
            if (cant > p.getStockActual())
                errores.add("Stock insuficiente para " + p.getNombre() + ": disponible "
                        + p.getStockActual() + ", solicitado " + cant + ".");
        });
        if (!errores.isEmpty()) throw new NegocioException(String.join("\n", errores));

        Venta venta = Venta.builder()
                .numero(siguienteNumero()).fecha(LocalDateTime.now())
                .cliente(cliente).usuario(sesion.getUsuario()).turno(turno)
                .metodoPago(metodo).estado(EstadoVenta.EMITIDA).total(BigDecimal.ZERO).build();

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            Producto p = productos.get(e.getKey());
            int cant = e.getValue();
            venta.agregarDetalle(DetalleVenta.builder()
                    .producto(p).cantidad(cant)
                    .precioUnitario(p.getPrecioVenta()).costoUnitario(p.getPrecioCompra()).build());
            total = total.add(p.getPrecioVenta().multiply(BigDecimal.valueOf(cant)));

            p.setStockActual(p.getStockActual() - cant);
            productoRepository.save(p);
            movimientoRepository.save(MovimientoStock.builder()
                    .fecha(venta.getFecha()).producto(p).tipo(TipoMovimiento.VENTA)
                    .cantidad(-cant).saldo(p.getStockActual()).referencia(venta.getNumero()).build());
        }
        venta.setTotal(total);

        if (cliente != null) {
            cliente.setNumeroCompras(cliente.getNumeroCompras() + 1);
            cliente.setTotalComprado(cliente.getTotalComprado().add(total));
            cliente.setUltimaCompra(LocalDate.now());
            clienteRepository.save(cliente);
        }

        Venta guardada = repository.save(venta);
        log.info("Venta registrada: {} total={}", guardada.getNumero(), guardada.getTotal());
        return guardada;
    }

    @Transactional
    public Venta anular(Long id) {
        sesion.exigirAdmin();
        Venta v = buscarConDetalles(id);
        if (v.getEstado() == EstadoVenta.ANULADA) throw new NegocioException("La venta ya está anulada.");
        for (DetalleVenta d : v.getDetalles()) {
            Producto p = d.getProducto();
            p.setStockActual(p.getStockActual() + d.getCantidad());
            productoRepository.save(p);
            movimientoRepository.save(MovimientoStock.builder()
                    .fecha(LocalDateTime.now()).producto(p).tipo(TipoMovimiento.ANULACION)
                    .cantidad(d.getCantidad()).saldo(p.getStockActual()).referencia(v.getNumero()).build());
        }
        Cliente c = v.getCliente();
        if (c != null) {
            c.setNumeroCompras(Math.max(0, c.getNumeroCompras() - 1));
            c.setTotalComprado(c.getTotalComprado().subtract(v.getTotal()).max(BigDecimal.ZERO));
            clienteRepository.save(c);
        }
        v.setEstado(EstadoVenta.ANULADA);
        return repository.save(v);
    }

    private String siguienteNumero() {
        long n = repository.findTopByOrderByIdDesc().map(Venta::getId).orElse(0L) + 1;
        return String.format("V-%06d", n);
    }
}
