package com.dy.bipro.dto;

import com.dy.bipro.model.DYProducto;
import com.dy.bipro.model.DYProveedor;

import java.math.BigDecimal;

public record DYSugerencia(DYProducto producto, DYProveedor proveedor, int cantidad) {
    public BigDecimal costo() {
        return producto.getPrecioCompra().multiply(BigDecimal.valueOf(cantidad));
    }
}
