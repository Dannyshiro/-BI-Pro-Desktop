package com.dy.bipro.repository;

import com.dy.bipro.model.DYProveedor;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYProveedorRepository extends JpaRepository<DYProveedor, Long> {
    List<DYProveedor> findAllByOrderByNombreAsc();
}
