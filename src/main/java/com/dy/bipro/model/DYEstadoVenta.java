package com.dy.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DYEstadoVenta {
    EMITIDA("Emitida"),
    ANULADA("Anulada");

    private final String etiqueta;

    @Override
    public String toString() {
        return etiqueta;
    }
}
