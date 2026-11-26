package com.ejemplo.bipro.controller;

import com.ejemplo.bipro.controller.dialogo.*;
import com.ejemplo.bipro.dto.*;
import com.ejemplo.bipro.model.*;
import com.ejemplo.bipro.service.*;
import com.ejemplo.bipro.ui.*;
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
public class CajaController {
    private final CajaService cajaService;
    private final EstadoApp estado;

    @FXML private VBox raiz;

    @FXML
    public void initialize() {
        pintar();
    }

    private void pintar() {
        raiz.getChildren().clear();
        Optional<TurnoCaja> turno = cajaService.turnoAbierto();
        if (turno.isEmpty()) {
            TextField txtMonto = new TextField("200.00");
            Button abrir = Ui.boton("Abrir caja", "fth-unlock", "accent");
            abrir.setOnAction(e -> {
                try {
                    cajaService.abrir(Formato.leerMonto(txtMonto.getText()));
                    estado.notificarCambio();
                    pintar();
                } catch (Exception ex) { Alertas.manejar(ex); }
            });
            VBox card = Ui.tarjeta("La caja está cerrada" + (cajaService.cerradaHoy() ? " (ya se cerró un turno hoy)" : ""),
                    "Ingresa el efectivo con el que inicia el turno para poder registrar ventas.",
                    new VBox(6, Ui.texto("Monto inicial (S/)", "etiqueta-campo"), txtMonto), abrir);
            ((VBox) card).setSpacing(14);
            card.setMaxWidth(460);
            raiz.getChildren().add(card);
        } else {
            TurnoCaja t = turno.get();
            ResumenCaja r = cajaService.resumen(t);
            HBox kpis = new HBox(16,
                    Ui.kpi("Ventas del turno", Formato.soles(r.totalVentas()), r.numeroVentas() + " ventas", "fth-shopping-cart", true, null),
                    Ui.kpi("Monto inicial", Formato.soles(t.getMontoInicial()), "abierta " + Formato.hora(t.getApertura()), "fth-unlock", false, null),
                    Ui.kpi("Efectivo esperado", Formato.soles(r.efectivoEsperado()), "inicial + cobros en efectivo", "fth-dollar-sign", false, null));
            VBox metodos = new VBox(10);
            for (MetodoPago m : MetodoPago.values()) {
                HBox f = new HBox(10, Ui.icono(m.getIcono(), 16), Ui.texto(m.getEtiqueta(), "dato-valor"), Ui.espaciador(),
                        Ui.texto(r.conteo().getOrDefault(m, 0L) + " ventas", "caption"),
                        Ui.texto(Formato.soles(r.montos().getOrDefault(m, BigDecimal.ZERO)), "dato-valor"));
                f.setAlignment(Pos.CENTER_LEFT);
                metodos.getChildren().add(f);
            }
            TextField txtContado = new TextField();
            txtContado.setPromptText("Efectivo contado (S/)");
            Label lblDif = Ui.texto("", "caption");
            txtContado.textProperty().addListener((o, a, b) -> {
                BigDecimal c = Formato.leerMonto(b);
                lblDif.setText(c == null ? "" : "Diferencia: " + Formato.soles(c.subtract(r.efectivoEsperado())));
            });
            Button cerrar = Ui.boton("Cerrar caja", "fth-lock", "accent");
            cerrar.setOnAction(e -> {
                try {
                    BigDecimal c = Formato.leerMonto(txtContado.getText());
                    if (c == null) throw new com.ejemplo.bipro.exception.NegocioException("Ingrese el efectivo contado.");
                    if (!Alertas.confirmar("¿Cerrar la caja?", "Se registrará el cierre del turno con " + Formato.soles(c) + " contados.")) return;
                    TurnoCaja cerrado = cajaService.cerrar(c);
                    estado.notificarCambio();
                    Alertas.info("Caja cerrada", "Ventas: " + Formato.soles(cerrado.getTotalVentas())
                            + "\nDiferencia: " + Formato.soles(cerrado.getDiferencia()));
                    pintar();
                } catch (Exception ex) { Alertas.manejar(ex); }
            });
            VBox cierre = Ui.tarjeta("Cierre de caja", "Cuenta el efectivo físico y registra el cierre.", txtContado, lblDif, cerrar);
            cierre.setSpacing(12);
            VBox met = Ui.tarjeta("Ventas por método de pago", null, metodos);
            met.setSpacing(12);
            HBox fila = new HBox(16, met, cierre);
            HBox.setHgrow(met, Priority.ALWAYS);
            cierre.setPrefWidth(380);
            raiz.getChildren().addAll(kpis, fila);
        }
        TableView<TurnoCaja> hist = new TableView<>(FXCollections.observableArrayList(cajaService.historial()));
        Ui.prepararTabla(hist, "Aún no hay turnos cerrados.");
        Ui.col(hist, "Apertura", 140, t -> Formato.fechaHora(t.getApertura()));
        Ui.col(hist, "Cajero", 140, t -> t.getUsuario().getNombre());
        Ui.col(hist, "Ventas", 70, t -> String.valueOf(t.getNumeroVentas()));
        Ui.col(hist, "Total", 110, t -> Formato.soles(t.getTotalVentas()));
        Ui.col(hist, "Esperado", 110, t -> Formato.soles(t.getEfectivoEsperado()));
        Ui.col(hist, "Contado", 110, t -> Formato.soles(t.getEfectivoContado()));
        Ui.colNodo(hist, "Diferencia", 110, t -> Ui.badge(Formato.soles(t.getDiferencia()),
                t.getDiferencia().signum() == 0 ? "exito" : "advertencia"));
        VBox.setVgrow(hist, Priority.ALWAYS);
        raiz.getChildren().addAll(Ui.texto("Historial de turnos", "h3"), hist);
    }
}
