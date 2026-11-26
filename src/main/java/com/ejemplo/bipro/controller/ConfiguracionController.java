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
import com.ejemplo.bipro.config.DataSeeder;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ConfiguracionController {
    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;
    private final FormDialogos formDialogos;
    private final DataSeeder seeder;
    private final Sesion sesion;
    private final ViewManager vistas;

    @FXML private VBox raiz;
    private final TableView<Usuario> tabla = new TableView<>();

    @FXML
    public void initialize() {
        Empresa e = empresaService.obtener();
        TextField razon = new TextField(e.getRazonSocial());
        TextField ruc = new TextField(e.getRuc());
        TextField dir = new TextField(Objects.toString(e.getDireccion(), ""));
        TextField tel = new TextField(Objects.toString(e.getTelefono(), ""));
        TextField igv = new TextField(e.getIgv().toPlainString());
        TextField serie = new TextField(e.getSerieBoleta());
        GridPane g = Ui.grilla();
        Ui.campo(g, 0, 0, 2, "Razón social *", razon);
        Ui.campo(g, 1, 0, 1, "RUC *", ruc);
        Ui.campo(g, 1, 1, 1, "Teléfono", tel);
        Ui.campo(g, 2, 0, 2, "Dirección", dir);
        Ui.campo(g, 3, 0, 1, "IGV (%) *", igv);
        Ui.campo(g, 3, 1, 1, "Serie de boleta *", serie);
        Button guardar = Ui.boton("Guardar cambios", "fth-save", "accent");
        guardar.setOnAction(ev -> {
            try {
                e.setRazonSocial(razon.getText().trim());
                e.setRuc(ruc.getText().trim());
                e.setDireccion(dir.getText().trim());
                e.setTelefono(tel.getText().trim());
                e.setIgv(Formato.leerMonto(igv.getText()));
                e.setSerieBoleta(serie.getText().trim());
                empresaService.guardar(e);
                Alertas.info("Configuración guardada", "Los datos de la empresa se usarán en los comprobantes.");
            } catch (Exception ex) { Alertas.manejar(ex); }
        });
        VBox empresa = Ui.tarjeta("Datos de la empresa", "Aparecen en las boletas de venta.", g, guardar);
        empresa.setSpacing(14);

        Ui.prepararTabla(tabla, "Sin usuarios.");
        Ui.colNodo(tabla, "Usuario", 220, u -> {
            Label av = new Label(u.getIniciales());
            av.getStyleClass().add("avatar");
            HBox h = new HBox(10, av, new VBox(1, Ui.texto(u.getNombre(), "dato-valor"), Ui.texto("@" + u.getUsuario(), "caption")));
            h.setAlignment(Pos.CENTER_LEFT);
            return h;
        });
        Ui.colNodo(tabla, "Rol", 160, u -> {
            ComboBox<Rol> c = new ComboBox<>(FXCollections.observableArrayList(Rol.values()));
            c.setValue(u.getRol());
            c.setOnAction(ev -> {
                try { usuarioService.cambiarRol(u.getId(), c.getValue()); }
                catch (Exception ex) { Alertas.manejar(ex); c.setValue(u.getRol()); }
            });
            return c;
        });
        Ui.colNodo(tabla, "Estado", 100, u -> Ui.badge(u.isActivo() ? "Activo" : "Inactivo", u.isActivo() ? "exito" : "neutro"));
        Ui.colNodo(tabla, "", 60, u -> Ui.botonIcono("fth-power", u.isActivo() ? "Desactivar" : "Activar", () -> {
            try { usuarioService.alternarActivo(u.getId()); cargar(); } catch (Exception ex) { Alertas.manejar(ex); }
        }));
        tabla.setPrefHeight(230);
        Button nuevo = Ui.boton("Nuevo usuario", "fth-user-plus", "secundario");
        nuevo.setOnAction(ev -> { if (formDialogos.usuarioNuevo()) cargar(); });
        HBox cab = new HBox(12, Ui.texto("Usuarios y roles", "h3"), Ui.espaciador(), nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);
        VBox usuarios = new VBox(12, cab, tabla);
        usuarios.getStyleClass().add("tarjeta");

        Button reset = Ui.boton("Restablecer datos de demostración", "fth-refresh-cw", "secundario");
        reset.setOnAction(ev -> {
            if (!Alertas.confirmar("¿Restablecer todos los datos?", "Se borrará todo y se cargarán los datos de demostración. Tendrás que iniciar sesión otra vez.")) return;
            try {
                seeder.reiniciar();
                sesion.cerrar();
                vistas.mostrarLogin();
            } catch (Exception ex) { Alertas.manejar(ex); }
        });
        VBox zona = Ui.tarjeta("Zona de datos", "Útil para volver a empezar las pruebas.", reset);
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
