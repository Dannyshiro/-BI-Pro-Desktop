package com.dy.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DYEstadoStock {
    AGOTADO("Agotado", "peligro"),
    CRITICO("Crítico", "advertencia"),
    NORMAL("Normal", "exito"),
    EXCESO("Exceso", "info");

    private final String etiqueta;

    private final String estilo;

    @Override
    public String toString() {
        return etiqueta;
    }
}
