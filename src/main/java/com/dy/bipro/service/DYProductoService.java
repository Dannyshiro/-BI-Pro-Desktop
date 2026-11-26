package com.dy.bipro.service;

import com.dy.bipro.exception.DYNegocioException;
import com.dy.bipro.model.*;
import com.dy.bipro.repository.DYDetalleOrdenRepository;
import com.dy.bipro.repository.DYDetalleVentaRepository;
import com.dy.bipro.repository.DYMovimientoStockRepository;
import com.dy.bipro.repository.DYProductoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DYProductoService {
    private final DYProductoRepository repository;
    private final DYMovimientoStockRepository movimientoRepository;
    private final DYDetalleVentaRepository detalleVentaRepository;
    private final DYDetalleOrdenRepository detalleOrdenRepository;
    private final DYSesion sesion;

    public List<DYProducto> listarTodos() {
        return repository.findAllByOrderByNombreAsc();
    }

    public List<DYProducto> buscar(String texto, DYCategoria categoria, DYEstadoStock estado) {
        String t = texto == null ? "" : texto.trim().toLowerCase();
        return repository.findAllByOrderByNombreAsc().stream()
                .filter(p -> t.isEmpty() || p.getNombre().toLowerCase().contains(t) || p.getSku().toLowerCase().contains(t))
                .filter(p -> categoria == null || p.getCategoria().getId().equals(categoria.getId()))
                .filter(p -> estado == null || p.getEstado() == estado)
                .toList();
    }

    @Transactional
    public DYProducto guardar(@Valid DYProducto producto) {
        sesion.exigirAdmin();
        if (producto.getStockMinimo() > producto.getStockMaximo())
            throw new DYNegocioException("El stock mínimo no puede ser mayor que el stock máximo.");
        if (producto.getPrecioVenta().compareTo(producto.getPrecioCompra()) <= 0)
            throw new DYNegocioException("El precio de venta debe ser mayor que el precio de compra.");
        producto.setSku(producto.getSku().trim().toUpperCase());
        repository.findBySkuIgnoreCase(producto.getSku()).ifPresent(existente -> {
            if (producto.getId() == null || !existente.getId().equals(producto.getId()))
                throw new DYNegocioException("Ya existe un producto con SKU " + producto.getSku() + ".");
        });

        int stockAnterior = producto.getId() == null ? 0
                : repository.findById(producto.getId()).map(DYProducto::getStockActual).orElse(0);
        DYProducto guardado = repository.save(producto);

        int diferencia = guardado.getStockActual() - stockAnterior;
        if (diferencia != 0) {
            registrarMovimiento(guardado, DYTipoMovimiento.AJUSTE, diferencia,
                    stockAnterior == 0 && producto.getId() == null ? "Stock inicial" : "Edición de producto");
        }
        log.info("Producto guardado: id={} sku={}", guardado.getId(), guardado.getSku());
        return guardado;
    }

    @Transactional
    public void eliminar(DYProducto producto) {
        sesion.exigirAdmin();
        if (detalleVentaRepository.existsByProducto(producto))
            throw new DYNegocioException("No se puede eliminar \"" + producto.getNombre()
                    + "\": tiene ventas registradas. Conserva el producto para el historial.");
        if (detalleOrdenRepository.existsByProducto(producto))
            throw new DYNegocioException("No se puede eliminar \"" + producto.getNombre()
                    + "\": figura en órdenes de compra.");
        movimientoRepository.deleteByProducto(producto);
        repository.delete(producto);
    }

    @Transactional
    public DYProducto ajustar(Long productoId, int cantidad, String motivo) {
        sesion.exigirAdmin();
        if (cantidad == 0) throw new DYNegocioException("La cantidad del ajuste debe ser distinta de 0.");
        DYProducto p = repository.findById(productoId).orElseThrow();
        if (p.getStockActual() + cantidad < 0)
            throw new DYNegocioException("El stock resultante no puede ser negativo (stock actual: " + p.getStockActual() + ").");
        p.setStockActual(p.getStockActual() + cantidad);
        repository.save(p);
        registrarMovimiento(p, DYTipoMovimiento.AJUSTE, cantidad, motivo);
        return p;
    }

    public List<DYMovimientoStock> movimientos(DYProducto producto) {
        return movimientoRepository.findTop50ByProductoOrderByFechaDescIdDesc(producto);
    }

    private void registrarMovimiento(DYProducto p, DYTipoMovimiento tipo, int cantidad, String referencia) {
        movimientoRepository.save(DYMovimientoStock.builder()
                .fecha(LocalDateTime.now()).producto(p).tipo(tipo)
                .cantidad(cantidad).saldo(p.getStockActual()).referencia(referencia).build());
    }
}
