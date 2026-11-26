package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.DetalleOrden;
import com.ejemplo.bipro.model.EstadoOrden;
import com.ejemplo.bipro.model.Producto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleOrdenRepository extends JpaRepository<DetalleOrden, Long> {
    boolean existsByProducto(Producto producto);

    @Query("SELECT DISTINCT d.producto.id FROM DetalleOrden d WHERE d.orden.estado = :estado")
    List<Long> productosConOrden(@Param("estado") EstadoOrden estado);
}
