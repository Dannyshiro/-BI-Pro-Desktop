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
public class FinanzasController implements Recargable {
    private final ReporteService reporteService;
    private final GastoService gastoService;
    private final FormDialogos formDialogos;
    private final EstadoApp estado;

    @FXML private VBox raiz;
    private final HBox kpis = new HBox(16);
    private final BarChart<String, Number> barras = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final PieChart torta = new PieChart();
    private final TableView<Gasto> tabla = new TableView<>();

    @FXML
    public void initialize() {
        barras.setAnimated(false);
        barras.setPrefHeight(300);
        torta.setAnimated(false);
        torta.setPrefHeight(300);
        torta.setLegendVisible(true);
        VBox c1 = Ui.tarjeta("Ingresos vs egresos", "Últimos 6 meses (egresos = costo de ventas + gastos)", barras);
        VBox c2 = Ui.tarjeta("Gastos por categoría", "Rango de fechas seleccionado", torta);
        HBox graficos = new HBox(16, c1, c2);
        HBox.setHgrow(c1, Priority.ALWAYS);
        c2.setPrefWidth(420);

        Ui.prepararTabla(tabla, "No hay gastos registrados.");
        Ui.col(tabla, "Fecha", 90, g -> Formato.fecha(g.getFecha()));
        Ui.colNodo(tabla, "Categoría", 110, g -> Ui.badge(g.getCategoria().getEtiqueta(), "neutro"));
        Ui.col(tabla, "Descripción", 280, g -> Formato.vacio(g.getDescripcion()));
        Ui.col(tabla, "Monto", 100, g -> Formato.soles(g.getMonto()));
        Ui.col(tabla, "Comprobante", 140, g -> g.getComprobante() == null ? "—" : new java.io.File(g.getComprobante()).getName());
        Ui.colNodo(tabla, "", 90, g -> new HBox(4,
                Ui.botonIcono("fth-edit-2", "Editar", () -> { if (formDialogos.gasto(g)) recargar(); }),
                Ui.botonIcono("fth-trash-2", "Eliminar", () -> {
                    if (Alertas.confirmar("¿Eliminar este gasto?", g.getDescripcion())) {
                        try { gastoService.eliminar(g); estado.notificarCambio(); recargar(); } catch (Exception e) { Alertas.manejar(e); }
                    }
                })));
        tabla.setPrefHeight(300);
        Button nuevo = Ui.boton("Registrar gasto", "fth-plus", "accent");
        nuevo.setOnAction(e -> { if (formDialogos.gasto(null)) recargar(); });
        HBox barra = new HBox(12, Ui.texto("Gastos operativos", "h3"), Ui.espaciador(), nuevo);
        barra.setAlignment(Pos.CENTER_LEFT);
        raiz.getChildren().addAll(kpis, graficos, barra, tabla);
        recargar();
    }

    @Override
    public void recargar() {
        RangoFechas r = estado.getRango();
        Kpis a = reporteService.kpis(r.actual(LocalDate.now()));
        Kpis b = reporteService.kpis(r.anterior(LocalDate.now()));
        double vu = ReporteService.variacion(a.utilidad(), b.utilidad());
        kpis.getChildren().setAll(
                Ui.kpi("Ingresos", Formato.soles(a.ventas()), r.getEtiqueta(), "fth-trending-up", true, null),
                Ui.kpi("Gastos operativos", Formato.soles(a.gastos()), "costo de ventas aparte: " + Formato.soles(a.costo()), "fth-credit-card", false, null),
                Ui.kpi("Utilidad neta", Formato.soles(a.utilidad()), Formato.variacion(vu) + " " + r.comparacion(), "fth-award", false, vu >= 0),
                Ui.kpi("Margen neto", a.margenNeto() + "%", "utilidad / ingresos", "fth-percent", false, null));

        XYChart.Series<String, Number> ing = new XYChart.Series<>();
        XYChart.Series<String, Number> egr = new XYChart.Series<>();
        ing.setName("Ingresos");
        egr.setName("Egresos");
        for (MesResumen m : reporteService.ultimosMeses(6)) {
            String x = Formato.capitalizar(m.mes().getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Formato.PERU));
            ing.getData().add(new XYChart.Data<>(x, m.ingresos()));
            egr.getData().add(new XYChart.Data<>(x, m.egresos()));
        }
        barras.getData().setAll(List.of(ing, egr));

        torta.getData().setAll(reporteService.gastosPorCategoria(r.actual(LocalDate.now())).entrySet().stream()
                .map(e -> new PieChart.Data(e.getKey().getEtiqueta() + " " + Formato.solesEntero(e.getValue()), e.getValue().doubleValue()))
                .toList());
        tabla.getItems().setAll(gastoService.listar());
    }
}
