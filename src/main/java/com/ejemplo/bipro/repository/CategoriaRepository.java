package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.Categoria;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    List<Categoria> findAllByOrderByNombreAsc();
}
