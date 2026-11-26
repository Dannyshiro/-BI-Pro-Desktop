package com.dy.bipro.controller.dialogo;

import com.dy.bipro.model.*;
import com.dy.bipro.service.*;
import com.dy.bipro.ui.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DYClienteDialogos {
    private final DYClienteService clienteService;
    private final DYVentaDialogos ventaDialogos;
    private final DYEstadoApp estado;

    public boolean formulario(DYCliente c) {
        boolean nuevo = c == null;
        TextField txtNombre = new TextField(nuevo ? "" : c.getNombre());
        TextField txtDoc = new TextField(nuevo ? "" : c.getDocumento());
        txtDoc.setPromptText("DNI (8) o RUC (11)");
        TextField txtTel = new TextField(nuevo ? "" : Objects.toString(c.getTelefono(), ""));
        txtTel.setPromptText("(opcional) 9 dígitos");
        TextField txtEmail = new TextField(nuevo ? "" : Objects.toString(c.getEmail(), ""));
        txtEmail.setPromptText("(opcional)");

        GridPane g = DYUi.grilla();
        DYUi.campo(g, 0, 0, 2, "Nombre o razón social *", txtNombre);
        DYUi.campo(g, 1, 0, 1, "DNI / RUC *", txtDoc);
        DYUi.campo(g, 1, 1, 1, "Teléfono", txtTel);
        DYUi.campo(g, 2, 0, 2, "Correo electrónico", txtEmail);

        boolean ok = DYUi.formulario(nuevo ? "Nuevo cliente" : "Editar cliente", g, "Guardar", () -> {
            DYCliente x = nuevo ? new DYCliente() : c;
            x.setNombre(txtNombre.getText().trim());
            x.setDocumento(txtDoc.getText().trim());
            x.setTelefono(txtTel.getText().trim());
            x.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
            clienteService.guardar(x);
        });
        if (ok) {
            estado.notificarCambio();
            DYAlertas.info(nuevo ? "Cliente registrado" : "Cliente actualizado", txtNombre.getText().trim());
        }
        return ok;
    }

    public void detalle(DYCliente c) {
        GridPane stats = new GridPane();
        stats.setHgap(12);
        stats.setVgap(12);
        BigDecimal ticket = c.getNumeroCompras() == 0 ? BigDecimal.ZERO
                : c.getTotalComprado().divide(BigDecimal.valueOf(c.getNumeroCompras()), 2, RoundingMode.HALF_UP);
        stats.addRow(0, stat("Compras", String.valueOf(c.getNumeroCompras())), stat("Total comprado", DYFormato.soles(c.getTotalComprado())));
        stats.addRow(1, stat("Ticket promedio", DYFormato.soles(ticket)), stat("Última compra", DYFormato.fecha(c.getUltimaCompra())));

        TableView<DYVenta> historial = new TableView<>();
        DYUi.prepararTabla(historial, "Aún no hay ventas registradas en el sistema.");
        DYUi.col(historial, "N°", 100, DYVenta::getNumero);
        DYUi.col(historial, "Fecha", 140, v -> DYFormato.fechaHora(v.getFecha()));
        DYUi.col(historial, "Pago", 110, v -> v.getMetodoPago().getEtiqueta());
        DYUi.col(historial, "Total", 100, v -> DYFormato.soles(v.getTotal()));
        DYUi.colNodo(historial, "Estado", 90, v -> DYUi.badge(v.getEstado().getEtiqueta(),
                v.getEstado() == DYEstadoVenta.ANULADA ? "peligro" : "exito"));
        historial.getItems().setAll(clienteService.historial(c));
        historial.setPrefHeight(240);
        historial.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && historial.getSelectionModel().getSelectedItem() != null)
                ventaDialogos.comprobante(historial.getSelectionModel().getSelectedItem().getId());
        });

        HBox cab = new HBox(10, DYUi.badge(c.getSegmento().getEtiqueta(), c.getSegmento().getEstilo()),
                DYUi.texto(c.getTipoDocumento() + " " + c.getDocumento(), "caption"),
                DYUi.texto(DYFormato.vacio(c.getTelefono()) + " · " + DYFormato.vacio(c.getEmail()), "caption"));
        cab.setAlignment(Pos.CENTER_LEFT);
        VBox contenido = new VBox(16, cab, stats, DYUi.texto("Ventas registradas (doble clic para ver el comprobante)", "h3"), historial);
        contenido.setPrefWidth(600);

        ButtonType venta = new ButtonType("Nueva venta", ButtonBar.ButtonData.LEFT);
        ButtonType editar = new ButtonType("Editar", ButtonBar.ButtonData.OTHER);
        ButtonType r = DYUi.ventana(c.getNombre(), contenido, venta, editar, ButtonType.CLOSE);
        if (r == venta) ventaDialogos.nuevaVenta(c);
        else if (r == editar) formulario(c);
    }

    private static VBox stat(String etiqueta, String valor) {
        VBox v = new VBox(4, DYUi.texto(etiqueta, "caption"), DYUi.texto(valor, "h3"));
        v.getStyleClass().add("caja-borde");
        v.setPrefWidth(280);
        return v;
    }
}
