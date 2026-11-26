package com.ejemplo.bipro.dto;

import com.ejemplo.bipro.model.Producto;

import java.math.BigDecimal;

public record LineaVenta(Producto producto, int cantidad) {
    public BigDecimal importe() {
        return producto.getPrecioVenta().multiply(BigDecimal.valueOf(cantidad));
    }
}
