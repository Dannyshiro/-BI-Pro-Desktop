package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.Cliente;
import com.ejemplo.bipro.model.EstadoVenta;
import com.ejemplo.bipro.model.TurnoCaja;
import com.ejemplo.bipro.model.Venta;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findTopByOrderByIdDesc();

    List<Venta> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);

    List<Venta> findByClienteOrderByFechaDesc(Cliente cliente);

    boolean existsByClienteAndEstado(Cliente cliente, EstadoVenta estado);

    @Query("SELECT DISTINCT v FROM Venta v LEFT JOIN FETCH v.detalles WHERE v.id = :id")
    Optional<Venta> buscarConDetalles(@Param("id") Long id);

    @Query("""
            SELECT COALESCE(SUM(v.total), 0) FROM Venta v
            WHERE v.estado = :estado AND v.fecha BETWEEN :desde AND :hasta
            """)
    Number totalEntre(@Param("estado") EstadoVenta estado,
                      @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT COUNT(v) FROM Venta v
            WHERE v.estado = :estado AND v.fecha BETWEEN :desde AND :hasta
            """)
    long contarEntre(@Param("estado") EstadoVenta estado,
                     @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT v.fecha, v.total FROM Venta v
            WHERE v.estado = :estado AND v.fecha BETWEEN :desde AND :hasta
            """)
    List<Object[]> fechasYTotales(@Param("estado") EstadoVenta estado,
                                  @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT v.metodoPago, COUNT(v), COALESCE(SUM(v.total), 0) FROM Venta v
            WHERE v.turno = :turno AND v.estado = :estado
            GROUP BY v.metodoPago
            """)
    List<Object[]> resumenPorMetodo(@Param("turno") TurnoCaja turno, @Param("estado") EstadoVenta estado);
}
