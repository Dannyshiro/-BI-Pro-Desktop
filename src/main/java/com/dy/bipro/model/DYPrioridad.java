package com.dy.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DYPrioridad {
    ALTA("Alta", "peligro"),
    MEDIA("Media", "advertencia"),
    BAJA("Baja", "info");

    private final String etiqueta;
    private final String estilo;

    @Override
    public String toString() {
        return etiqueta;
    }
}
