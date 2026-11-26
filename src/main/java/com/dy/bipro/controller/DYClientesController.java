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
public class DYClientesController {
    private final DYClienteService clienteService;
    private final DYClienteDialogos clienteDialogos;
    private final DYSesion sesion;

    @FXML private VBox raiz;
    private final TextField txtBuscar = new TextField();
    private final ComboBox<DYSegmento> cmbSeg = new ComboBox<>();
    private final TableView<DYCliente> tabla = new TableView<>();
    private final HBox kpis = new HBox(16);

    @FXML
    public void initialize() {
        txtBuscar.setPromptText("Buscar por nombre o documento…");
        txtBuscar.setPrefWidth(280);
        cmbSeg.setPromptText("Todos los segmentos");
        cmbSeg.setConverter(conv(DYSegmento::getEtiqueta, "Todos los segmentos"));
        cmbSeg.getItems().setAll(Arrays.asList((DYSegmento) null));
        cmbSeg.getItems().addAll(DYSegmento.values());
        txtBuscar.textProperty().addListener((o, a, b) -> filtrar());
        cmbSeg.valueProperty().addListener((o, a, b) -> filtrar());

        DYUi.prepararTabla(tabla, "No hay clientes que coincidan.");
        DYUi.colNodo(tabla, "Cliente", 240, c -> {
            Label av = new Label(c.getIniciales());
            av.getStyleClass().add("avatar");
            HBox h = new HBox(10, av, new VBox(1, DYUi.texto(c.getNombre(), "dato-valor"), DYUi.texto(c.getTipoDocumento() + " " + c.getDocumento(), "caption")));
            h.setAlignment(Pos.CENTER_LEFT);
            return h;
        });
        DYUi.colNodo(tabla, "Segmento", 100, c -> DYUi.badge(c.getSegmento().getEtiqueta(), c.getSegmento().getEstilo()));
        DYUi.col(tabla, "Compras", 70, c -> String.valueOf(c.getNumeroCompras()));
        DYUi.col(tabla, "Total comprado", 120, c -> DYFormato.soles(c.getTotalComprado()));
        DYUi.col(tabla, "Última compra", 110, c -> DYFormato.fecha(c.getUltimaCompra()));
        DYUi.col(tabla, "Teléfono", 100, c -> DYFormato.vacio(c.getTelefono()));
        DYUi.colNodo(tabla, "", 100, c -> {
            HBox h = new HBox(4, DYUi.botonIcono("fth-eye", "Ver ficha", () -> { clienteDialogos.detalle(c); filtrar(); }));
            if (sesion.esAdmin()) h.getChildren().add(DYUi.botonIcono("fth-trash-2", "Eliminar", () -> eliminar(c)));
            return h;
        });
        tabla.setOnMouseClicked(e -> {
            DYCliente c = tabla.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && c != null) { clienteDialogos.detalle(c); filtrar(); }
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button nuevo = DYUi.boton("Nuevo cliente", "fth-user-plus", "accent");
        nuevo.setOnAction(e -> { if (clienteDialogos.formulario(null)) filtrar(); });
        HBox barra = new HBox(10, txtBuscar, cmbSeg, DYUi.espaciador(), nuevo);
        barra.setAlignment(Pos.CENTER_LEFT);
        raiz.getChildren().addAll(kpis, barra, tabla);
        filtrar();
    }

    private void filtrar() {
        List<DYCliente> todos = clienteService.listar();
        tabla.getItems().setAll(clienteService.buscar(txtBuscar.getText(), cmbSeg.getValue()));
        BigDecimal total = todos.stream().map(DYCliente::getTotalComprado).reduce(BigDecimal.ZERO, BigDecimal::add);
        long frec = todos.stream().filter(c -> c.getSegmento() == DYSegmento.FRECUENTE).count();
        long ina = todos.stream().filter(c -> c.getSegmento() == DYSegmento.INACTIVO).count();
        kpis.getChildren().setAll(
                DYUi.kpi("Clientes", String.valueOf(todos.size()), "registrados", "fth-users", true, null),
                DYUi.kpi("Frecuentes", String.valueOf(frec), "20 compras o más", "fth-star", false, null),
                DYUi.kpi("Inactivos", String.valueOf(ina), "más de 60 días sin comprar", "fth-user-x", false, ina == 0),
                DYUi.kpi("Total comprado", DYFormato.solesEntero(total), "histórico", "fth-dollar-sign", false, null));
    }

    private void eliminar(DYCliente c) {
        if (!DYAlertas.confirmar("¿Eliminar a " + c.getNombre() + "?", "Esta acción no se puede deshacer.")) return;
        try { clienteService.eliminar(c); filtrar(); } catch (Exception e) { DYAlertas.manejar(e); }
    }

    private static <T> StringConverter<T> conv(Function<T, String> f, String nulo) {
        return new StringConverter<>() {
            @Override public String toString(T t) { return t == null ? nulo : f.apply(t); }
            @Override public T fromString(String s) { return null; }
        };
    }
}
