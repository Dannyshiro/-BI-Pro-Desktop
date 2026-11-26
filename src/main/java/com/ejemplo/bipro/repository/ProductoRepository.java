package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.Categoria;
import com.ejemplo.bipro.model.Producto;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findBySkuIgnoreCase(String sku);

    long countByCategoria(Categoria categoria);

    List<Producto> findAllByOrderByNombreAsc();
}
