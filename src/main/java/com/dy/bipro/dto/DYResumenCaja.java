package com.dy.bipro.dto;

import com.dy.bipro.model.DYMetodoPago;
import com.dy.bipro.model.DYTurnoCaja;

import java.math.BigDecimal;
import java.util.Map;

public record DYResumenCaja(DYTurnoCaja turno,
                          BigDecimal totalVentas,
                          long numeroVentas,
                          Map<DYMetodoPago, BigDecimal> montos,
                          Map<DYMetodoPago, Long> conteo) {
    public BigDecimal efectivo() {
        return montos.getOrDefault(DYMetodoPago.EFECTIVO, BigDecimal.ZERO);
    }

    public BigDecimal efectivoEsperado() {
        return turno.getMontoInicial().add(efectivo());
    }
}
