package com.dy.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DYEstadoCaja {
    ABIERTA("Abierta"),
    CERRADA("Cerrada");

    private final String etiqueta;

    @Override
    public String toString() {
        return etiqueta;
    }
}
