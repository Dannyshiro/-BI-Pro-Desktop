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
public class InventarioController {
    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final ProveedorService proveedorService;
    private final ProductoDialogos productoDialogos;
    private final FormDialogos formDialogos;
    private final Sesion sesion;

    @FXML private VBox raiz;
    private final TextField txtBuscar = new TextField();
    private final ComboBox<Categoria> cmbCat = new ComboBox<>();
    private final ComboBox<EstadoStock> cmbEst = new ComboBox<>();
    private final TableView<Producto> tabla = new TableView<>();
    private final Label lblConteo = Ui.texto("", "caption");
    private final HBox kpis = new HBox(16);

    @FXML
    public void initialize() {
        txtBuscar.setPromptText("Buscar por nombre o SKU…");
        txtBuscar.setPrefWidth(280);
        cmbCat.setPromptText("Todas las categorías");
        cmbCat.setConverter(conv(Categoria::getNombre, "Todas las categorías"));
        cmbEst.setPromptText("Todos los estados");
        cmbEst.setConverter(conv(EstadoStock::getEtiqueta, "Todos los estados"));
        cmbEst.getItems().setAll(Arrays.asList((EstadoStock) null));
        cmbEst.getItems().addAll(EstadoStock.values());
        cargarCategorias();
        txtBuscar.textProperty().addListener((o, a, b) -> filtrar());
        cmbCat.valueProperty().addListener((o, a, b) -> filtrar());
        cmbEst.valueProperty().addListener((o, a, b) -> filtrar());

        Ui.prepararTabla(tabla, "No hay productos que coincidan con el filtro.");
        Ui.col(tabla, "Producto", 230, Producto::getNombre);
        Ui.col(tabla, "SKU", 90, Producto::getSku);
        Ui.col(tabla, "Categoría", 110, p -> p.getCategoria().getNombre());
        Ui.col(tabla, "Stock", 70, p -> String.valueOf(p.getStockActual()));
        Ui.col(tabla, "Mín / Máx", 90, p -> p.getStockMinimo() + " / " + p.getStockMaximo());
        Ui.colNodo(tabla, "Estado", 100, p -> Ui.badge(p.getEstado().getEtiqueta(), p.getEstado().getEstilo()));
        Ui.col(tabla, "P. venta", 90, p -> Formato.soles(p.getPrecioVenta()));
        Ui.colNodo(tabla, "", 130, p -> {
            HBox h = new HBox(4, Ui.botonIcono("fth-eye", "Ver detalle", () -> { productoDialogos.detalle(p); filtrar(); }));
            if (sesion.esAdmin()) {
                h.getChildren().add(Ui.botonIcono("fth-edit-2", "Editar", () -> { if (productoDialogos.formulario(p)) filtrar(); }));
                h.getChildren().add(Ui.botonIcono("fth-trash-2", "Eliminar", () -> eliminar(p)));
            }
            return h;
        });
        tabla.setOnMouseClicked(e -> {
            Producto p = tabla.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && p != null) { productoDialogos.detalle(p); filtrar(); }
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);

        HBox barra = new HBox(10, txtBuscar, cmbCat, cmbEst, Ui.espaciador());
        barra.setAlignment(Pos.CENTER_LEFT);
        if (sesion.esAdmin()) {
            Button cats = Ui.boton("Categorías", "fth-tag", "secundario");
            cats.setOnAction(e -> categorias());
            Button orden = Ui.boton("Orden sugerida", "fth-clipboard", "secundario");
            orden.setOnAction(e -> { formDialogos.ordenSugerida(); filtrar(); });
            Button nuevo = Ui.boton("Nuevo producto", "fth-plus", "accent");
            nuevo.setOnAction(e -> { if (productoDialogos.formulario(null)) filtrar(); });
            barra.getChildren().addAll(cats, orden, nuevo);
        }
        raiz.getChildren().addAll(kpis, barra, tabla, lblConteo);
        filtrar();
    }

    private void cargarCategorias() {
        Categoria actual = cmbCat.getValue();
        cmbCat.getItems().setAll(Arrays.asList((Categoria) null));
        cmbCat.getItems().addAll(categoriaService.listar());
        cmbCat.setValue(actual == null ? null : cmbCat.getItems().stream()
                .filter(c -> c != null && c.getId().equals(actual.getId())).findFirst().orElse(null));
    }

    private void filtrar() {
        List<Producto> todos = productoService.listarTodos();
        List<Producto> lista = productoService.buscar(txtBuscar.getText(), cmbCat.getValue(), cmbEst.getValue());
        tabla.getItems().setAll(lista);
        lblConteo.setText("Mostrando " + lista.size() + " de " + todos.size() + " productos");
        BigDecimal valor = todos.stream().map(p -> p.getPrecioCompra().multiply(BigDecimal.valueOf(p.getStockActual())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long agot = todos.stream().filter(p -> p.getEstado() == EstadoStock.AGOTADO).count();
        long crit = todos.stream().filter(p -> p.getEstado() == EstadoStock.CRITICO).count();
        long exc = todos.stream().filter(p -> p.getEstado() == EstadoStock.EXCESO).count();
        kpis.getChildren().setAll(
                Ui.kpi("Productos", String.valueOf(todos.size()), "en catálogo", "fth-package", true, null),
                Ui.kpi("Valor del inventario", Formato.solesEntero(valor), "a precio de compra", "fth-dollar-sign", false, null),
                Ui.kpi("Agotados", String.valueOf(agot), "sin stock", "fth-alert-octagon", false, agot == 0),
                Ui.kpi("Críticos", String.valueOf(crit), "bajo el mínimo", "fth-alert-triangle", false, crit == 0),
                Ui.kpi("En exceso", String.valueOf(exc), "sobre el máximo", "fth-trending-up", false, null));
    }

    private void eliminar(Producto p) {
        if (!Alertas.confirmar("¿Eliminar \"" + p.getNombre() + "\"?", "Esta acción no se puede deshacer.")) return;
        try { productoService.eliminar(p); filtrar(); } catch (Exception e) { Alertas.manejar(e); }
    }

    private void categorias() {
        TableView<Categoria> t = new TableView<>();
        Ui.prepararTabla(t, "Sin categorías.");
        Runnable carga = () -> t.getItems().setAll(categoriaService.listar());
        Ui.col(t, "Categoría", 160, Categoria::getNombre);
        Ui.col(t, "Proveedor", 220, c -> c.getProveedor() == null ? "Sin proveedor" : c.getProveedor().getNombre());
        Ui.col(t, "Productos", 80, c -> String.valueOf(categoriaService.contarProductos(c)));
        Ui.colNodo(t, "", 80, c -> new HBox(4,
                Ui.botonIcono("fth-truck", "Cambiar proveedor", () -> { elegirProveedor(c); carga.run(); }),
                Ui.botonIcono("fth-trash-2", "Eliminar", () -> {
                    try { categoriaService.eliminar(c); carga.run(); } catch (Exception e) { Alertas.manejar(e); }
                })));
        carga.run();
        t.setPrefSize(560, 280);
        Button nueva = Ui.boton("Nueva categoría", "fth-plus", "accent");
        nueva.setOnAction(e -> {
            TextField n = new TextField();
            ComboBox<Proveedor> p = new ComboBox<>(FXCollections.observableArrayList(proveedorService.listar()));
            GridPane g = Ui.grilla();
            Ui.campo(g, 0, 0, 1, "Nombre *", n);
            Ui.campo(g, 0, 1, 1, "Proveedor", p);
            Ui.formulario("Nueva categoría", g, "Crear", () -> categoriaService.crear(n.getText(), p.getValue()));
            carga.run();
        });
        Ui.ventana("Categorías", new VBox(12, nueva, t), ButtonType.CLOSE);
        cargarCategorias();
    }

    private void elegirProveedor(Categoria c) {
        ComboBox<Proveedor> p = new ComboBox<>(FXCollections.observableArrayList(proveedorService.listar()));
        GridPane g = Ui.grilla();
        Ui.campo(g, 0, 0, 2, "Proveedor de " + c.getNombre(), p);
        Ui.formulario("Asignar proveedor", g, "Guardar", () -> categoriaService.asignarProveedor(c, p.getValue()));
    }

    private static <T> StringConverter<T> conv(Function<T, String> f, String nulo) {
        return new StringConverter<>() {
            @Override public String toString(T t) { return t == null ? nulo : f.apply(t); }
            @Override public T fromString(String s) { return null; }
        };
    }
}
