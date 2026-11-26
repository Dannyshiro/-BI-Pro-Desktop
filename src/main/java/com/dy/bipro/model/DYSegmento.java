package com.dy.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DYSegmento {
    FRECUENTE("Frecuente", "exito"),
    REGULAR("Regular", "neutro"),
    NUEVO("Nuevo", "info"),
    INACTIVO("Inactivo", "advertencia");

    private final String etiqueta;
    private final String estilo;

    @Override
    public String toString() {
        return etiqueta;
    }
}
