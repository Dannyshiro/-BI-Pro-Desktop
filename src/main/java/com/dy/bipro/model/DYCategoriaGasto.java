package com.dy.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DYCategoriaGasto {
    PLANILLA("Planilla"),
    ALQUILER("Alquiler"),
    SERVICIOS("Servicios"),
    TRANSPORTE("Transporte"),
    MARKETING("Marketing"),
    OTROS("Otros");

    private final String etiqueta;

    @Override
    public String toString() {
        return etiqueta;
    }
}
