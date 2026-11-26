package com.dy.bipro.repository;

import com.dy.bipro.model.DYMovimientoStock;
import com.dy.bipro.model.DYProducto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYMovimientoStockRepository extends JpaRepository<DYMovimientoStock, Long> {
    List<DYMovimientoStock> findTop50ByProductoOrderByFechaDescIdDesc(DYProducto producto);

    void deleteByProducto(DYProducto producto);
}
