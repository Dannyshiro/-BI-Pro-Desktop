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
public class DYProductoDialogos {
    private final DYProductoService productoService;
    private final DYCategoriaService categoriaService;
    private final DYCompraService compraService;
    private final DYSesion sesion;
    private final DYEstadoApp estado;

    public boolean formulario(DYProducto p) {
        boolean nuevo = p == null;
        TextField txtNombre = new TextField(nuevo ? "" : p.getNombre());
        TextField txtSku = new TextField(nuevo ? "" : p.getSku());
        ComboBox<DYCategoria> cmbCategoria = new ComboBox<>(FXCollections.observableArrayList(categoriaService.listar()));
        if (!nuevo) cmbCategoria.getItems().stream().filter(c -> c.getId().equals(p.getCategoria().getId()))
                .findFirst().ifPresent(cmbCategoria::setValue);
        TextArea txtDescripcion = new TextArea(nuevo ? "" : Objects.toString(p.getDescripcion(), ""));
        txtDescripcion.setPrefRowCount(2);
        txtDescripcion.setWrapText(true);
        TextField txtCompra = new TextField(nuevo ? "" : p.getPrecioCompra().toPlainString());
        TextField txtVenta = new TextField(nuevo ? "" : p.getPrecioVenta().toPlainString());
        TextField txtStock = new TextField(nuevo ? "0" : String.valueOf(p.getStockActual()));
        TextField txtMin = new TextField(nuevo ? "" : String.valueOf(p.getStockMinimo()));
        TextField txtMax = new TextField(nuevo ? "" : String.valueOf(p.getStockMaximo()));

        GridPane g = DYUi.grilla();
        DYUi.campo(g, 0, 0, 2, "Nombre *", txtNombre);
        DYUi.campo(g, 1, 0, 1, "SKU *", txtSku);
        DYUi.campo(g, 1, 1, 1, "Categoría *", cmbCategoria);
        DYUi.campo(g, 2, 0, 2, "Descripción", txtDescripcion);
        DYUi.campo(g, 3, 0, 1, "Precio de compra (S/) *", txtCompra);
        DYUi.campo(g, 3, 1, 1, "Precio de venta (S/) *", txtVenta);
        GridPane stock = new GridPane();
        stock.setHgap(12);
        stock.getStyleClass().add("grupo-stock");
        DYUi.campo(stock, 0, 0, 1, "Stock actual *", txtStock);
        DYUi.campo(stock, 0, 1, 1, "Stock mínimo *", txtMin);
        DYUi.campo(stock, 0, 2, 1, "Stock máximo *", txtMax);
        g.add(stock, 0, 4, 2, 1);

        boolean ok = DYUi.formulario(nuevo ? "Nuevo producto" : "Editar producto", g, "Guardar", () -> {
            DYProducto x = nuevo ? new DYProducto() : p;
            x.setNombre(txtNombre.getText().trim());
            x.setSku(txtSku.getText().trim());
            x.setCategoria(cmbCategoria.getValue());
            x.setDescripcion(txtDescripcion.getText().trim());
            x.setPrecioCompra(DYFormato.leerMonto(txtCompra.getText()));
            x.setPrecioVenta(DYFormato.leerMonto(txtVenta.getText()));
            x.setStockActual(DYFormato.leerEntero(txtStock.getText()));
            x.setStockMinimo(DYFormato.leerEntero(txtMin.getText()));
            x.setStockMaximo(DYFormato.leerEntero(txtMax.getText()));
            productoService.guardar(x);
        });
        if (ok) {
            estado.notificarCambio();
            DYAlertas.info(nuevo ? "Producto registrado" : "Producto actualizado", txtNombre.getText().trim());
        }
        return ok;
    }

    public void detalle(DYProducto p) {
        Label lblStock = DYUi.texto("", "kpi-valor");
        ProgressBar barra = new ProgressBar();
        barra.setMaxWidth(Double.MAX_VALUE);
        Label lblEstado = new Label();
        TableView<DYMovimientoStock> movimientos = new TableView<>();
        DYUi.prepararTabla(movimientos, "Sin movimientos registrados.");
        DYUi.col(movimientos, "Movimiento", 150, m -> m.getTipo().getEtiqueta());
        DYUi.col(movimientos, "Referencia", 140, m -> DYFormato.vacio(m.getReferencia()));
        DYUi.col(movimientos, "Fecha", 130, m -> DYFormato.fechaHora(m.getFecha()));
        DYUi.col(movimientos, "Cant.", 60, m -> (m.getCantidad() > 0 ? "+" : "") + m.getCantidad());
        DYUi.col(movimientos, "Saldo", 60, m -> String.valueOf(m.getSaldo()));
        movimientos.setPrefHeight(220);

        Runnable refrescar = () -> {
            DYProducto x = productoService.listarTodos().stream().filter(y -> y.getId().equals(p.getId())).findFirst().orElse(p);
            lblStock.setText(x.getStockActual() + " u.");
            barra.setProgress(Math.min(1.0, x.getStockActual() / (double) x.getStockMaximo()));
            lblEstado.setGraphic(DYUi.badge(x.getEstado().getEtiqueta(), x.getEstado().getEstilo()));
            movimientos.getItems().setAll(productoService.movimientos(x));
        };
        refrescar.run();

        BigDecimal margen = p.getPrecioVenta().signum() == 0 ? BigDecimal.ZERO
                : p.getPrecioVenta().subtract(p.getPrecioCompra()).multiply(BigDecimal.valueOf(100))
                .divide(p.getPrecioVenta(), 1, RoundingMode.HALF_UP);
        GridPane stats = new GridPane();
        stats.setHgap(12);
        stats.setVgap(12);
        stats.addRow(0, stat("Precio de compra", DYFormato.soles(p.getPrecioCompra())), stat("Precio de venta", DYFormato.soles(p.getPrecioVenta())));
        stats.addRow(1, stat("Margen unitario", margen + "%"),
                stat("Valor del inventario", DYFormato.soles(p.getPrecioCompra().multiply(BigDecimal.valueOf(p.getStockActual())))));

        DYProveedor prov = p.getCategoria().getProveedor();
        boolean enOrden = compraService.productosConOrdenPendiente().contains(p.getId());
        HBox proveedor = new HBox(12, DYUi.icono("fth-truck", 18), new VBox(2,
                DYUi.texto(prov == null ? "Sin proveedor" : prov.getNombre(), "dato-valor"),
                DYUi.texto(prov == null ? "Asigna un proveedor a la categoría" : prov.getContacto() + " · entrega en "
                        + prov.getPlazoEntregaDias() + " días", "caption")), DYUi.espaciador());
        if (enOrden) proveedor.getChildren().add(DYUi.badge("OC pendiente", "advertencia"));
        proveedor.setAlignment(Pos.CENTER_LEFT);
        proveedor.getStyleClass().add("caja-borde");

        VBox contenido = new VBox(16,
                new HBox(12, DYUi.texto(p.getSku() + " · " + p.getCategoria().getNombre(), "caption"), lblEstado),
                new VBox(6, DYUi.texto("Stock actual  (mín. " + p.getStockMinimo() + " · máx. " + p.getStockMaximo() + ")", "caption"),
                        lblStock, barra),
                stats, proveedor);

        if (sesion.esAdmin()) {
            TextField txtCant = new TextField();
            txtCant.setPromptText("± cantidad");
            txtCant.setPrefWidth(110);
            ComboBox<String> cmbMotivo = new ComboBox<>(FXCollections.observableArrayList(
                    "Conteo físico", "Merma o vencimiento", "Devolución de cliente", "Consumo interno"));
            cmbMotivo.setValue("Conteo físico");
            Button aplicar = DYUi.boton("Aplicar", "fth-sliders", "accent");
            aplicar.setOnAction(e -> {
                try {
                    Integer n = DYFormato.leerEntero(txtCant.getText());
                    if (n == null) throw new com.dy.bipro.exception.DYNegocioException("Ingrese un número entero (negativo para salidas).");
                    productoService.ajustar(p.getId(), n, cmbMotivo.getValue());
                    txtCant.clear();
                    refrescar.run();
                    estado.notificarCambio();
                } catch (Exception ex) {
                    DYAlertas.manejar(ex);
                }
            });
            HBox ajuste = new HBox(10, txtCant, cmbMotivo, aplicar);
            ajuste.setAlignment(Pos.CENTER_LEFT);
            contenido.getChildren().add(new VBox(8, DYUi.texto("Ajuste de inventario", "h3"),
                    DYUi.texto("Usa valores negativos para salidas.", "caption"), ajuste));
        }
        contenido.getChildren().add(new VBox(8, DYUi.texto("Movimientos de stock", "h3"), movimientos));
        contenido.setPrefWidth(640);

        ButtonType editar = new ButtonType("Editar producto", ButtonBar.ButtonData.LEFT);
        ButtonType r = sesion.esAdmin()
                ? DYUi.ventana(p.getNombre(), new ScrollPane(contenido), editar, ButtonType.CLOSE)
                : DYUi.ventana(p.getNombre(), new ScrollPane(contenido), ButtonType.CLOSE);
        if (r == editar) formulario(p);
    }

    private static VBox stat(String etiqueta, String valor) {
        VBox v = new VBox(4, DYUi.texto(etiqueta, "caption"), DYUi.texto(valor, "h3"));
        v.getStyleClass().add("caja-borde");
        v.setPrefWidth(300);
        return v;
    }
}
