package com.ejemplo.bipro.dto;

import com.ejemplo.bipro.model.MetodoPago;
import com.ejemplo.bipro.model.TurnoCaja;

import java.math.BigDecimal;
import java.util.Map;

public record ResumenCaja(TurnoCaja turno,
                          BigDecimal totalVentas,
                          long numeroVentas,
                          Map<MetodoPago, BigDecimal> montos,
                          Map<MetodoPago, Long> conteo) {
    public BigDecimal efectivo() {
        return montos.getOrDefault(MetodoPago.EFECTIVO, BigDecimal.ZERO);
    }

    public BigDecimal efectivoEsperado() {
        return turno.getMontoInicial().add(efectivo());
    }
}
