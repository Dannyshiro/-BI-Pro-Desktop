package com.dy.bipro.service;

import com.dy.bipro.model.DYGasto;
import com.dy.bipro.repository.DYGastoRepository;
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
public class DYGastoService {
    private final DYGastoRepository repository;
    private final DYSesion sesion;

    public List<DYGasto> listar() {
        return repository.findAllByOrderByFechaDescIdDesc();
    }

    @Transactional
    public DYGasto guardar(@Valid DYGasto gasto) {
        sesion.exigirAdmin();
        return repository.save(gasto);
    }

    @Transactional
    public void eliminar(DYGasto gasto) {
        sesion.exigirAdmin();
        repository.delete(gasto);
    }
}
