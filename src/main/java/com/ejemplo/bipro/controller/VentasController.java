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
public class VentasController implements Recargable {
    private final VentaService ventaService;
    private final VentaDialogos ventaDialogos;
    private final ReporteService reporteService;
    private final EstadoApp estado;

    @FXML private VBox raiz;
    private final DatePicker dpDia = new DatePicker(LocalDate.now());
    private final TableView<Venta> tabla = new TableView<>();
    private final HBox kpis = new HBox(16);

    @FXML
    public void initialize() {
        Button nueva = Ui.boton("Nueva venta", "fth-plus", "accent");
        nueva.setOnAction(e -> { ventaDialogos.nuevaVenta(null); recargar(); });
        dpDia.valueProperty().addListener((o, a, b) -> recargar());
        HBox barra = new HBox(12, Ui.texto("Ventas del día", "h3"), dpDia, Ui.espaciador(), nueva);
        barra.setAlignment(Pos.CENTER_LEFT);

        Ui.prepararTabla(tabla, "No hay ventas registradas en esta fecha.");
        Ui.col(tabla, "N°", 100, Venta::getNumero);
        Ui.col(tabla, "Hora", 70, v -> Formato.hora(v.getFecha()));
        Ui.col(tabla, "Cliente", 220, Venta::getClienteNombre);
        Ui.col(tabla, "Pago", 120, v -> v.getMetodoPago().getEtiqueta());
        Ui.col(tabla, "Cajero", 140, v -> v.getUsuario().getNombre());
        Ui.col(tabla, "Total", 110, v -> Formato.soles(v.getTotal()));
        Ui.colNodo(tabla, "Estado", 100, v -> Ui.badge(v.getEstado().getEtiqueta(),
                v.getEstado() == EstadoVenta.ANULADA ? "peligro" : "exito"));
        tabla.setOnMouseClicked(e -> {
            Venta v = tabla.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && v != null) { ventaDialogos.comprobante(v.getId()); recargar(); }
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);
        raiz.getChildren().addAll(kpis, barra, tabla, Ui.texto("Doble clic en una venta para ver el comprobante.", "caption"));
        recargar();
    }

    @Override
    public void recargar() {
        RangoFechas r = estado.getRango();
        Kpis k = reporteService.kpis(r.actual(LocalDate.now()));
        kpis.getChildren().setAll(
                Ui.kpi("Ventas · " + r.getEtiqueta(), Formato.soles(k.ventas()), k.transacciones() + " transacciones", "fth-trending-up", true, null),
                Ui.kpi("Ticket promedio", Formato.soles(k.ticketPromedio()), "por venta", "fth-shopping-bag", false, null),
                Ui.kpi("Margen bruto", k.margenBruto() + "%", "ventas − costo", "fth-percent", false, null));
        tabla.getItems().setAll(ventaService.ventasDelDia(dpDia.getValue()));
    }
}
