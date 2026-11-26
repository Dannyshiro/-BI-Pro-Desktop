package com.ejemplo.bipro.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MesResumen(YearMonth mes, BigDecimal ingresos, BigDecimal egresos) {
}
