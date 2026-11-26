package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.Rol;
import com.ejemplo.bipro.model.Usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuarioIgnoreCase(String usuario);

    boolean existsByUsuarioIgnoreCase(String usuario);

    long countByRolAndActivoTrue(Rol rol);

    List<Usuario> findAllByOrderByNombreAsc();
}
