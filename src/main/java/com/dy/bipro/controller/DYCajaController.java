package com.dy.bipro.controller;

import com.dy.bipro.controller.dialogo.*;
import com.dy.bipro.dto.*;
import com.dy.bipro.model.*;
import com.dy.bipro.service.*;
import com.dy.bipro.ui.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DYCajaController {
    private final DYCajaService cajaService;
    private final DYEstadoApp estado;

    @FXML private VBox raiz;

    @FXML
    public void initialize() {
        pintar();
    }

    private void pintar() {
        raiz.getChildren().clear();
        Optional<DYTurnoCaja> turno = cajaService.turnoAbierto();
        if (turno.isEmpty()) {
            TextField txtMonto = new TextField("200.00");
            Button abrir = DYUi.boton("Abrir caja", "fth-unlock", "accent");
            abrir.setOnAction(e -> {
                try {
                    cajaService.abrir(DYFormato.leerMonto(txtMonto.getText()));
                    estado.notificarCambio();
                    pintar();
                } catch (Exception ex) { DYAlertas.manejar(ex); }
            });
            VBox card = DYUi.tarjeta("La caja está cerrada" + (cajaService.cerradaHoy() ? " (ya se cerró un turno hoy)" : ""),
                    "Ingresa el efectivo con el que inicia el turno para poder registrar ventas.",
                    new VBox(6, DYUi.texto("Monto inicial (S/)", "etiqueta-campo"), txtMonto), abrir);
            ((VBox) card).setSpacing(14);
            card.setMaxWidth(460);
            raiz.getChildren().add(card);
        } else {
            DYTurnoCaja t = turno.get();
            DYResumenCaja r = cajaService.resumen(t);
            HBox kpis = new HBox(16,
                    DYUi.kpi("Ventas del turno", DYFormato.soles(r.totalVentas()), r.numeroVentas() + " ventas", "fth-shopping-cart", true, null),
                    DYUi.kpi("Monto inicial", DYFormato.soles(t.getMontoInicial()), "abierta " + DYFormato.hora(t.getApertura()), "fth-unlock", false, null),
                    DYUi.kpi("Efectivo esperado", DYFormato.soles(r.efectivoEsperado()), "inicial + cobros en efectivo", "fth-dollar-sign", false, null));
            VBox metodos = new VBox(10);
            for (DYMetodoPago m : DYMetodoPago.values()) {
                HBox f = new HBox(10, DYUi.icono(m.getIcono(), 16), DYUi.texto(m.getEtiqueta(), "dato-valor"), DYUi.espaciador(),
                        DYUi.texto(r.conteo().getOrDefault(m, 0L) + " ventas", "caption"),
                        DYUi.texto(DYFormato.soles(r.montos().getOrDefault(m, BigDecimal.ZERO)), "dato-valor"));
                f.setAlignment(Pos.CENTER_LEFT);
                metodos.getChildren().add(f);
            }
            TextField txtContado = new TextField();
            txtContado.setPromptText("Efectivo contado (S/)");
            Label lblDif = DYUi.texto("", "caption");
            txtContado.textProperty().addListener((o, a, b) -> {
                BigDecimal c = DYFormato.leerMonto(b);
                lblDif.setText(c == null ? "" : "Diferencia: " + DYFormato.soles(c.subtract(r.efectivoEsperado())));
            });
            Button cerrar = DYUi.boton("Cerrar caja", "fth-lock", "accent");
            cerrar.setOnAction(e -> {
                try {
                    BigDecimal c = DYFormato.leerMonto(txtContado.getText());
                    if (c == null) throw new com.dy.bipro.exception.DYNegocioException("Ingrese el efectivo contado.");
                    if (!DYAlertas.confirmar("¿Cerrar la caja?", "Se registrará el cierre del turno con " + DYFormato.soles(c) + " contados.")) return;
                    DYTurnoCaja cerrado = cajaService.cerrar(c);
                    estado.notificarCambio();
                    DYAlertas.info("Caja cerrada", "Ventas: " + DYFormato.soles(cerrado.getTotalVentas())
                            + "\nDiferencia: " + DYFormato.soles(cerrado.getDiferencia()));
                    pintar();
                } catch (Exception ex) { DYAlertas.manejar(ex); }
            });
            VBox cierre = DYUi.tarjeta("Cierre de caja", "Cuenta el efectivo físico y registra el cierre.", txtContado, lblDif, cerrar);
            cierre.setSpacing(12);
            VBox met = DYUi.tarjeta("Ventas por método de pago", null, metodos);
            met.setSpacing(12);
            HBox fila = new HBox(16, met, cierre);
            HBox.setHgrow(met, Priority.ALWAYS);
            cierre.setPrefWidth(380);
            raiz.getChildren().addAll(kpis, fila);
        }
        TableView<DYTurnoCaja> hist = new TableView<>(FXCollections.observableArrayList(cajaService.historial()));
        DYUi.prepararTabla(hist, "Aún no hay turnos cerrados.");
        DYUi.col(hist, "Apertura", 140, t -> DYFormato.fechaHora(t.getApertura()));
        DYUi.col(hist, "Cajero", 140, t -> t.getUsuario().getNombre());
        DYUi.col(hist, "Ventas", 70, t -> String.valueOf(t.getNumeroVentas()));
        DYUi.col(hist, "Total", 110, t -> DYFormato.soles(t.getTotalVentas()));
        DYUi.col(hist, "Esperado", 110, t -> DYFormato.soles(t.getEfectivoEsperado()));
        DYUi.col(hist, "Contado", 110, t -> DYFormato.soles(t.getEfectivoContado()));
        DYUi.colNodo(hist, "Diferencia", 110, t -> DYUi.badge(DYFormato.soles(t.getDiferencia()),
                t.getDiferencia().signum() == 0 ? "exito" : "advertencia"));
        VBox.setVgrow(hist, Priority.ALWAYS);
        raiz.getChildren().addAll(DYUi.texto("Historial de turnos", "h3"), hist);
    }
}
