package com.dy.bipro.repository;

import com.dy.bipro.model.DYEstadoOrden;
import com.dy.bipro.model.DYOrdenCompra;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYOrdenCompraRepository extends JpaRepository<DYOrdenCompra, Long> {
    Optional<DYOrdenCompra> findTopByOrderByIdDesc();

    long countByEstado(DYEstadoOrden estado);

    @Query("SELECT DISTINCT o FROM DYOrdenCompra o LEFT JOIN FETCH o.detalles ORDER BY o.fecha DESC, o.id DESC")
    List<DYOrdenCompra> listarConDetalles();

    @Query("SELECT DISTINCT o FROM DYOrdenCompra o LEFT JOIN FETCH o.detalles WHERE o.id = :id")
    Optional<DYOrdenCompra> buscarConDetalles(@Param("id") Long id);
}
