package com.dy.bipro.dto;

import com.dy.bipro.model.DYProducto;

import java.math.BigDecimal;

public record DYLineaVenta(DYProducto producto, int cantidad) {
    public BigDecimal importe() {
        return producto.getPrecioVenta().multiply(BigDecimal.valueOf(cantidad));
    }
}
