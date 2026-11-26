package com.dy.bipro.repository;

import com.dy.bipro.model.DYCategoria;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYCategoriaRepository extends JpaRepository<DYCategoria, Long> {
    Optional<DYCategoria> findByNombreIgnoreCase(String nombre);

    List<DYCategoria> findAllByOrderByNombreAsc();
}
