package com.ejemplo.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoTarea {
    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    HECHO("Hecho");

    private final String etiqueta;

    @Override
    public String toString() {
        return etiqueta;
    }
}
