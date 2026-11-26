package com.dy.bipro.ui;

import com.dy.bipro.exception.DYNegocioException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.stream.Collectors;

public final class DYAlertas {
    private DYAlertas() {
    }

    public static void info(String encabezado, String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, "Información", encabezado, mensaje);
    }

    public static void error(String encabezado, String mensaje) {
        mostrar(Alert.AlertType.ERROR, "Regla de negocio", encabezado, mensaje);
    }

    public static void advertencia(String encabezado, String mensaje) {
        mostrar(Alert.AlertType.WARNING, "Validación", encabezado, mensaje);
    }

    public static boolean confirmar(String encabezado, String mensaje) {
        Alert a = crear(Alert.AlertType.CONFIRMATION, "Confirmar", encabezado, mensaje);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    public static void manejar(Throwable e) {
        Throwable causa = e;
        while (causa.getCause() != null && !(causa instanceof ConstraintViolationException)
                && !(causa instanceof DYNegocioException)) {
            causa = causa.getCause();
        }
        if (causa instanceof ConstraintViolationException cve) {
            String detalle = cve.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage).distinct()
                    .collect(Collectors.joining("\n• ", "• ", ""));
            advertencia("Corrija los siguientes campos:", detalle);
        } else if (causa instanceof DYNegocioException ne) {
            error("No se pudo completar la operación", ne.getMessage());
        } else {
            mostrar(Alert.AlertType.ERROR, "Error", "Error inesperado", String.valueOf(e.getMessage()));
        }
    }

    private static void mostrar(Alert.AlertType tipo, String titulo, String encabezado, String mensaje) {
        crear(tipo, titulo, encabezado, mensaje).showAndWait();
    }

    private static Alert crear(Alert.AlertType tipo, String titulo, String encabezado, String mensaje) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(encabezado);
        a.setContentText(mensaje);
        DYUi.preparar(a.getDialogPane(), a);
        return a;
    }
}
