package com.ejemplo.bipro.dto;

import com.ejemplo.bipro.model.Producto;
import com.ejemplo.bipro.model.Proveedor;

import java.math.BigDecimal;

public record Sugerencia(Producto producto, Proveedor proveedor, int cantidad) {
    public BigDecimal costo() {
        return producto.getPrecioCompra().multiply(BigDecimal.valueOf(cantidad));
    }
}
