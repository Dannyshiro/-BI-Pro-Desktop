package com.dy.bipro.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record DYMesResumen(YearMonth mes, BigDecimal ingresos, BigDecimal egresos) {
}
