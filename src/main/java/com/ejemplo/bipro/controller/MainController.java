package com.ejemplo.bipro.controller;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.ejemplo.bipro.dto.Notificacion;
import com.ejemplo.bipro.model.RangoFechas;
import com.ejemplo.bipro.model.Usuario;
import com.ejemplo.bipro.service.CajaService;
import com.ejemplo.bipro.service.DiagnosticoService;
import com.ejemplo.bipro.service.Sesion;
import com.ejemplo.bipro.ui.*;
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
public class MainController {
    private final ViewManager vistas;
    private final Sesion sesion;
    private final EstadoApp estado;
    private final CajaService cajaService;
    private final DiagnosticoService diagnosticoService;

    @FXML private VBox menu;
    @FXML private Button btnConfig;
    @FXML private Button btnSalir;
    @FXML private Label lblIniciales;
    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private Label lblTitulo;
    @FXML private Label lblFecha;
    @FXML private ComboBox<RangoFechas> cmbRango;
    @FXML private Button btnCaja;
    @FXML private Button btnNotif;
    @FXML private Label lblBadge;
    @FXML private ToggleButton tglTema;
    @FXML private StackPane contenido;
    @FXML private VBox panelNotif;
    @FXML private ListView<Notificacion> lstNotif;

    private final ToggleGroup grupo = new ToggleGroup();
    private final Map<Modulo, ToggleButton> botones = new EnumMap<>(Modulo.class);
    private Object controladorActual;

    private final ChangeListener<Number> alCambiarDatos = (o, a, b) -> actualizarEstado();
    private final ChangeListener<Modulo> alNavegar = (o, a, b) -> {
        if (b != null) navegar(b);
    };

    @FXML
    public void initialize() {
        Usuario u = sesion.getUsuario();
        lblIniciales.setText(u.getIniciales());
        lblUsuario.setText(u.getNombre());
        lblRol.setText(u.getRol().getEtiqueta());
        lblFecha.setText(Formato.capitalizar(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, yyyy", Formato.PERU))));

        for (Modulo m : Modulo.values()) {
            if (m == Modulo.CONFIGURACION || (m.isSoloAdmin() && !sesion.esAdmin())) continue;
            ToggleButton b = new ToggleButton(m.getTitulo(), Ui.icono(m.getIcono(), 17));
            b.getStyleClass().add("sidebar-item");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setToggleGroup(grupo);
            b.setOnAction(e -> navegar(m));
            botones.put(m, b);
            menu.getChildren().add(b);
        }
        btnConfig.setGraphic(Ui.icono("fth-settings", 17));
        btnConfig.setVisible(sesion.esAdmin());
        btnConfig.setManaged(sesion.esAdmin());
        btnSalir.setGraphic(Ui.icono("fth-log-out", 16));
        btnSalir.setTooltip(new Tooltip("Cerrar sesión"));
        btnNotif.setGraphic(Ui.icono("fth-bell", 17));
        tglTema.setGraphic(Ui.icono("fth-moon", 17));

        cmbRango.getItems().setAll(RangoFechas.values());
        cmbRango.setValue(estado.getRango());
        cmbRango.valueProperty().addListener((o, a, b) -> {
            estado.rangoProperty().set(b);
            if (controladorActual instanceof Recargable r) r.recargar();
        });

        lstNotif.setCellFactory(l -> new NotificacionCell(this::abrirNotificacion, sesion::leida));
        estado.cambiosProperty().addListener(alCambiarDatos);
        estado.navegacionProperty().addListener(alNavegar);

        actualizarEstado();
        navegar(Modulo.DASHBOARD);
    }

    public void navegar(Modulo m) {
        if (m.isSoloAdmin() && !sesion.esAdmin()) {
            Alertas.error("Permiso denegado", "Acción reservada para administradores.");
            return;
        }
        ToggleButton b = botones.get(m);
        if (b != null) b.setSelected(true);
        else grupo.selectToggle(null);
        btnConfig.getStyleClass().remove("activo");
        if (m == Modulo.CONFIGURACION) btnConfig.getStyleClass().add("activo");

        lblTitulo.setText(m.getTitulo());
        ViewManager.Vista<Object> v = vistas.cargar(m.getVista());
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
        btnCaja.setGraphic(Ui.icono(abierta ? "fth-unlock" : "fth-lock", 15));
        btnCaja.getStyleClass().removeAll("chip-ok", "chip-alerta");
        btnCaja.getStyleClass().add(abierta ? "chip-ok" : "chip-alerta");

        List<Notificacion> lista = diagnosticoService.generar();
        lstNotif.getItems().setAll(lista);
        long sinLeer = lista.stream().filter(n -> !sesion.leida(n.id())).count();
        lblBadge.setText(String.valueOf(sinLeer));
        lblBadge.setVisible(sinLeer > 0);
    }

    private void abrirNotificacion(Notificacion n) {
        sesion.marcarLeida(n.id());
        actualizarEstado();
        navegar(Modulo.valueOf(n.modulo()));
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
        navegar(Modulo.CAJA);
    }

    @FXML
    private void onConfig() {
        navegar(Modulo.CONFIGURACION);
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
        tglTema.setGraphic(Ui.icono(oscuro ? "fth-sun" : "fth-moon", 17));
    }

    @FXML
    private void onLogout() {
        if (!Alertas.confirmar("¿Cerrar la sesión actual?", "Los datos registrados se conservan.")) return;
        estado.cambiosProperty().removeListener(alCambiarDatos);
        estado.navegacionProperty().removeListener(alNavegar);
        sesion.cerrar();
        vistas.mostrarLogin();
    }
}
