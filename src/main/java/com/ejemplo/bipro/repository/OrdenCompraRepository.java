package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.EstadoOrden;
import com.ejemplo.bipro.model.OrdenCompra;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
    Optional<OrdenCompra> findTopByOrderByIdDesc();

    long countByEstado(EstadoOrden estado);

    @Query("SELECT DISTINCT o FROM OrdenCompra o LEFT JOIN FETCH o.detalles ORDER BY o.fecha DESC, o.id DESC")
    List<OrdenCompra> listarConDetalles();

    @Query("SELECT DISTINCT o FROM OrdenCompra o LEFT JOIN FETCH o.detalles WHERE o.id = :id")
    Optional<OrdenCompra> buscarConDetalles(@Param("id") Long id);
}
