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
public class DYComprasController {
    private final DYCompraService compraService;
    private final DYProveedorService proveedorService;
    private final DYFormDialogos formDialogos;
    private final DYEstadoApp estado;

    @FXML private VBox raiz;
    private final TableView<DYOrdenCompra> tabla = new TableView<>();
    private final TableView<DYProveedor> provs = new TableView<>();
    private final HBox kpis = new HBox(16);

    @FXML
    public void initialize() {
        DYUi.prepararTabla(tabla, "Aún no hay órdenes de compra.");
        DYUi.col(tabla, "N° orden", 130, DYOrdenCompra::getNumero);
        DYUi.col(tabla, "Fecha", 90, o -> DYFormato.fecha(o.getFecha()));
        DYUi.col(tabla, "Proveedor", 200, o -> o.getProveedor().getNombre());
        DYUi.col(tabla, "Productos", 70, o -> String.valueOf(o.getDetalles().size()));
        DYUi.col(tabla, "Total", 100, o -> DYFormato.soles(o.getTotal()));
        DYUi.colNodo(tabla, "Estado", 100, o -> DYUi.badge(o.getEstado().getEtiqueta(), o.getEstado().getEstilo()));
        DYUi.colNodo(tabla, "", 120, o -> {
            HBox h = new HBox(4, DYUi.botonIcono("fth-eye", "Ver líneas", () -> ver(o)));
            if (o.getEstado() == DYEstadoOrden.PENDIENTE) {
                h.getChildren().add(DYUi.botonIcono("fth-check-circle", "Registrar recepción", () -> accion(() -> compraService.recibir(o.getId()),
                        "¿Recibir " + o.getNumero() + "?", "El stock de sus productos aumentará.")));
                h.getChildren().add(DYUi.botonIcono("fth-x-circle", "Anular", () -> accion(() -> compraService.anular(o.getId()),
                        "¿Anular " + o.getNumero() + "?", "La orden quedará anulada.")));
            }
            return h;
        });
        tabla.setPrefHeight(300);

        Button gen = DYUi.boton("Generar orden sugerida", "fth-clipboard", "accent");
        gen.setOnAction(e -> { formDialogos.ordenSugerida(); recargar(); });
        HBox barra = new HBox(12, DYUi.texto("Órdenes de compra", "h3"), DYUi.espaciador(), gen);
        barra.setAlignment(Pos.CENTER_LEFT);

        DYUi.prepararTabla(provs, "Sin proveedores.");
        DYUi.col(provs, "Proveedor", 220, DYProveedor::getNombre);
        DYUi.col(provs, "RUC", 110, DYProveedor::getRuc);
        DYUi.col(provs, "Contacto", 150, p -> DYFormato.vacio(p.getContacto()));
        DYUi.col(provs, "Teléfono", 100, p -> DYFormato.vacio(p.getTelefono()));
        DYUi.col(provs, "Entrega", 80, p -> p.getPlazoEntregaDias() + " días");
        DYUi.colNodo(provs, "", 50, p -> DYUi.botonIcono("fth-edit-2", "Editar", () -> { if (formDialogos.proveedor(p)) recargar(); }));
        provs.setPrefHeight(220);
        Button nuevoProv = DYUi.boton("Nuevo proveedor", "fth-plus", "secundario");
        nuevoProv.setOnAction(e -> { if (formDialogos.proveedor(null)) recargar(); });
        HBox barraProv = new HBox(12, DYUi.texto("Proveedores", "h3"), DYUi.espaciador(), nuevoProv);
        barraProv.setAlignment(Pos.CENTER_LEFT);

        raiz.getChildren().addAll(kpis, barra, tabla, barraProv, provs);
        recargar();
    }

    private void accion(Runnable r, String titulo, String msg) {
        if (!DYAlertas.confirmar(titulo, msg)) return;
        try { r.run(); estado.notificarCambio(); recargar(); } catch (Exception e) { DYAlertas.manejar(e); }
    }

    private void ver(DYOrdenCompra o) {
        TableView<DYDetalleOrden> t = new TableView<>(FXCollections.observableArrayList(o.getDetalles()));
        DYUi.prepararTabla(t, "");
        DYUi.col(t, "Producto", 240, d -> d.getProducto().getNombre());
        DYUi.col(t, "Cant.", 70, d -> String.valueOf(d.getCantidad()));
        DYUi.col(t, "Costo unit.", 100, d -> DYFormato.soles(d.getCostoUnitario()));
        DYUi.col(t, "Subtotal", 100, d -> DYFormato.soles(d.getSubtotal()));
        t.setPrefSize(560, 240);
        DYUi.ventana(o.getNumero() + " · " + o.getProveedor().getNombre(), t, ButtonType.CLOSE);
    }

    private void recargar() {
        List<DYOrdenCompra> ordenes = compraService.listar();
        tabla.getItems().setAll(ordenes);
        provs.getItems().setAll(proveedorService.listar());
        BigDecimal pend = ordenes.stream().filter(o -> o.getEstado() == DYEstadoOrden.PENDIENTE)
                .map(DYOrdenCompra::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.getChildren().setAll(
                DYUi.kpi("Órdenes pendientes", String.valueOf(compraService.pendientes()), DYFormato.soles(pend) + " por recibir", "fth-truck", true, null),
                DYUi.kpi("Por reponer", String.valueOf(compraService.sugerencia().size()), "productos sin orden", "fth-alert-triangle", false, null),
                DYUi.kpi("Proveedores", String.valueOf(provs.getItems().size()), "registrados", "fth-users", false, null));
    }
}
