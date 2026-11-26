package com.dy.bipro.controller;

import com.dy.bipro.controller.dialogo.DYFormDialogos;
import com.dy.bipro.controller.dialogo.DYVentaDialogos;
import com.dy.bipro.dto.DYIntervalo;
import com.dy.bipro.dto.DYKpis;
import com.dy.bipro.dto.DYNotificacion;
import com.dy.bipro.dto.DYResumenCaja;
import com.dy.bipro.model.DYRangoFechas;
import com.dy.bipro.model.DYTurnoCaja;
import com.dy.bipro.service.*;
import com.dy.bipro.ui.*;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DYDashboardController implements DYRecargable {
    private final DYReporteService reporteService;
    private final DYDiagnosticoService diagnosticoService;
    private final DYCajaService cajaService;
    private final DYCompraService compraService;
    private final DYProductoService productoService;
    private final DYEstadoApp estado;
    private final DYSesion sesion;
    private final DYVentaDialogos ventaDialogos;
    private final DYFormDialogos formDialogos;

    @FXML private Label lblSaludo;
    @FXML private Label lblResumen;
    @FXML private Label lblPendientes;
    @FXML private Label lblFlujo;
    @FXML private Label lblTendencia;
    @FXML private HBox boxFlujo;
    @FXML private HBox boxKpis;
    @FXML private LineChart<String, Number> grafTendencia;
    @FXML private ListView<DYNotificacion> lstDiagnostico;
    @FXML private Button btnNuevaVenta;
    @FXML private Button btnOrden;

    @FXML
    public void initialize() {
        btnNuevaVenta.setGraphic(DYUi.icono("fth-plus", 16));
        btnOrden.setGraphic(DYUi.icono("fth-clipboard", 16));
        lstDiagnostico.setCellFactory(l -> new DYNotificacionCell(this::abrir, sesion::leida));
        recargar();
    }

    @Override
    public void recargar() {
        LocalDate hoy = LocalDate.now();
        DYRangoFechas rango = estado.getRango();
        DYIntervalo ia = rango.actual(hoy);
        DYIntervalo ib = rango.anterior(hoy);
        DYKpis a = reporteService.kpis(ia);
        DYKpis b = reporteService.kpis(ib);

        int hora = LocalTime.now().getHour();
        String saludo = hora < 12 ? "Buenos días" : hora < 19 ? "Buenas tardes" : "Buenas noches";
        lblSaludo.setText(saludo + ", " + sesion.getUsuario().getNombre().split(" ")[0]);
        double vVentas = DYReporteService.variacion(a.ventas(), b.ventas());
        lblResumen.setText("Llevas " + DYFormato.soles(a.ventas()) + " en ventas (" + rango.getEtiqueta().toLowerCase()
                + "), " + DYFormato.variacion(vVentas) + " " + rango.comparacion() + ".");
        int porReponer = compraService.sugerencia().size();
        Optional<DYTurnoCaja> turno = cajaService.turnoAbierto();
        lblPendientes.setText(porReponer + " productos sin orden de compra · " + compraService.pendientes()
                + " órdenes por recibir · caja " + (turno.isPresent() ? "abierta" : "cerrada"));

        long criticos = productoService.listarTodos().stream()
                .filter(p -> p.getStockActual() < p.getStockMinimo()).count();
        double dMargen = a.margenBruto().subtract(b.margenBruto()).doubleValue();
        double dTicket = DYReporteService.variacion(a.ticketPromedio(), b.ticketPromedio());
        boxKpis.getChildren().setAll(
                DYUi.kpi("Ventas", DYFormato.soles(a.ventas()), DYFormato.variacion(vVentas) + " " + rango.comparacion(),
                        "fth-trending-up", true, vVentas >= 0),
                DYUi.kpi("Margen bruto", a.margenBruto() + "%",
                        String.format(Locale.US, "%s%.1f pts %s", dMargen >= 0 ? "+" : "−", Math.abs(dMargen), rango.comparacion()),
                        "fth-percent", false, dMargen >= 0),
                DYUi.kpi("Ticket promedio", DYFormato.soles(a.ticketPromedio()),
                        DYFormato.variacion(dTicket) + " · " + a.transacciones() + " ventas", "fth-shopping-bag", false, dTicket >= 0),
                DYUi.kpi("Stock crítico", String.valueOf(criticos), "productos bajo el mínimo", "fth-alert-triangle",
                        false, criticos == 0));

        construirFlujo(turno, porReponer);
        construirGrafico(rango, ia, ib);
        lstDiagnostico.getItems().setAll(diagnosticoService.generar());
    }

    private void construirFlujo(Optional<DYTurnoCaja> turno, int porReponer) {
        boolean abierta = turno.isPresent();
        boolean cerradaHoy = cajaService.cerradaHoy();
        DYResumenCaja rc = turno.map(cajaService::resumen).orElse(null);

        String[] titulos = {"Abrir caja", "Registrar ventas", "Reponer stock", "Cerrar caja"};
        String[] detalles = {
                abierta ? "Abierta " + DYFormato.hora(turno.get().getApertura()) : cerradaHoy ? "Turno cerrado" : "Pendiente de apertura",
                rc != null ? rc.numeroVentas() + " ventas · " + DYFormato.soles(rc.totalVentas()) : "Requiere caja abierta",
                porReponer == 0 ? "Reposición en curso" : porReponer + " productos sin orden",
                abierta ? "Al terminar el turno" : cerradaHoy ? "Cierre registrado" : "—"};
        boolean[] hecho = {abierta || cerradaHoy, (rc != null && rc.numeroVentas() > 0) || (cerradaHoy && !abierta),
                porReponer == 0, cerradaHoy && !abierta};
        DYModulo[] destino = {DYModulo.CAJA, DYModulo.VENTAS, DYModulo.INVENTARIO, DYModulo.CAJA};

        int actual = -1;
        for (int i = 0; i < 4; i++) if (!hecho[i]) { actual = i; break; }

        boxFlujo.getChildren().clear();
        int completos = 0;
        for (int i = 0; i < 4; i++) {
            Label num = new Label(hecho[i] ? null : String.valueOf(i + 1));
            if (hecho[i]) num.setGraphic(DYUi.icono("fth-check", 14));
            num.getStyleClass().add("paso-num");
            HBox paso = new HBox(12, num, new VBox(2, DYUi.texto(titulos[i], "paso-titulo"), DYUi.texto(detalles[i], "caption")));
            paso.getStyleClass().add("paso");
            if (hecho[i]) { paso.getStyleClass().add("paso-hecho"); completos++; }
            if (i == actual) paso.getStyleClass().add("paso-actual");
            HBox.setHgrow(paso, Priority.ALWAYS);
            paso.setMaxWidth(Double.MAX_VALUE);
            DYModulo m = destino[i];
            paso.setOnMouseClicked(e -> estado.ir(m));
            boxFlujo.getChildren().add(paso);
        }
        lblFlujo.setText(completos + " de 4 completados");
    }

    private void construirGrafico(DYRangoFechas rango, DYIntervalo ia, DYIntervalo ib) {
        XYChart.Series<String, Number> actual = new XYChart.Series<>();
        XYChart.Series<String, Number> previo = new XYChart.Series<>();
        actual.setName("Periodo actual");
        previo.setName("Periodo anterior");

        if (rango == DYRangoFechas.HOY) {
            lblTendencia.setText("Ingresos acumulados por hora · hoy");
            Map<Integer, BigDecimal> hoy = reporteService.serieHoraria(ia.desde());
            Map<Integer, BigDecimal> ant = reporteService.serieHoraria(ib.desde());
            BigDecimal acA = BigDecimal.ZERO, acB = BigDecimal.ZERO;
            int horaActual = Math.max(LocalTime.now().getHour(), 9);
            for (int h = 8; h <= 21; h++) {
                String x = String.format("%02d:00", h);
                acB = acB.add(ant.get(h));
                previo.getData().add(new XYChart.Data<>(x, acB));
                acA = acA.add(hoy.get(h));
                if (h <= horaActual) actual.getData().add(new XYChart.Data<>(x, acA));
            }
        } else {
            List<Map.Entry<LocalDate, BigDecimal>> a = new ArrayList<>(reporteService.serieDiaria(ia).entrySet());
            List<Map.Entry<LocalDate, BigDecimal>> b = new ArrayList<>(reporteService.serieDiaria(ib).entrySet());
            int paso = ia.dias() > 62 ? 7 : 1;
            lblTendencia.setText((paso == 7 ? "Ingresos semanales · " : "Ingresos diarios · ") + rango.getEtiqueta());
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM", DYFormato.PERU);
            for (int i = 0; i < a.size(); i += paso) {
                String x = a.get(i).getKey().format(f);
                actual.getData().add(new XYChart.Data<>(x, suma(a, i, paso)));
                previo.getData().add(new XYChart.Data<>(x, suma(b, i, paso)));
            }
        }
        grafTendencia.getData().setAll(List.of(actual, previo));
    }

    private static BigDecimal suma(List<Map.Entry<LocalDate, BigDecimal>> lista, int desde, int n) {
        BigDecimal s = BigDecimal.ZERO;
        for (int i = desde; i < Math.min(lista.size(), desde + n); i++) s = s.add(lista.get(i).getValue());
        return s;
    }

    private void abrir(DYNotificacion n) {
        sesion.marcarLeida(n.id());
        estado.notificarCambio();
        estado.ir(DYModulo.valueOf(n.modulo()));
    }

    @FXML
    private void onNuevaVenta() {
        ventaDialogos.nuevaVenta(null);
        recargar();
    }

    @FXML
    private void onOrdenSugerida() {
        formDialogos.ordenSugerida();
        recargar();
    }
}
