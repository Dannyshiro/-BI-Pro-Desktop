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
public class DYVentasController implements DYRecargable {
    private final DYVentaService ventaService;
    private final DYVentaDialogos ventaDialogos;
    private final DYReporteService reporteService;
    private final DYEstadoApp estado;

    @FXML private VBox raiz;
    private final DatePicker dpDia = new DatePicker(LocalDate.now());
    private final TableView<DYVenta> tabla = new TableView<>();
    private final HBox kpis = new HBox(16);

    @FXML
    public void initialize() {
        Button nueva = DYUi.boton("Nueva venta", "fth-plus", "accent");
        nueva.setOnAction(e -> { ventaDialogos.nuevaVenta(null); recargar(); });
        dpDia.valueProperty().addListener((o, a, b) -> recargar());
        HBox barra = new HBox(12, DYUi.texto("Ventas del día", "h3"), dpDia, DYUi.espaciador(), nueva);
        barra.setAlignment(Pos.CENTER_LEFT);

        DYUi.prepararTabla(tabla, "No hay ventas registradas en esta fecha.");
        DYUi.col(tabla, "N°", 100, DYVenta::getNumero);
        DYUi.col(tabla, "Hora", 70, v -> DYFormato.hora(v.getFecha()));
        DYUi.col(tabla, "Cliente", 220, DYVenta::getClienteNombre);
        DYUi.col(tabla, "Pago", 120, v -> v.getMetodoPago().getEtiqueta());
        DYUi.col(tabla, "Cajero", 140, v -> v.getUsuario().getNombre());
        DYUi.col(tabla, "Total", 110, v -> DYFormato.soles(v.getTotal()));
        DYUi.colNodo(tabla, "Estado", 100, v -> DYUi.badge(v.getEstado().getEtiqueta(),
                v.getEstado() == DYEstadoVenta.ANULADA ? "peligro" : "exito"));
        tabla.setOnMouseClicked(e -> {
            DYVenta v = tabla.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && v != null) { ventaDialogos.comprobante(v.getId()); recargar(); }
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);
        raiz.getChildren().addAll(kpis, barra, tabla, DYUi.texto("Doble clic en una venta para ver el comprobante.", "caption"));
        recargar();
    }

    @Override
    public void recargar() {
        DYRangoFechas r = estado.getRango();
        DYKpis k = reporteService.kpis(r.actual(LocalDate.now()));
        kpis.getChildren().setAll(
                DYUi.kpi("Ventas · " + r.getEtiqueta(), DYFormato.soles(k.ventas()), k.transacciones() + " transacciones", "fth-trending-up", true, null),
                DYUi.kpi("Ticket promedio", DYFormato.soles(k.ticketPromedio()), "por venta", "fth-shopping-bag", false, null),
                DYUi.kpi("Margen bruto", k.margenBruto() + "%", "ventas − costo", "fth-percent", false, null));
        tabla.getItems().setAll(ventaService.ventasDelDia(dpDia.getValue()));
    }
}
