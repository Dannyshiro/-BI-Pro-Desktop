package com.dy.bipro.repository;

import com.dy.bipro.model.DYCategoria;
import com.dy.bipro.model.DYProducto;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYProductoRepository extends JpaRepository<DYProducto, Long> {
    Optional<DYProducto> findBySkuIgnoreCase(String sku);

    long countByCategoria(DYCategoria categoria);

    List<DYProducto> findAllByOrderByNombreAsc();
}
