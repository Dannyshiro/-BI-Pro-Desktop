package com.ejemplo.bipro.controller;

import com.ejemplo.bipro.controller.dialogo.FormDialogos;
import com.ejemplo.bipro.controller.dialogo.VentaDialogos;
import com.ejemplo.bipro.dto.Intervalo;
import com.ejemplo.bipro.dto.Kpis;
import com.ejemplo.bipro.dto.Notificacion;
import com.ejemplo.bipro.dto.ResumenCaja;
import com.ejemplo.bipro.model.RangoFechas;
import com.ejemplo.bipro.model.TurnoCaja;
import com.ejemplo.bipro.service.*;
import com.ejemplo.bipro.ui.*;
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
public class DashboardController implements Recargable {
    private final ReporteService reporteService;
    private final DiagnosticoService diagnosticoService;
    private final CajaService cajaService;
    private final CompraService compraService;
    private final ProductoService productoService;
    private final EstadoApp estado;
    private final Sesion sesion;
    private final VentaDialogos ventaDialogos;
    private final FormDialogos formDialogos;

    @FXML private Label lblSaludo;
    @FXML private Label lblResumen;
    @FXML private Label lblPendientes;
    @FXML private Label lblFlujo;
    @FXML private Label lblTendencia;
    @FXML private HBox boxFlujo;
    @FXML private HBox boxKpis;
    @FXML private LineChart<String, Number> grafTendencia;
    @FXML private ListView<Notificacion> lstDiagnostico;
    @FXML private Button btnNuevaVenta;
    @FXML private Button btnOrden;

    @FXML
    public void initialize() {
        btnNuevaVenta.setGraphic(Ui.icono("fth-plus", 16));
        btnOrden.setGraphic(Ui.icono("fth-clipboard", 16));
        lstDiagnostico.setCellFactory(l -> new NotificacionCell(this::abrir, sesion::leida));
        recargar();
    }

    @Override
    public void recargar() {
        LocalDate hoy = LocalDate.now();
        RangoFechas rango = estado.getRango();
        Intervalo ia = rango.actual(hoy);
        Intervalo ib = rango.anterior(hoy);
        Kpis a = reporteService.kpis(ia);
        Kpis b = reporteService.kpis(ib);

        int hora = LocalTime.now().getHour();
        String saludo = hora < 12 ? "Buenos días" : hora < 19 ? "Buenas tardes" : "Buenas noches";
        lblSaludo.setText(saludo + ", " + sesion.getUsuario().getNombre().split(" ")[0]);
        double vVentas = ReporteService.variacion(a.ventas(), b.ventas());
        lblResumen.setText("Llevas " + Formato.soles(a.ventas()) + " en ventas (" + rango.getEtiqueta().toLowerCase()
                + "), " + Formato.variacion(vVentas) + " " + rango.comparacion() + ".");
        int porReponer = compraService.sugerencia().size();
        Optional<TurnoCaja> turno = cajaService.turnoAbierto();
        lblPendientes.setText(porReponer + " productos sin orden de compra · " + compraService.pendientes()
                + " órdenes por recibir · caja " + (turno.isPresent() ? "abierta" : "cerrada"));

        long criticos = productoService.listarTodos().stream()
                .filter(p -> p.getStockActual() < p.getStockMinimo()).count();
        double dMargen = a.margenBruto().subtract(b.margenBruto()).doubleValue();
        double dTicket = ReporteService.variacion(a.ticketPromedio(), b.ticketPromedio());
        boxKpis.getChildren().setAll(
                Ui.kpi("Ventas", Formato.soles(a.ventas()), Formato.variacion(vVentas) + " " + rango.comparacion(),
                        "fth-trending-up", true, vVentas >= 0),
                Ui.kpi("Margen bruto", a.margenBruto() + "%",
                        String.format(Locale.US, "%s%.1f pts %s", dMargen >= 0 ? "+" : "−", Math.abs(dMargen), rango.comparacion()),
                        "fth-percent", false, dMargen >= 0),
                Ui.kpi("Ticket promedio", Formato.soles(a.ticketPromedio()),
                        Formato.variacion(dTicket) + " · " + a.transacciones() + " ventas", "fth-shopping-bag", false, dTicket >= 0),
                Ui.kpi("Stock crítico", String.valueOf(criticos), "productos bajo el mínimo", "fth-alert-triangle",
                        false, criticos == 0));

        construirFlujo(turno, porReponer);
        construirGrafico(rango, ia, ib);
        lstDiagnostico.getItems().setAll(diagnosticoService.generar());
    }

    private void construirFlujo(Optional<TurnoCaja> turno, int porReponer) {
        boolean abierta = turno.isPresent();
        boolean cerradaHoy = cajaService.cerradaHoy();
        ResumenCaja rc = turno.map(cajaService::resumen).orElse(null);

        String[] titulos = {"Abrir caja", "Registrar ventas", "Reponer stock", "Cerrar caja"};
        String[] detalles = {
                abierta ? "Abierta " + Formato.hora(turno.get().getApertura()) : cerradaHoy ? "Turno cerrado" : "Pendiente de apertura",
                rc != null ? rc.numeroVentas() + " ventas · " + Formato.soles(rc.totalVentas()) : "Requiere caja abierta",
                porReponer == 0 ? "Reposición en curso" : porReponer + " productos sin orden",
                abierta ? "Al terminar el turno" : cerradaHoy ? "Cierre registrado" : "—"};
        boolean[] hecho = {abierta || cerradaHoy, (rc != null && rc.numeroVentas() > 0) || (cerradaHoy && !abierta),
                porReponer == 0, cerradaHoy && !abierta};
        Modulo[] destino = {Modulo.CAJA, Modulo.VENTAS, Modulo.INVENTARIO, Modulo.CAJA};

        int actual = -1;
        for (int i = 0; i < 4; i++) if (!hecho[i]) { actual = i; break; }

        boxFlujo.getChildren().clear();
        int completos = 0;
        for (int i = 0; i < 4; i++) {
            Label num = new Label(hecho[i] ? null : String.valueOf(i + 1));
            if (hecho[i]) num.setGraphic(Ui.icono("fth-check", 14));
            num.getStyleClass().add("paso-num");
            HBox paso = new HBox(12, num, new VBox(2, Ui.texto(titulos[i], "paso-titulo"), Ui.texto(detalles[i], "caption")));
            paso.getStyleClass().add("paso");
            if (hecho[i]) { paso.getStyleClass().add("paso-hecho"); completos++; }
            if (i == actual) paso.getStyleClass().add("paso-actual");
            HBox.setHgrow(paso, Priority.ALWAYS);
            paso.setMaxWidth(Double.MAX_VALUE);
            Modulo m = destino[i];
            paso.setOnMouseClicked(e -> estado.ir(m));
            boxFlujo.getChildren().add(paso);
        }
        lblFlujo.setText(completos + " de 4 completados");
    }

    private void construirGrafico(RangoFechas rango, Intervalo ia, Intervalo ib) {
        XYChart.Series<String, Number> actual = new XYChart.Series<>();
        XYChart.Series<String, Number> previo = new XYChart.Series<>();
        actual.setName("Periodo actual");
        previo.setName("Periodo anterior");

        if (rango == RangoFechas.HOY) {
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
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM", Formato.PERU);
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

    private void abrir(Notificacion n) {
        sesion.marcarLeida(n.id());
        estado.notificarCambio();
        estado.ir(Modulo.valueOf(n.modulo()));
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
