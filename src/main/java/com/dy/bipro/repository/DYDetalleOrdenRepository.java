package com.dy.bipro.repository;

import com.dy.bipro.model.DYDetalleOrden;
import com.dy.bipro.model.DYEstadoOrden;
import com.dy.bipro.model.DYProducto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYDetalleOrdenRepository extends JpaRepository<DYDetalleOrden, Long> {
    boolean existsByProducto(DYProducto producto);

    @Query("SELECT DISTINCT d.producto.id FROM DYDetalleOrden d WHERE d.orden.estado = :estado")
    List<Long> productosConOrden(@Param("estado") DYEstadoOrden estado);
}
