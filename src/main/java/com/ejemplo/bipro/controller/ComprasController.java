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
public class ComprasController {
    private final CompraService compraService;
    private final ProveedorService proveedorService;
    private final FormDialogos formDialogos;
    private final EstadoApp estado;

    @FXML private VBox raiz;
    private final TableView<OrdenCompra> tabla = new TableView<>();
    private final TableView<Proveedor> provs = new TableView<>();
    private final HBox kpis = new HBox(16);

    @FXML
    public void initialize() {
        Ui.prepararTabla(tabla, "Aún no hay órdenes de compra.");
        Ui.col(tabla, "N° orden", 130, OrdenCompra::getNumero);
        Ui.col(tabla, "Fecha", 90, o -> Formato.fecha(o.getFecha()));
        Ui.col(tabla, "Proveedor", 200, o -> o.getProveedor().getNombre());
        Ui.col(tabla, "Productos", 70, o -> String.valueOf(o.getDetalles().size()));
        Ui.col(tabla, "Total", 100, o -> Formato.soles(o.getTotal()));
        Ui.colNodo(tabla, "Estado", 100, o -> Ui.badge(o.getEstado().getEtiqueta(), o.getEstado().getEstilo()));
        Ui.colNodo(tabla, "", 120, o -> {
            HBox h = new HBox(4, Ui.botonIcono("fth-eye", "Ver líneas", () -> ver(o)));
            if (o.getEstado() == EstadoOrden.PENDIENTE) {
                h.getChildren().add(Ui.botonIcono("fth-check-circle", "Registrar recepción", () -> accion(() -> compraService.recibir(o.getId()),
                        "¿Recibir " + o.getNumero() + "?", "El stock de sus productos aumentará.")));
                h.getChildren().add(Ui.botonIcono("fth-x-circle", "Anular", () -> accion(() -> compraService.anular(o.getId()),
                        "¿Anular " + o.getNumero() + "?", "La orden quedará anulada.")));
            }
            return h;
        });
        tabla.setPrefHeight(300);

        Button gen = Ui.boton("Generar orden sugerida", "fth-clipboard", "accent");
        gen.setOnAction(e -> { formDialogos.ordenSugerida(); recargar(); });
        HBox barra = new HBox(12, Ui.texto("Órdenes de compra", "h3"), Ui.espaciador(), gen);
        barra.setAlignment(Pos.CENTER_LEFT);

        Ui.prepararTabla(provs, "Sin proveedores.");
        Ui.col(provs, "Proveedor", 220, Proveedor::getNombre);
        Ui.col(provs, "RUC", 110, Proveedor::getRuc);
        Ui.col(provs, "Contacto", 150, p -> Formato.vacio(p.getContacto()));
        Ui.col(provs, "Teléfono", 100, p -> Formato.vacio(p.getTelefono()));
        Ui.col(provs, "Entrega", 80, p -> p.getPlazoEntregaDias() + " días");
        Ui.colNodo(provs, "", 50, p -> Ui.botonIcono("fth-edit-2", "Editar", () -> { if (formDialogos.proveedor(p)) recargar(); }));
        provs.setPrefHeight(220);
        Button nuevoProv = Ui.boton("Nuevo proveedor", "fth-plus", "secundario");
        nuevoProv.setOnAction(e -> { if (formDialogos.proveedor(null)) recargar(); });
        HBox barraProv = new HBox(12, Ui.texto("Proveedores", "h3"), Ui.espaciador(), nuevoProv);
        barraProv.setAlignment(Pos.CENTER_LEFT);

        raiz.getChildren().addAll(kpis, barra, tabla, barraProv, provs);
        recargar();
    }

    private void accion(Runnable r, String titulo, String msg) {
        if (!Alertas.confirmar(titulo, msg)) return;
        try { r.run(); estado.notificarCambio(); recargar(); } catch (Exception e) { Alertas.manejar(e); }
    }

    private void ver(OrdenCompra o) {
        TableView<DetalleOrden> t = new TableView<>(FXCollections.observableArrayList(o.getDetalles()));
        Ui.prepararTabla(t, "");
        Ui.col(t, "Producto", 240, d -> d.getProducto().getNombre());
        Ui.col(t, "Cant.", 70, d -> String.valueOf(d.getCantidad()));
        Ui.col(t, "Costo unit.", 100, d -> Formato.soles(d.getCostoUnitario()));
        Ui.col(t, "Subtotal", 100, d -> Formato.soles(d.getSubtotal()));
        t.setPrefSize(560, 240);
        Ui.ventana(o.getNumero() + " · " + o.getProveedor().getNombre(), t, ButtonType.CLOSE);
    }

    private void recargar() {
        List<OrdenCompra> ordenes = compraService.listar();
        tabla.getItems().setAll(ordenes);
        provs.getItems().setAll(proveedorService.listar());
        BigDecimal pend = ordenes.stream().filter(o -> o.getEstado() == EstadoOrden.PENDIENTE)
                .map(OrdenCompra::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        kpis.getChildren().setAll(
                Ui.kpi("Órdenes pendientes", String.valueOf(compraService.pendientes()), Formato.soles(pend) + " por recibir", "fth-truck", true, null),
                Ui.kpi("Por reponer", String.valueOf(compraService.sugerencia().size()), "productos sin orden", "fth-alert-triangle", false, null),
                Ui.kpi("Proveedores", String.valueOf(provs.getItems().size()), "registrados", "fth-users", false, null));
    }
}
