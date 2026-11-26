package com.ejemplo.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoMovimiento {
    VENTA("Venta"),
    COMPRA("Compra recibida"),
    AJUSTE("Ajuste"),
    ANULACION("Anulación de venta");

    private final String etiqueta;

    @Override
    public String toString() {
        return etiqueta;
    }
}
