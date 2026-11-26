package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.MovimientoStock;
import com.ejemplo.bipro.model.Producto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {
    List<MovimientoStock> findTop50ByProductoOrderByFechaDescIdDesc(Producto producto);

    void deleteByProducto(Producto producto);
}
