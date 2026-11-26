package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.DetalleVenta;
import com.ejemplo.bipro.model.EstadoVenta;
import com.ejemplo.bipro.model.Producto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    boolean existsByProducto(Producto producto);

    @Query("""
            SELECT COALESCE(SUM(d.cantidad * d.costoUnitario), 0) FROM DetalleVenta d
            WHERE d.venta.estado = :estado AND d.venta.fecha BETWEEN :desde AND :hasta
            """)
    Number costoEntre(@Param("estado") EstadoVenta estado,
                      @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT d.producto.categoria.nombre, SUM(d.cantidad * d.precioUnitario) FROM DetalleVenta d
            WHERE d.venta.estado = :estado AND d.venta.fecha BETWEEN :desde AND :hasta
            GROUP BY d.producto.categoria.nombre
            """)
    List<Object[]> ventasPorCategoria(@Param("estado") EstadoVenta estado,
                                      @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
