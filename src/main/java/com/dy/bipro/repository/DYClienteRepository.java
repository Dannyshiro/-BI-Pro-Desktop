package com.dy.bipro.repository;

import com.dy.bipro.model.DYCliente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYClienteRepository extends JpaRepository<DYCliente, Long> {
    Optional<DYCliente> findByDocumento(String documento);

    List<DYCliente> findAllByOrderByNombreAsc();
}
