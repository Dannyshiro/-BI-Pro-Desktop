package com.dy.bipro.controller;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.dy.bipro.dto.DYNotificacion;
import com.dy.bipro.model.DYRangoFechas;
import com.dy.bipro.model.DYUsuario;
import com.dy.bipro.service.DYCajaService;
import com.dy.bipro.service.DYDiagnosticoService;
import com.dy.bipro.service.DYSesion;
import com.dy.bipro.ui.*;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DYMainController {
    private final DYViewManager vistas;
    private final DYSesion sesion;
    private final DYEstadoApp estado;
    private final DYCajaService cajaService;
    private final DYDiagnosticoService diagnosticoService;

    @FXML private VBox menu;
    @FXML private Button btnConfig;
    @FXML private Button btnSalir;
    @FXML private Label lblIniciales;
    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private Label lblTitulo;
    @FXML private Label lblFecha;
    @FXML private ComboBox<DYRangoFechas> cmbRango;
    @FXML private Button btnCaja;
    @FXML private Button btnNotif;
    @FXML private Label lblBadge;
    @FXML private ToggleButton tglTema;
    @FXML private StackPane contenido;
    @FXML private VBox panelNotif;
    @FXML private ListView<DYNotificacion> lstNotif;

    private final ToggleGroup grupo = new ToggleGroup();
    private final Map<DYModulo, ToggleButton> botones = new EnumMap<>(DYModulo.class);
    private Object controladorActual;

    private final ChangeListener<Number> alCambiarDatos = (o, a, b) -> actualizarEstado();
    private final ChangeListener<DYModulo> alNavegar = (o, a, b) -> {
        if (b != null) navegar(b);
    };

    @FXML
    public void initialize() {
        DYUsuario u = sesion.getUsuario();
        lblIniciales.setText(u.getIniciales());
        lblUsuario.setText(u.getNombre());
        lblRol.setText(u.getRol().getEtiqueta());
        lblFecha.setText(DYFormato.capitalizar(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, yyyy", DYFormato.PERU))));

        for (DYModulo m : DYModulo.values()) {
            if (m == DYModulo.CONFIGURACION || (m.isSoloAdmin() && !sesion.esAdmin())) continue;
            ToggleButton b = new ToggleButton(m.getTitulo(), DYUi.icono(m.getIcono(), 17));
            b.getStyleClass().add("sidebar-item");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setToggleGroup(grupo);
            b.setOnAction(e -> navegar(m));
            botones.put(m, b);
            menu.getChildren().add(b);
        }
        btnConfig.setGraphic(DYUi.icono("fth-settings", 17));
        btnConfig.setVisible(sesion.esAdmin());
        btnConfig.setManaged(sesion.esAdmin());
        btnSalir.setGraphic(DYUi.icono("fth-log-out", 16));
        btnSalir.setTooltip(new Tooltip("Cerrar sesión"));
        btnNotif.setGraphic(DYUi.icono("fth-bell", 17));
        tglTema.setGraphic(DYUi.icono("fth-moon", 17));

        cmbRango.getItems().setAll(DYRangoFechas.values());
        cmbRango.setValue(estado.getRango());
        cmbRango.valueProperty().addListener((o, a, b) -> {
            estado.rangoProperty().set(b);
            if (controladorActual instanceof DYRecargable r) r.recargar();
        });

        lstNotif.setCellFactory(l -> new DYNotificacionCell(this::abrirNotificacion, sesion::leida));
        estado.cambiosProperty().addListener(alCambiarDatos);
        estado.navegacionProperty().addListener(alNavegar);

        actualizarEstado();
        navegar(DYModulo.DASHBOARD);
    }

    public void navegar(DYModulo m) {
        if (m.isSoloAdmin() && !sesion.esAdmin()) {
            DYAlertas.error("Permiso denegado", "Acción reservada para administradores.");
            return;
        }
        ToggleButton b = botones.get(m);
        if (b != null) b.setSelected(true);
        else grupo.selectToggle(null);
        btnConfig.getStyleClass().remove("activo");
        if (m == DYModulo.CONFIGURACION) btnConfig.getStyleClass().add("activo");

        lblTitulo.setText(m.getTitulo());
        DYViewManager.DYVista<Object> v = vistas.cargar(m.getVista());
        controladorActual = v.controller();
        Parent root = v.root();
        root.setOpacity(0);
        contenido.getChildren().setAll(root);
        FadeTransition fade = new FadeTransition(Duration.millis(220), root);
        fade.setToValue(1);
        fade.play();
        cerrarPanel();
    }

    private void actualizarEstado() {
        boolean abierta = cajaService.turnoAbierto().isPresent();
        btnCaja.setText(abierta ? "Caja abierta" : "Caja cerrada");
        btnCaja.setGraphic(DYUi.icono(abierta ? "fth-unlock" : "fth-lock", 15));
        btnCaja.getStyleClass().removeAll("chip-ok", "chip-alerta");
        btnCaja.getStyleClass().add(abierta ? "chip-ok" : "chip-alerta");

        List<DYNotificacion> lista = diagnosticoService.generar();
        lstNotif.getItems().setAll(lista);
        long sinLeer = lista.stream().filter(n -> !sesion.leida(n.id())).count();
        lblBadge.setText(String.valueOf(sinLeer));
        lblBadge.setVisible(sinLeer > 0);
    }

    private void abrirNotificacion(DYNotificacion n) {
        sesion.marcarLeida(n.id());
        actualizarEstado();
        navegar(DYModulo.valueOf(n.modulo()));
    }

    @FXML
    private void onNotificaciones() {
        if (panelNotif.isVisible()) {
            cerrarPanel();
            return;
        }
        panelNotif.setVisible(true);
        panelNotif.setManaged(true);
        panelNotif.setTranslateX(380);
        TranslateTransition t = new TranslateTransition(Duration.millis(240), panelNotif);
        t.setToX(0);
        t.play();
    }

    private void cerrarPanel() {
        panelNotif.setVisible(false);
        panelNotif.setManaged(false);
    }

    @FXML
    private void onMarcarLeidas() {
        lstNotif.getItems().forEach(n -> sesion.marcarLeida(n.id()));
        actualizarEstado();
    }

    @FXML
    private void onCaja() {
        navegar(DYModulo.CAJA);
    }

    @FXML
    private void onConfig() {
        navegar(DYModulo.CONFIGURACION);
    }

    @FXML
    private void onTema() {
        boolean oscuro = tglTema.isSelected();
        Application.setUserAgentStylesheet(oscuro
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        Parent root = contenido.getScene().getRoot();
        root.getStyleClass().remove("oscuro");
        if (oscuro) root.getStyleClass().add("oscuro");
        tglTema.setGraphic(DYUi.icono(oscuro ? "fth-sun" : "fth-moon", 17));
    }

    @FXML
    private void onLogout() {
        if (!DYAlertas.confirmar("¿Cerrar la sesión actual?", "Los datos registrados se conservan.")) return;
        estado.cambiosProperty().removeListener(alCambiarDatos);
        estado.navegacionProperty().removeListener(alNavegar);
        sesion.cerrar();
        vistas.mostrarLogin();
    }
}
