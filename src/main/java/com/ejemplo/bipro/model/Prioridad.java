package com.ejemplo.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Prioridad {
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
