package com.dy.bipro.service;

import com.dy.bipro.dto.DYSugerencia;
import com.dy.bipro.exception.DYNegocioException;
import com.dy.bipro.model.*;
import com.dy.bipro.repository.DYDetalleOrdenRepository;
import com.dy.bipro.repository.DYMovimientoStockRepository;
import com.dy.bipro.repository.DYOrdenCompraRepository;
import com.dy.bipro.repository.DYProductoRepository;
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
public class DYCompraService {
    private final DYOrdenCompraRepository repository;
    private final DYDetalleOrdenRepository detalleRepository;
    private final DYProductoRepository productoRepository;
    private final DYMovimientoStockRepository movimientoRepository;
    private final DYSesion sesion;

    public List<DYOrdenCompra> listar() {
        return repository.listarConDetalles();
    }

    public Set<Long> productosConOrdenPendiente() {
        return new HashSet<>(detalleRepository.productosConOrden(DYEstadoOrden.PENDIENTE));
    }

    public List<DYSugerencia> sugerencia() {
        Set<Long> enOrden = productosConOrdenPendiente();
        return productoRepository.findAllByOrderByNombreAsc().stream()
                .filter(p -> p.getStockActual() < p.getStockMinimo() && !enOrden.contains(p.getId()))
                .map(p -> new DYSugerencia(p, p.getCategoria().getProveedor(), p.getStockMaximo() - p.getStockActual()))
                .toList();
    }

    @Transactional
    public List<DYOrdenCompra> generarOrdenes() {
        sesion.exigirAdmin();
        List<DYSugerencia> sugerencias = sugerencia();
        if (sugerencias.isEmpty()) throw new DYNegocioException("No hay productos pendientes de reposición.");
        List<String> sinProveedor = sugerencias.stream().filter(s -> s.proveedor() == null)
                .map(s -> s.producto().getCategoria().getNombre()).distinct().toList();
        if (!sinProveedor.isEmpty())
            throw new DYNegocioException("Asigna un proveedor a estas categorías: " + String.join(", ", sinProveedor));

        Map<DYProveedor, List<DYSugerencia>> porProveedor = sugerencias.stream()
                .collect(Collectors.groupingBy(DYSugerencia::proveedor, LinkedHashMap::new, Collectors.toList()));

        long siguiente = repository.findTopByOrderByIdDesc().map(DYOrdenCompra::getId).orElse(0L);
        List<DYOrdenCompra> creadas = new ArrayList<>();
        for (Map.Entry<DYProveedor, List<DYSugerencia>> e : porProveedor.entrySet()) {
            siguiente++;
            DYOrdenCompra o = DYOrdenCompra.builder()
                    .numero(String.format("OC-%d-%04d", LocalDate.now().getYear(), siguiente))
                    .fecha(LocalDate.now()).proveedor(e.getKey()).estado(DYEstadoOrden.PENDIENTE).build();
            for (DYSugerencia s : e.getValue()) {
                o.agregarDetalle(DYDetalleOrden.builder().producto(s.producto())
                        .cantidad(s.cantidad()).costoUnitario(s.producto().getPrecioCompra()).build());
            }
            creadas.add(repository.save(o));
        }
        return creadas;
    }

    @Transactional
    public DYOrdenCompra recibir(Long id) {
        sesion.exigirAdmin();
        DYOrdenCompra o = repository.buscarConDetalles(id).orElseThrow();
        if (o.getEstado() != DYEstadoOrden.PENDIENTE) throw new DYNegocioException("Solo se reciben órdenes pendientes.");
        for (DYDetalleOrden d : o.getDetalles()) {
            DYProducto p = d.getProducto();
            p.setStockActual(p.getStockActual() + d.getCantidad());
            productoRepository.save(p);
            movimientoRepository.save(DYMovimientoStock.builder()
                    .fecha(LocalDateTime.now()).producto(p).tipo(DYTipoMovimiento.COMPRA)
                    .cantidad(d.getCantidad()).saldo(p.getStockActual()).referencia(o.getNumero()).build());
        }
        o.setEstado(DYEstadoOrden.RECIBIDA);
        o.setFechaRecepcion(LocalDate.now());
        return repository.save(o);
    }

    @Transactional
    public DYOrdenCompra anular(Long id) {
        sesion.exigirAdmin();
        DYOrdenCompra o = repository.findById(id).orElseThrow();
        if (o.getEstado() != DYEstadoOrden.PENDIENTE) throw new DYNegocioException("Solo se anulan órdenes pendientes.");
        o.setEstado(DYEstadoOrden.ANULADA);
        return repository.save(o);
    }

    public long pendientes() {
        return repository.countByEstado(DYEstadoOrden.PENDIENTE);
    }
}
