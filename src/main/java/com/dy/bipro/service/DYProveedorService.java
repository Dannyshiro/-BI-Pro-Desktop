package com.dy.bipro.service;

import com.dy.bipro.model.DYProveedor;
import com.dy.bipro.repository.DYProveedorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DYProveedorService {
    private final DYProveedorRepository repository;
    private final DYSesion sesion;

    public List<DYProveedor> listar() {
        return repository.findAllByOrderByNombreAsc();
    }

    @Transactional
    public DYProveedor guardar(@Valid DYProveedor proveedor) {
        sesion.exigirAdmin();
        return repository.save(proveedor);
    }
}
