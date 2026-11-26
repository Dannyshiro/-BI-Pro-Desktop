package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.Cliente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByDocumento(String documento);

    List<Cliente> findAllByOrderByNombreAsc();
}
