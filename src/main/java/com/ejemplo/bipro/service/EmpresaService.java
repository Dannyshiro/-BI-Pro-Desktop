package com.ejemplo.bipro.service;

import com.ejemplo.bipro.model.Empresa;
import com.ejemplo.bipro.repository.EmpresaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmpresaService {
    private final EmpresaRepository repository;
    private final Sesion sesion;

    public Empresa obtener() {
        return repository.findFirstByOrderByIdAsc().orElseGet(() -> Empresa.builder()
                .razonSocial("Mi Empresa SAC").ruc("20000000000").igv(new BigDecimal("18"))
                .serieBoleta("B001").build());
    }

    @Transactional
    public Empresa guardar(@Valid Empresa empresa) {
        sesion.exigirAdmin();
        empresa.setSerieBoleta(empresa.getSerieBoleta().toUpperCase());
        return repository.save(empresa);
    }
}
