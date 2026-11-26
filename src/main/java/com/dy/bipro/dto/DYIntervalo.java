package com.dy.bipro.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public record DYIntervalo(LocalDate desde, LocalDate hasta) {
    public LocalDateTime inicio() {
        return desde.atStartOfDay();
    }

    public LocalDateTime fin() {
        return hasta.atTime(LocalTime.MAX);
    }

    public long dias() {
        return ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    public static DYIntervalo dia(LocalDate d) {
        return new DYIntervalo(d, d);
    }
}
