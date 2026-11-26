package com.dy.bipro.model;

import com.dy.bipro.dto.DYIntervalo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public enum DYRangoFechas {
    HOY("Hoy"),
    ULTIMOS_7_DIAS("Últimos 7 días"),
    ULTIMOS_30_DIAS("Últimos 30 días"),
    ESTE_MES("Este mes"),
    ESTE_TRIMESTRE("Este trimestre"),
    ESTE_ANIO("Este año");

    private final String etiqueta;

    public DYIntervalo actual(LocalDate hoy) {
        return switch (this) {
            case HOY -> new DYIntervalo(hoy, hoy);
            case ULTIMOS_7_DIAS -> new DYIntervalo(hoy.minusDays(6), hoy);
            case ULTIMOS_30_DIAS -> new DYIntervalo(hoy.minusDays(29), hoy);
            case ESTE_MES -> new DYIntervalo(hoy.withDayOfMonth(1), hoy);
            case ESTE_TRIMESTRE -> new DYIntervalo(
                    hoy.withMonth(((hoy.getMonthValue() - 1) / 3) * 3 + 1).withDayOfMonth(1), hoy);
            case ESTE_ANIO -> new DYIntervalo(hoy.withDayOfYear(1), hoy);
        };
    }

    public DYIntervalo anterior(LocalDate hoy) {
        DYIntervalo a = actual(hoy);
        return switch (this) {
            case HOY -> new DYIntervalo(hoy.minusWeeks(1), hoy.minusWeeks(1));
            case ESTE_MES -> new DYIntervalo(a.desde().minusMonths(1), a.hasta().minusMonths(1));
            case ESTE_TRIMESTRE -> new DYIntervalo(a.desde().minusMonths(3), a.hasta().minusMonths(3));
            case ESTE_ANIO -> new DYIntervalo(a.desde().minusYears(1), a.hasta().minusYears(1));
            default -> new DYIntervalo(a.desde().minusDays(a.dias()), a.desde().minusDays(1));
        };
    }

    public String comparacion() {
        return switch (this) {
            case HOY -> "vs. mismo día de la semana pasada";
            case ESTE_MES -> "vs. mismo tramo del mes anterior";
            case ESTE_TRIMESTRE -> "vs. trimestre anterior";
            case ESTE_ANIO -> "vs. año anterior a la fecha";
            default -> "vs. periodo anterior";
        };
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
