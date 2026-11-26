package com.dy.bipro.service;

import com.dy.bipro.model.DYEmpresa;
import com.dy.bipro.repository.DYEmpresaRepository;
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
public class DYEmpresaService {
    private final DYEmpresaRepository repository;
    private final DYSesion sesion;

    public DYEmpresa obtener() {
        return repository.findFirstByOrderByIdAsc().orElseGet(() -> DYEmpresa.builder()
                .razonSocial("Mi Empresa SAC").ruc("20000000000").igv(new BigDecimal("18"))
                .serieBoleta("B001").build());
    }

    @Transactional
    public DYEmpresa guardar(@Valid DYEmpresa empresa) {
        sesion.exigirAdmin();
        empresa.setSerieBoleta(empresa.getSerieBoleta().toUpperCase());
        return repository.save(empresa);
    }
}
