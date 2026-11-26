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
public class ClientesController {
    private final ClienteService clienteService;
    private final ClienteDialogos clienteDialogos;
    private final Sesion sesion;

    @FXML private VBox raiz;
    private final TextField txtBuscar = new TextField();
    private final ComboBox<Segmento> cmbSeg = new ComboBox<>();
    private final TableView<Cliente> tabla = new TableView<>();
    private final HBox kpis = new HBox(16);

    @FXML
    public void initialize() {
        txtBuscar.setPromptText("Buscar por nombre o documento…");
        txtBuscar.setPrefWidth(280);
        cmbSeg.setPromptText("Todos los segmentos");
        cmbSeg.setConverter(conv(Segmento::getEtiqueta, "Todos los segmentos"));
        cmbSeg.getItems().setAll(Arrays.asList((Segmento) null));
        cmbSeg.getItems().addAll(Segmento.values());
        txtBuscar.textProperty().addListener((o, a, b) -> filtrar());
        cmbSeg.valueProperty().addListener((o, a, b) -> filtrar());

        Ui.prepararTabla(tabla, "No hay clientes que coincidan.");
        Ui.colNodo(tabla, "Cliente", 240, c -> {
            Label av = new Label(c.getIniciales());
            av.getStyleClass().add("avatar");
            HBox h = new HBox(10, av, new VBox(1, Ui.texto(c.getNombre(), "dato-valor"), Ui.texto(c.getTipoDocumento() + " " + c.getDocumento(), "caption")));
            h.setAlignment(Pos.CENTER_LEFT);
            return h;
        });
        Ui.colNodo(tabla, "Segmento", 100, c -> Ui.badge(c.getSegmento().getEtiqueta(), c.getSegmento().getEstilo()));
        Ui.col(tabla, "Compras", 70, c -> String.valueOf(c.getNumeroCompras()));
        Ui.col(tabla, "Total comprado", 120, c -> Formato.soles(c.getTotalComprado()));
        Ui.col(tabla, "Última compra", 110, c -> Formato.fecha(c.getUltimaCompra()));
        Ui.col(tabla, "Teléfono", 100, c -> Formato.vacio(c.getTelefono()));
        Ui.colNodo(tabla, "", 100, c -> {
            HBox h = new HBox(4, Ui.botonIcono("fth-eye", "Ver ficha", () -> { clienteDialogos.detalle(c); filtrar(); }));
            if (sesion.esAdmin()) h.getChildren().add(Ui.botonIcono("fth-trash-2", "Eliminar", () -> eliminar(c)));
            return h;
        });
        tabla.setOnMouseClicked(e -> {
            Cliente c = tabla.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && c != null) { clienteDialogos.detalle(c); filtrar(); }
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button nuevo = Ui.boton("Nuevo cliente", "fth-user-plus", "accent");
        nuevo.setOnAction(e -> { if (clienteDialogos.formulario(null)) filtrar(); });
        HBox barra = new HBox(10, txtBuscar, cmbSeg, Ui.espaciador(), nuevo);
        barra.setAlignment(Pos.CENTER_LEFT);
        raiz.getChildren().addAll(kpis, barra, tabla);
        filtrar();
    }

    private void filtrar() {
        List<Cliente> todos = clienteService.listar();
        tabla.getItems().setAll(clienteService.buscar(txtBuscar.getText(), cmbSeg.getValue()));
        BigDecimal total = todos.stream().map(Cliente::getTotalComprado).reduce(BigDecimal.ZERO, BigDecimal::add);
        long frec = todos.stream().filter(c -> c.getSegmento() == Segmento.FRECUENTE).count();
        long ina = todos.stream().filter(c -> c.getSegmento() == Segmento.INACTIVO).count();
        kpis.getChildren().setAll(
                Ui.kpi("Clientes", String.valueOf(todos.size()), "registrados", "fth-users", true, null),
                Ui.kpi("Frecuentes", String.valueOf(frec), "20 compras o más", "fth-star", false, null),
                Ui.kpi("Inactivos", String.valueOf(ina), "más de 60 días sin comprar", "fth-user-x", false, ina == 0),
                Ui.kpi("Total comprado", Formato.solesEntero(total), "histórico", "fth-dollar-sign", false, null));
    }

    private void eliminar(Cliente c) {
        if (!Alertas.confirmar("¿Eliminar a " + c.getNombre() + "?", "Esta acción no se puede deshacer.")) return;
        try { clienteService.eliminar(c); filtrar(); } catch (Exception e) { Alertas.manejar(e); }
    }

    private static <T> StringConverter<T> conv(Function<T, String> f, String nulo) {
        return new StringConverter<>() {
            @Override public String toString(T t) { return t == null ? nulo : f.apply(t); }
            @Override public T fromString(String s) { return null; }
        };
    }
}
