package com.ejemplo.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoVenta {
    EMITIDA("Emitida"),
    ANULADA("Anulada");

    private final String etiqueta;

    @Override
    public String toString() {
        return etiqueta;
    }
}
