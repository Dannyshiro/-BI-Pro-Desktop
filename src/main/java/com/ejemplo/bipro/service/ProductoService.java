package com.ejemplo.bipro.service;

import com.ejemplo.bipro.exception.NegocioException;
import com.ejemplo.bipro.model.*;
import com.ejemplo.bipro.repository.DetalleOrdenRepository;
import com.ejemplo.bipro.repository.DetalleVentaRepository;
import com.ejemplo.bipro.repository.MovimientoStockRepository;
import com.ejemplo.bipro.repository.ProductoRepository;
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
public class ProductoService {
    private final ProductoRepository repository;
    private final MovimientoStockRepository movimientoRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final DetalleOrdenRepository detalleOrdenRepository;
    private final Sesion sesion;

    public List<Producto> listarTodos() {
        return repository.findAllByOrderByNombreAsc();
    }

    public List<Producto> buscar(String texto, Categoria categoria, EstadoStock estado) {
        String t = texto == null ? "" : texto.trim().toLowerCase();
        return repository.findAllByOrderByNombreAsc().stream()
                .filter(p -> t.isEmpty() || p.getNombre().toLowerCase().contains(t) || p.getSku().toLowerCase().contains(t))
                .filter(p -> categoria == null || p.getCategoria().getId().equals(categoria.getId()))
                .filter(p -> estado == null || p.getEstado() == estado)
                .toList();
    }

    @Transactional
    public Producto guardar(@Valid Producto producto) {
        sesion.exigirAdmin();
        if (producto.getStockMinimo() > producto.getStockMaximo())
            throw new NegocioException("El stock mínimo no puede ser mayor que el stock máximo.");
        if (producto.getPrecioVenta().compareTo(producto.getPrecioCompra()) <= 0)
            throw new NegocioException("El precio de venta debe ser mayor que el precio de compra.");
        producto.setSku(producto.getSku().trim().toUpperCase());
        repository.findBySkuIgnoreCase(producto.getSku()).ifPresent(existente -> {
            if (producto.getId() == null || !existente.getId().equals(producto.getId()))
                throw new NegocioException("Ya existe un producto con SKU " + producto.getSku() + ".");
        });

        int stockAnterior = producto.getId() == null ? 0
                : repository.findById(producto.getId()).map(Producto::getStockActual).orElse(0);
        Producto guardado = repository.save(producto);

        int diferencia = guardado.getStockActual() - stockAnterior;
        if (diferencia != 0) {
            registrarMovimiento(guardado, TipoMovimiento.AJUSTE, diferencia,
                    stockAnterior == 0 && producto.getId() == null ? "Stock inicial" : "Edición de producto");
        }
        log.info("Producto guardado: id={} sku={}", guardado.getId(), guardado.getSku());
        return guardado;
    }

    @Transactional
    public void eliminar(Producto producto) {
        sesion.exigirAdmin();
        if (detalleVentaRepository.existsByProducto(producto))
            throw new NegocioException("No se puede eliminar \"" + producto.getNombre()
                    + "\": tiene ventas registradas. Conserva el producto para el historial.");
        if (detalleOrdenRepository.existsByProducto(producto))
            throw new NegocioException("No se puede eliminar \"" + producto.getNombre()
                    + "\": figura en órdenes de compra.");
        movimientoRepository.deleteByProducto(producto);
        repository.delete(producto);
    }

    @Transactional
    public Producto ajustar(Long productoId, int cantidad, String motivo) {
        sesion.exigirAdmin();
        if (cantidad == 0) throw new NegocioException("La cantidad del ajuste debe ser distinta de 0.");
        Producto p = repository.findById(productoId).orElseThrow();
        if (p.getStockActual() + cantidad < 0)
            throw new NegocioException("El stock resultante no puede ser negativo (stock actual: " + p.getStockActual() + ").");
        p.setStockActual(p.getStockActual() + cantidad);
        repository.save(p);
        registrarMovimiento(p, TipoMovimiento.AJUSTE, cantidad, motivo);
        return p;
    }

    public List<MovimientoStock> movimientos(Producto producto) {
        return movimientoRepository.findTop50ByProductoOrderByFechaDescIdDesc(producto);
    }

    private void registrarMovimiento(Producto p, TipoMovimiento tipo, int cantidad, String referencia) {
        movimientoRepository.save(MovimientoStock.builder()
                .fecha(LocalDateTime.now()).producto(p).tipo(tipo)
                .cantidad(cantidad).saldo(p.getStockActual()).referencia(referencia).build());
    }
}
