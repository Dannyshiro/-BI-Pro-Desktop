package com.ejemplo.bipro.service;

import com.ejemplo.bipro.dto.Sugerencia;
import com.ejemplo.bipro.exception.NegocioException;
import com.ejemplo.bipro.model.*;
import com.ejemplo.bipro.repository.DetalleOrdenRepository;
import com.ejemplo.bipro.repository.MovimientoStockRepository;
import com.ejemplo.bipro.repository.OrdenCompraRepository;
import com.ejemplo.bipro.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompraService {
    private final OrdenCompraRepository repository;
    private final DetalleOrdenRepository detalleRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoRepository;
    private final Sesion sesion;

    public List<OrdenCompra> listar() {
        return repository.listarConDetalles();
    }

    public Set<Long> productosConOrdenPendiente() {
        return new HashSet<>(detalleRepository.productosConOrden(EstadoOrden.PENDIENTE));
    }

    public List<Sugerencia> sugerencia() {
        Set<Long> enOrden = productosConOrdenPendiente();
        return productoRepository.findAllByOrderByNombreAsc().stream()
                .filter(p -> p.getStockActual() < p.getStockMinimo() && !enOrden.contains(p.getId()))
                .map(p -> new Sugerencia(p, p.getCategoria().getProveedor(), p.getStockMaximo() - p.getStockActual()))
                .toList();
    }

    @Transactional
    public List<OrdenCompra> generarOrdenes() {
        sesion.exigirAdmin();
        List<Sugerencia> sugerencias = sugerencia();
        if (sugerencias.isEmpty()) throw new NegocioException("No hay productos pendientes de reposición.");
        List<String> sinProveedor = sugerencias.stream().filter(s -> s.proveedor() == null)
                .map(s -> s.producto().getCategoria().getNombre()).distinct().toList();
        if (!sinProveedor.isEmpty())
            throw new NegocioException("Asigna un proveedor a estas categorías: " + String.join(", ", sinProveedor));

        Map<Proveedor, List<Sugerencia>> porProveedor = sugerencias.stream()
                .collect(Collectors.groupingBy(Sugerencia::proveedor, LinkedHashMap::new, Collectors.toList()));

        long siguiente = repository.findTopByOrderByIdDesc().map(OrdenCompra::getId).orElse(0L);
        List<OrdenCompra> creadas = new ArrayList<>();
        for (Map.Entry<Proveedor, List<Sugerencia>> e : porProveedor.entrySet()) {
            siguiente++;
            OrdenCompra o = OrdenCompra.builder()
                    .numero(String.format("OC-%d-%04d", LocalDate.now().getYear(), siguiente))
                    .fecha(LocalDate.now()).proveedor(e.getKey()).estado(EstadoOrden.PENDIENTE).build();
            for (Sugerencia s : e.getValue()) {
                o.agregarDetalle(DetalleOrden.builder().producto(s.producto())
                        .cantidad(s.cantidad()).costoUnitario(s.producto().getPrecioCompra()).build());
            }
            creadas.add(repository.save(o));
        }
        return creadas;
    }

    @Transactional
    public OrdenCompra recibir(Long id) {
        sesion.exigirAdmin();
        OrdenCompra o = repository.buscarConDetalles(id).orElseThrow();
        if (o.getEstado() != EstadoOrden.PENDIENTE) throw new NegocioException("Solo se reciben órdenes pendientes.");
        for (DetalleOrden d : o.getDetalles()) {
            Producto p = d.getProducto();
            p.setStockActual(p.getStockActual() + d.getCantidad());
            productoRepository.save(p);
            movimientoRepository.save(MovimientoStock.builder()
                    .fecha(LocalDateTime.now()).producto(p).tipo(TipoMovimiento.COMPRA)
                    .cantidad(d.getCantidad()).saldo(p.getStockActual()).referencia(o.getNumero()).build());
        }
        o.setEstado(EstadoOrden.RECIBIDA);
        o.setFechaRecepcion(LocalDate.now());
        return repository.save(o);
    }

    @Transactional
    public OrdenCompra anular(Long id) {
        sesion.exigirAdmin();
        OrdenCompra o = repository.findById(id).orElseThrow();
        if (o.getEstado() != EstadoOrden.PENDIENTE) throw new NegocioException("Solo se anulan órdenes pendientes.");
        o.setEstado(EstadoOrden.ANULADA);
        return repository.save(o);
    }

    public long pendientes() {
        return repository.countByEstado(EstadoOrden.PENDIENTE);
    }
}
