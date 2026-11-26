package com.dy.bipro.repository;

import com.dy.bipro.model.DYEmpresa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYEmpresaRepository extends JpaRepository<DYEmpresa, Long> {
    Optional<DYEmpresa> findFirstByOrderByIdAsc();
}
