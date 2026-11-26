package com.ejemplo.bipro.service;

import com.ejemplo.bipro.model.Gasto;
import com.ejemplo.bipro.repository.GastoRepository;
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
public class GastoService {
    private final GastoRepository repository;
    private final Sesion sesion;

    public List<Gasto> listar() {
        return repository.findAllByOrderByFechaDescIdDesc();
    }

    @Transactional
    public Gasto guardar(@Valid Gasto gasto) {
        sesion.exigirAdmin();
        return repository.save(gasto);
    }

    @Transactional
    public void eliminar(Gasto gasto) {
        sesion.exigirAdmin();
        repository.delete(gasto);
    }
}
