package com.dy.bipro.repository;

import com.dy.bipro.model.DYDetalleVenta;
import com.dy.bipro.model.DYEstadoVenta;
import com.dy.bipro.model.DYProducto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYDetalleVentaRepository extends JpaRepository<DYDetalleVenta, Long> {
    boolean existsByProducto(DYProducto producto);

    @Query("""
            SELECT COALESCE(SUM(d.cantidad * d.costoUnitario), 0) FROM DYDetalleVenta d
            WHERE d.venta.estado = :estado AND d.venta.fecha BETWEEN :desde AND :hasta
            """)
    Number costoEntre(@Param("estado") DYEstadoVenta estado,
                      @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT d.producto.categoria.nombre, SUM(d.cantidad * d.precioUnitario) FROM DYDetalleVenta d
            WHERE d.venta.estado = :estado AND d.venta.fecha BETWEEN :desde AND :hasta
            GROUP BY d.producto.categoria.nombre
            """)
    List<Object[]> ventasPorCategoria(@Param("estado") DYEstadoVenta estado,
                                      @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
