package com.dy.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DYSeveridad {
    CRITICO("Crítico", "peligro", "fth-alert-octagon"),
    ADVERTENCIA("Advertencia", "advertencia", "fth-alert-triangle"),
    INFO("Info", "info", "fth-info");

    private final String etiqueta;
    private final String estilo;
    private final String icono;

    @Override
    public String toString() {
        return etiqueta;
    }
}
