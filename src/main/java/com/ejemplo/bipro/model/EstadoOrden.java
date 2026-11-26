package com.ejemplo.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoOrden {
    PENDIENTE("Pendiente", "advertencia"),
    RECIBIDA("Recibida", "exito"),
    ANULADA("Anulada", "neutro");

    private final String etiqueta;
    private final String estilo;

    @Override
    public String toString() {
        return etiqueta;
    }
}
