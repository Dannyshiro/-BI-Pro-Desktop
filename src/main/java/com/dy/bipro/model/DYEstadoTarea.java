package com.dy.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DYEstadoTarea {
    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    HECHO("Hecho");

    private final String etiqueta;

    @Override
    public String toString() {
        return etiqueta;
    }
}
