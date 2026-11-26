package com.ejemplo.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MetodoPago {
    EFECTIVO("Efectivo", "fth-dollar-sign"),
    YAPE("Yape", "fth-smartphone"),
    TARJETA("Tarjeta", "fth-credit-card"),
    TRANSFERENCIA("Transferencia", "fth-repeat");

    private final String etiqueta;
    private final String icono;

    @Override
    public String toString() {
        return etiqueta;
    }
}
