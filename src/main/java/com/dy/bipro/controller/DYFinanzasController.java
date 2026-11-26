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
public class DYFinanzasController implements DYRecargable {
    private final DYReporteService reporteService;
    private final DYGastoService gastoService;
    private final DYFormDialogos formDialogos;
    private final DYEstadoApp estado;

    @FXML private VBox raiz;
    private final HBox kpis = new HBox(16);
    private final BarChart<String, Number> barras = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final PieChart torta = new PieChart();
    private final TableView<DYGasto> tabla = new TableView<>();

    @FXML
    public void initialize() {
        barras.setAnimated(false);
        barras.setPrefHeight(300);
        torta.setAnimated(false);
        torta.setPrefHeight(300);
        torta.setLegendVisible(true);
        VBox c1 = DYUi.tarjeta("Ingresos vs egresos", "Últimos 6 meses (egresos = costo de ventas + gastos)", barras);
        VBox c2 = DYUi.tarjeta("Gastos por categoría", "Rango de fechas seleccionado", torta);
        HBox graficos = new HBox(16, c1, c2);
        HBox.setHgrow(c1, Priority.ALWAYS);
        c2.setPrefWidth(420);

        DYUi.prepararTabla(tabla, "No hay gastos registrados.");
        DYUi.col(tabla, "Fecha", 90, g -> DYFormato.fecha(g.getFecha()));
        DYUi.colNodo(tabla, "Categoría", 110, g -> DYUi.badge(g.getCategoria().getEtiqueta(), "neutro"));
        DYUi.col(tabla, "Descripción", 280, g -> DYFormato.vacio(g.getDescripcion()));
        DYUi.col(tabla, "Monto", 100, g -> DYFormato.soles(g.getMonto()));
        DYUi.col(tabla, "Comprobante", 140, g -> g.getComprobante() == null ? "—" : new java.io.File(g.getComprobante()).getName());
        DYUi.colNodo(tabla, "", 90, g -> new HBox(4,
                DYUi.botonIcono("fth-edit-2", "Editar", () -> { if (formDialogos.gasto(g)) recargar(); }),
                DYUi.botonIcono("fth-trash-2", "Eliminar", () -> {
                    if (DYAlertas.confirmar("¿Eliminar este gasto?", g.getDescripcion())) {
                        try { gastoService.eliminar(g); estado.notificarCambio(); recargar(); } catch (Exception e) { DYAlertas.manejar(e); }
                    }
                })));
        tabla.setPrefHeight(300);
        Button nuevo = DYUi.boton("Registrar gasto", "fth-plus", "accent");
        nuevo.setOnAction(e -> { if (formDialogos.gasto(null)) recargar(); });
        HBox barra = new HBox(12, DYUi.texto("Gastos operativos", "h3"), DYUi.espaciador(), nuevo);
        barra.setAlignment(Pos.CENTER_LEFT);
        raiz.getChildren().addAll(kpis, graficos, barra, tabla);
        recargar();
    }

    @Override
    public void recargar() {
        DYRangoFechas r = estado.getRango();
        DYKpis a = reporteService.kpis(r.actual(LocalDate.now()));
        DYKpis b = reporteService.kpis(r.anterior(LocalDate.now()));
        double vu = DYReporteService.variacion(a.utilidad(), b.utilidad());
        kpis.getChildren().setAll(
                DYUi.kpi("Ingresos", DYFormato.soles(a.ventas()), r.getEtiqueta(), "fth-trending-up", true, null),
                DYUi.kpi("Gastos operativos", DYFormato.soles(a.gastos()), "costo de ventas aparte: " + DYFormato.soles(a.costo()), "fth-credit-card", false, null),
                DYUi.kpi("Utilidad neta", DYFormato.soles(a.utilidad()), DYFormato.variacion(vu) + " " + r.comparacion(), "fth-award", false, vu >= 0),
                DYUi.kpi("Margen neto", a.margenNeto() + "%", "utilidad / ingresos", "fth-percent", false, null));

        XYChart.Series<String, Number> ing = new XYChart.Series<>();
        XYChart.Series<String, Number> egr = new XYChart.Series<>();
        ing.setName("Ingresos");
        egr.setName("Egresos");
        for (DYMesResumen m : reporteService.ultimosMeses(6)) {
            String x = DYFormato.capitalizar(m.mes().getMonth().getDisplayName(java.time.format.TextStyle.SHORT, DYFormato.PERU));
            ing.getData().add(new XYChart.Data<>(x, m.ingresos()));
            egr.getData().add(new XYChart.Data<>(x, m.egresos()));
        }
        barras.getData().setAll(List.of(ing, egr));

        torta.getData().setAll(reporteService.gastosPorCategoria(r.actual(LocalDate.now())).entrySet().stream()
                .map(e -> new PieChart.Data(e.getKey().getEtiqueta() + " " + DYFormato.solesEntero(e.getValue()), e.getValue().doubleValue()))
                .toList());
        tabla.getItems().setAll(gastoService.listar());
    }
}
