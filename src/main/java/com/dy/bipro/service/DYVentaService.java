package com.dy.bipro.service;

import com.dy.bipro.dto.DYLineaVenta;
import com.dy.bipro.exception.DYNegocioException;
import com.dy.bipro.model.*;
import com.dy.bipro.repository.DYClienteRepository;
import com.dy.bipro.repository.DYMovimientoStockRepository;
import com.dy.bipro.repository.DYProductoRepository;
import com.dy.bipro.repository.DYVentaRepository;
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
public class DYVentaService {
    private final DYVentaRepository repository;
    private final DYProductoRepository productoRepository;
    private final DYClienteRepository clienteRepository;
    private final DYMovimientoStockRepository movimientoRepository;
    private final DYCajaService cajaService;
    private final DYSesion sesion;

    public List<DYVenta> ventasDelDia(LocalDate dia) {
        return repository.findByFechaBetweenOrderByFechaDesc(dia.atStartOfDay(), dia.atTime(LocalTime.MAX));
    }

    public DYVenta buscarConDetalles(Long id) {
        return repository.buscarConDetalles(id).orElseThrow(() -> new DYNegocioException("La venta no existe."));
    }

    @Transactional
    public DYVenta registrar(DYCliente cliente, DYMetodoPago metodo, List<DYLineaVenta> lineas) {
        DYTurnoCaja turno = cajaService.turnoAbierto()
                .orElseThrow(() -> new DYNegocioException("Abre la caja antes de registrar ventas."));
        if (metodo == null) throw new DYNegocioException("Seleccione el método de pago.");
        if (lineas == null || lineas.isEmpty()) throw new DYNegocioException("Agrega al menos un producto a la venta.");

        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (DYLineaVenta l : lineas) {
            if (l.producto() == null) throw new DYNegocioException("Hay una línea sin producto.");
            if (l.cantidad() <= 0) throw new DYNegocioException("La cantidad debe ser mayor que 0.");
            cantidades.merge(l.producto().getId(), l.cantidad(), Integer::sum);
        }

        List<String> errores = new ArrayList<>();
        Map<Long, DYProducto> productos = new HashMap<>();
        cantidades.forEach((id, cant) -> {
            DYProducto p = productoRepository.findById(id).orElseThrow();
            productos.put(id, p);
            if (cant > p.getStockActual())
                errores.add("Stock insuficiente para " + p.getNombre() + ": disponible "
                        + p.getStockActual() + ", solicitado " + cant + ".");
        });
        if (!errores.isEmpty()) throw new DYNegocioException(String.join("\n", errores));

        DYVenta venta = DYVenta.builder()
                .numero(siguienteNumero()).fecha(LocalDateTime.now())
                .cliente(cliente).usuario(sesion.getUsuario()).turno(turno)
                .metodoPago(metodo).estado(DYEstadoVenta.EMITIDA).total(BigDecimal.ZERO).build();

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            DYProducto p = productos.get(e.getKey());
            int cant = e.getValue();
            venta.agregarDetalle(DYDetalleVenta.builder()
                    .producto(p).cantidad(cant)
                    .precioUnitario(p.getPrecioVenta()).costoUnitario(p.getPrecioCompra()).build());
            total = total.add(p.getPrecioVenta().multiply(BigDecimal.valueOf(cant)));

            p.setStockActual(p.getStockActual() - cant);
            productoRepository.save(p);
            movimientoRepository.save(DYMovimientoStock.builder()
                    .fecha(venta.getFecha()).producto(p).tipo(DYTipoMovimiento.VENTA)
                    .cantidad(-cant).saldo(p.getStockActual()).referencia(venta.getNumero()).build());
        }
        venta.setTotal(total);

        if (cliente != null) {
            cliente.setNumeroCompras(cliente.getNumeroCompras() + 1);
            cliente.setTotalComprado(cliente.getTotalComprado().add(total));
            cliente.setUltimaCompra(LocalDate.now());
            clienteRepository.save(cliente);
        }

        DYVenta guardada = repository.save(venta);
        log.info("Venta registrada: {} total={}", guardada.getNumero(), guardada.getTotal());
        return guardada;
    }

    @Transactional
    public DYVenta anular(Long id) {
        sesion.exigirAdmin();
        DYVenta v = buscarConDetalles(id);
        if (v.getEstado() == DYEstadoVenta.ANULADA) throw new DYNegocioException("La venta ya está anulada.");
        for (DYDetalleVenta d : v.getDetalles()) {
            DYProducto p = d.getProducto();
            p.setStockActual(p.getStockActual() + d.getCantidad());
            productoRepository.save(p);
            movimientoRepository.save(DYMovimientoStock.builder()
                    .fecha(LocalDateTime.now()).producto(p).tipo(DYTipoMovimiento.ANULACION)
                    .cantidad(d.getCantidad()).saldo(p.getStockActual()).referencia(v.getNumero()).build());
        }
        DYCliente c = v.getCliente();
        if (c != null) {
            c.setNumeroCompras(Math.max(0, c.getNumeroCompras() - 1));
            c.setTotalComprado(c.getTotalComprado().subtract(v.getTotal()).max(BigDecimal.ZERO));
            clienteRepository.save(c);
        }
        v.setEstado(DYEstadoVenta.ANULADA);
        return repository.save(v);
    }

    private String siguienteNumero() {
        long n = repository.findTopByOrderByIdDesc().map(DYVenta::getId).orElse(0L) + 1;
        return String.format("V-%06d", n);
    }
}
