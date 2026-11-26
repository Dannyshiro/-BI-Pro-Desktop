package com.ejemplo.bipro.service;

import com.ejemplo.bipro.model.Proveedor;
import com.ejemplo.bipro.repository.ProveedorRepository;
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
public class ProveedorService {
    private final ProveedorRepository repository;
    private final Sesion sesion;

    public List<Proveedor> listar() {
        return repository.findAllByOrderByNombreAsc();
    }

    @Transactional
    public Proveedor guardar(@Valid Proveedor proveedor) {
        sesion.exigirAdmin();
        return repository.save(proveedor);
    }
}
