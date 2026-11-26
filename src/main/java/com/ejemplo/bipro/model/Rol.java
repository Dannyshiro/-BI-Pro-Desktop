package com.ejemplo.bipro.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Rol {
    ADMIN("Administrador"),
    VENDEDOR("Vendedor");

    private final String etiqueta;

    @Override
    public String toString() {
        return etiqueta;
    }
}
