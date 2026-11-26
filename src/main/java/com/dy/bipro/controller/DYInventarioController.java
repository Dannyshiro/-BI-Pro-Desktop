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
public class DYInventarioController {
    private final DYProductoService productoService;
    private final DYCategoriaService categoriaService;
    private final DYProveedorService proveedorService;
    private final DYProductoDialogos productoDialogos;
    private final DYFormDialogos formDialogos;
    private final DYSesion sesion;

    @FXML private VBox raiz;
    private final TextField txtBuscar = new TextField();
    private final ComboBox<DYCategoria> cmbCat = new ComboBox<>();
    private final ComboBox<DYEstadoStock> cmbEst = new ComboBox<>();
    private final TableView<DYProducto> tabla = new TableView<>();
    private final Label lblConteo = DYUi.texto("", "caption");
    private final HBox kpis = new HBox(16);

    @FXML
    public void initialize() {
        txtBuscar.setPromptText("Buscar por nombre o SKU…");
        txtBuscar.setPrefWidth(280);
        cmbCat.setPromptText("Todas las categorías");
        cmbCat.setConverter(conv(DYCategoria::getNombre, "Todas las categorías"));
        cmbEst.setPromptText("Todos los estados");
        cmbEst.setConverter(conv(DYEstadoStock::getEtiqueta, "Todos los estados"));
        cmbEst.getItems().setAll(Arrays.asList((DYEstadoStock) null));
        cmbEst.getItems().addAll(DYEstadoStock.values());
        cargarCategorias();
        txtBuscar.textProperty().addListener((o, a, b) -> filtrar());
        cmbCat.valueProperty().addListener((o, a, b) -> filtrar());
        cmbEst.valueProperty().addListener((o, a, b) -> filtrar());

        DYUi.prepararTabla(tabla, "No hay productos que coincidan con el filtro.");
        DYUi.col(tabla, "Producto", 230, DYProducto::getNombre);
        DYUi.col(tabla, "SKU", 90, DYProducto::getSku);
        DYUi.col(tabla, "Categoría", 110, p -> p.getCategoria().getNombre());
        DYUi.col(tabla, "Stock", 70, p -> String.valueOf(p.getStockActual()));
        DYUi.col(tabla, "Mín / Máx", 90, p -> p.getStockMinimo() + " / " + p.getStockMaximo());
        DYUi.colNodo(tabla, "Estado", 100, p -> DYUi.badge(p.getEstado().getEtiqueta(), p.getEstado().getEstilo()));
        DYUi.col(tabla, "P. venta", 90, p -> DYFormato.soles(p.getPrecioVenta()));
        DYUi.colNodo(tabla, "", 130, p -> {
            HBox h = new HBox(4, DYUi.botonIcono("fth-eye", "Ver detalle", () -> { productoDialogos.detalle(p); filtrar(); }));
            if (sesion.esAdmin()) {
                h.getChildren().add(DYUi.botonIcono("fth-edit-2", "Editar", () -> { if (productoDialogos.formulario(p)) filtrar(); }));
                h.getChildren().add(DYUi.botonIcono("fth-trash-2", "Eliminar", () -> eliminar(p)));
            }
            return h;
        });
        tabla.setOnMouseClicked(e -> {
            DYProducto p = tabla.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && p != null) { productoDialogos.detalle(p); filtrar(); }
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);

        HBox barra = new HBox(10, txtBuscar, cmbCat, cmbEst, DYUi.espaciador());
        barra.setAlignment(Pos.CENTER_LEFT);
        if (sesion.esAdmin()) {
            Button cats = DYUi.boton("Categorías", "fth-tag", "secundario");
            cats.setOnAction(e -> categorias());
            Button orden = DYUi.boton("Orden sugerida", "fth-clipboard", "secundario");
            orden.setOnAction(e -> { formDialogos.ordenSugerida(); filtrar(); });
            Button nuevo = DYUi.boton("Nuevo producto", "fth-plus", "accent");
            nuevo.setOnAction(e -> { if (productoDialogos.formulario(null)) filtrar(); });
            barra.getChildren().addAll(cats, orden, nuevo);
        }
        raiz.getChildren().addAll(kpis, barra, tabla, lblConteo);
        filtrar();
    }

    private void cargarCategorias() {
        DYCategoria actual = cmbCat.getValue();
        cmbCat.getItems().setAll(Arrays.asList((DYCategoria) null));
        cmbCat.getItems().addAll(categoriaService.listar());
        cmbCat.setValue(actual == null ? null : cmbCat.getItems().stream()
                .filter(c -> c != null && c.getId().equals(actual.getId())).findFirst().orElse(null));
    }

    private void filtrar() {
        List<DYProducto> todos = productoService.listarTodos();
        List<DYProducto> lista = productoService.buscar(txtBuscar.getText(), cmbCat.getValue(), cmbEst.getValue());
        tabla.getItems().setAll(lista);
        lblConteo.setText("Mostrando " + lista.size() + " de " + todos.size() + " productos");
        BigDecimal valor = todos.stream().map(p -> p.getPrecioCompra().multiply(BigDecimal.valueOf(p.getStockActual())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long agot = todos.stream().filter(p -> p.getEstado() == DYEstadoStock.AGOTADO).count();
        long crit = todos.stream().filter(p -> p.getEstado() == DYEstadoStock.CRITICO).count();
        long exc = todos.stream().filter(p -> p.getEstado() == DYEstadoStock.EXCESO).count();
        kpis.getChildren().setAll(
                DYUi.kpi("Productos", String.valueOf(todos.size()), "en catálogo", "fth-package", true, null),
                DYUi.kpi("Valor del inventario", DYFormato.solesEntero(valor), "a precio de compra", "fth-dollar-sign", false, null),
                DYUi.kpi("Agotados", String.valueOf(agot), "sin stock", "fth-alert-octagon", false, agot == 0),
                DYUi.kpi("Críticos", String.valueOf(crit), "bajo el mínimo", "fth-alert-triangle", false, crit == 0),
                DYUi.kpi("En exceso", String.valueOf(exc), "sobre el máximo", "fth-trending-up", false, null));
    }

    private void eliminar(DYProducto p) {
        if (!DYAlertas.confirmar("¿Eliminar \"" + p.getNombre() + "\"?", "Esta acción no se puede deshacer.")) return;
        try { productoService.eliminar(p); filtrar(); } catch (Exception e) { DYAlertas.manejar(e); }
    }

    private void categorias() {
        TableView<DYCategoria> t = new TableView<>();
        DYUi.prepararTabla(t, "Sin categorías.");
        Runnable carga = () -> t.getItems().setAll(categoriaService.listar());
        DYUi.col(t, "Categoría", 160, DYCategoria::getNombre);
        DYUi.col(t, "Proveedor", 220, c -> c.getProveedor() == null ? "Sin proveedor" : c.getProveedor().getNombre());
        DYUi.col(t, "Productos", 80, c -> String.valueOf(categoriaService.contarProductos(c)));
        DYUi.colNodo(t, "", 80, c -> new HBox(4,
                DYUi.botonIcono("fth-truck", "Cambiar proveedor", () -> { elegirProveedor(c); carga.run(); }),
                DYUi.botonIcono("fth-trash-2", "Eliminar", () -> {
                    try { categoriaService.eliminar(c); carga.run(); } catch (Exception e) { DYAlertas.manejar(e); }
                })));
        carga.run();
        t.setPrefSize(560, 280);
        Button nueva = DYUi.boton("Nueva categoría", "fth-plus", "accent");
        nueva.setOnAction(e -> {
            TextField n = new TextField();
            ComboBox<DYProveedor> p = new ComboBox<>(FXCollections.observableArrayList(proveedorService.listar()));
            GridPane g = DYUi.grilla();
            DYUi.campo(g, 0, 0, 1, "Nombre *", n);
            DYUi.campo(g, 0, 1, 1, "Proveedor", p);
            DYUi.formulario("Nueva categoría", g, "Crear", () -> categoriaService.crear(n.getText(), p.getValue()));
            carga.run();
        });
        DYUi.ventana("Categorías", new VBox(12, nueva, t), ButtonType.CLOSE);
        cargarCategorias();
    }

    private void elegirProveedor(DYCategoria c) {
        ComboBox<DYProveedor> p = new ComboBox<>(FXCollections.observableArrayList(proveedorService.listar()));
        GridPane g = DYUi.grilla();
        DYUi.campo(g, 0, 0, 2, "Proveedor de " + c.getNombre(), p);
        DYUi.formulario("Asignar proveedor", g, "Guardar", () -> categoriaService.asignarProveedor(c, p.getValue()));
    }

    private static <T> StringConverter<T> conv(Function<T, String> f, String nulo) {
        return new StringConverter<>() {
            @Override public String toString(T t) { return t == null ? nulo : f.apply(t); }
            @Override public T fromString(String s) { return null; }
        };
    }
}
