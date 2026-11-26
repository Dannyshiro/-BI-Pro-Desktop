package com.dy.bipro.ui;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DYFormato {
    private static final DecimalFormatSymbols US = DecimalFormatSymbols.getInstance(Locale.US);
    private static final DecimalFormat SOLES = new DecimalFormat("#,##0.00", US);
    private static final DecimalFormat ENTERO = new DecimalFormat("#,##0", US);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    public static final Locale PERU = Locale.forLanguageTag("es-PE");

    private DYFormato() {
    }

    public static String soles(BigDecimal v) {
        return "S/ " + SOLES.format(v == null ? BigDecimal.ZERO : v);
    }

    public static String solesEntero(BigDecimal v) {
        return "S/ " + ENTERO.format(v == null ? BigDecimal.ZERO : v);
    }

    public static String numero(BigDecimal v) {
        return SOLES.format(v == null ? BigDecimal.ZERO : v);
    }

    public static String fecha(LocalDate d) {
        return d == null ? "—" : d.format(FECHA);
    }

    public static String fechaHora(LocalDateTime d) {
        return d == null ? "—" : d.format(FECHA_HORA);
    }

    public static String hora(LocalDateTime d) {
        return d == null ? "—" : d.format(HORA);
    }

    public static String variacion(double pct) {
        return (pct >= 0 ? "+" : "−") + String.format(Locale.US, "%.1f%%", Math.abs(pct));
    }

    public static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "?";
        String[] p = nombre.trim().split("\\s+");
        return (p[0].substring(0, 1) + (p.length > 1 ? p[1].substring(0, 1) : "")).toUpperCase();
    }

    public static String capitalizar(String s) {
        return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static BigDecimal leerMonto(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : new BigDecimal(texto.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer leerEntero(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String vacio(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
