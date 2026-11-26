package com.ejemplo.bipro.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Kpis(BigDecimal ventas, BigDecimal costo, long transacciones, BigDecimal gastos) {
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public BigDecimal margenBruto() {
        if (ventas.signum() == 0) return BigDecimal.ZERO;
        return ventas.subtract(costo).multiply(CIEN).divide(ventas, 1, RoundingMode.HALF_UP);
    }

    public BigDecimal ticketPromedio() {
        if (transacciones == 0) return BigDecimal.ZERO;
        return ventas.divide(BigDecimal.valueOf(transacciones), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal utilidad() {
        return ventas.subtract(costo).subtract(gastos);
    }

    public BigDecimal margenNeto() {
        if (ventas.signum() == 0) return BigDecimal.ZERO;
        return utilidad().multiply(CIEN).divide(ventas, 1, RoundingMode.HALF_UP);
    }
}
