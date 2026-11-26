package com.dy.bipro.repository;

import com.dy.bipro.model.DYRol;
import com.dy.bipro.model.DYUsuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYUsuarioRepository extends JpaRepository<DYUsuario, Long> {
    Optional<DYUsuario> findByUsuarioIgnoreCase(String usuario);

    boolean existsByUsuarioIgnoreCase(String usuario);

    long countByRolAndActivoTrue(DYRol rol);

    List<DYUsuario> findAllByOrderByNombreAsc();
}
