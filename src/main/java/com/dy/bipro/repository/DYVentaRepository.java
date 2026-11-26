package com.dy.bipro.repository;

import com.dy.bipro.model.DYCliente;
import com.dy.bipro.model.DYEstadoVenta;
import com.dy.bipro.model.DYTurnoCaja;
import com.dy.bipro.model.DYVenta;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYVentaRepository extends JpaRepository<DYVenta, Long> {
    Optional<DYVenta> findTopByOrderByIdDesc();

    List<DYVenta> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);

    List<DYVenta> findByClienteOrderByFechaDesc(DYCliente cliente);

    boolean existsByClienteAndEstado(DYCliente cliente, DYEstadoVenta estado);

    @Query("SELECT DISTINCT v FROM DYVenta v LEFT JOIN FETCH v.detalles WHERE v.id = :id")
    Optional<DYVenta> buscarConDetalles(@Param("id") Long id);

    @Query("""
            SELECT COALESCE(SUM(v.total), 0) FROM DYVenta v
            WHERE v.estado = :estado AND v.fecha BETWEEN :desde AND :hasta
            """)
    Number totalEntre(@Param("estado") DYEstadoVenta estado,
                      @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT COUNT(v) FROM DYVenta v
            WHERE v.estado = :estado AND v.fecha BETWEEN :desde AND :hasta
            """)
    long contarEntre(@Param("estado") DYEstadoVenta estado,
                     @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT v.fecha, v.total FROM DYVenta v
            WHERE v.estado = :estado AND v.fecha BETWEEN :desde AND :hasta
            """)
    List<Object[]> fechasYTotales(@Param("estado") DYEstadoVenta estado,
                                  @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT v.metodoPago, COUNT(v), COALESCE(SUM(v.total), 0) FROM DYVenta v
            WHERE v.turno = :turno AND v.estado = :estado
            GROUP BY v.metodoPago
            """)
    List<Object[]> resumenPorMetodo(@Param("turno") DYTurnoCaja turno, @Param("estado") DYEstadoVenta estado);
}
