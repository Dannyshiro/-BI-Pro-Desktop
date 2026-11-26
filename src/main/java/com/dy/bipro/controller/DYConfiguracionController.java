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
import com.dy.bipro.config.DYDataSeeder;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DYConfiguracionController {
    private final DYEmpresaService empresaService;
    private final DYUsuarioService usuarioService;
    private final DYFormDialogos formDialogos;
    private final DYDataSeeder seeder;
    private final DYSesion sesion;
    private final DYViewManager vistas;

    @FXML private VBox raiz;
    private final TableView<DYUsuario> tabla = new TableView<>();

    @FXML
    public void initialize() {
        DYEmpresa e = empresaService.obtener();
        TextField razon = new TextField(e.getRazonSocial());
        TextField ruc = new TextField(e.getRuc());
        TextField dir = new TextField(Objects.toString(e.getDireccion(), ""));
        TextField tel = new TextField(Objects.toString(e.getTelefono(), ""));
        TextField igv = new TextField(e.getIgv().toPlainString());
        TextField serie = new TextField(e.getSerieBoleta());
        GridPane g = DYUi.grilla();
        DYUi.campo(g, 0, 0, 2, "Razón social *", razon);
        DYUi.campo(g, 1, 0, 1, "RUC *", ruc);
        DYUi.campo(g, 1, 1, 1, "Teléfono", tel);
        DYUi.campo(g, 2, 0, 2, "Dirección", dir);
        DYUi.campo(g, 3, 0, 1, "IGV (%) *", igv);
        DYUi.campo(g, 3, 1, 1, "Serie de boleta *", serie);
        Button guardar = DYUi.boton("Guardar cambios", "fth-save", "accent");
        guardar.setOnAction(ev -> {
            try {
                e.setRazonSocial(razon.getText().trim());
                e.setRuc(ruc.getText().trim());
                e.setDireccion(dir.getText().trim());
                e.setTelefono(tel.getText().trim());
                e.setIgv(DYFormato.leerMonto(igv.getText()));
                e.setSerieBoleta(serie.getText().trim());
                empresaService.guardar(e);
                DYAlertas.info("Configuración guardada", "Los datos de la empresa se usarán en los comprobantes.");
            } catch (Exception ex) { DYAlertas.manejar(ex); }
        });
        VBox empresa = DYUi.tarjeta("Datos de la empresa", "Aparecen en las boletas de venta.", g, guardar);
        empresa.setSpacing(14);

        DYUi.prepararTabla(tabla, "Sin usuarios.");
        DYUi.colNodo(tabla, "Usuario", 220, u -> {
            Label av = new Label(u.getIniciales());
            av.getStyleClass().add("avatar");
            HBox h = new HBox(10, av, new VBox(1, DYUi.texto(u.getNombre(), "dato-valor"), DYUi.texto("@" + u.getUsuario(), "caption")));
            h.setAlignment(Pos.CENTER_LEFT);
            return h;
        });
        DYUi.colNodo(tabla, "Rol", 160, u -> {
            ComboBox<DYRol> c = new ComboBox<>(FXCollections.observableArrayList(DYRol.values()));
            c.setValue(u.getRol());
            c.setOnAction(ev -> {
                try { usuarioService.cambiarRol(u.getId(), c.getValue()); }
                catch (Exception ex) { DYAlertas.manejar(ex); c.setValue(u.getRol()); }
            });
            return c;
        });
        DYUi.colNodo(tabla, "Estado", 100, u -> DYUi.badge(u.isActivo() ? "Activo" : "Inactivo", u.isActivo() ? "exito" : "neutro"));
        DYUi.colNodo(tabla, "", 60, u -> DYUi.botonIcono("fth-power", u.isActivo() ? "Desactivar" : "Activar", () -> {
            try { usuarioService.alternarActivo(u.getId()); cargar(); } catch (Exception ex) { DYAlertas.manejar(ex); }
        }));
        tabla.setPrefHeight(230);
        Button nuevo = DYUi.boton("Nuevo usuario", "fth-user-plus", "secundario");
        nuevo.setOnAction(ev -> { if (formDialogos.usuarioNuevo()) cargar(); });
        HBox cab = new HBox(12, DYUi.texto("Usuarios y roles", "h3"), DYUi.espaciador(), nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);
        VBox usuarios = new VBox(12, cab, tabla);
        usuarios.getStyleClass().add("tarjeta");

        Button reset = DYUi.boton("Restablecer datos de demostración", "fth-refresh-cw", "secundario");
        reset.setOnAction(ev -> {
            if (!DYAlertas.confirmar("¿Restablecer todos los datos?", "Se borrará todo y se cargarán los datos de demostración. Tendrás que iniciar sesión otra vez.")) return;
            try {
                seeder.reiniciar();
                sesion.cerrar();
                vistas.mostrarLogin();
            } catch (Exception ex) { DYAlertas.manejar(ex); }
        });
        VBox zona = DYUi.tarjeta("Zona de datos", "Útil para volver a empezar las pruebas.", reset);
        zona.setSpacing(12);

        ScrollPane sp = new ScrollPane(new VBox(16, empresa, usuarios, zona));
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-contenido");
        VBox.setVgrow(sp, Priority.ALWAYS);
        raiz.getChildren().add(sp);
        cargar();
    }

    private void cargar() {
        tabla.getItems().setAll(usuarioService.listar());
    }
}
