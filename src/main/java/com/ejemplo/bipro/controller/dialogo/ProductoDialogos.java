package com.ejemplo.bipro.controller.dialogo;

import com.ejemplo.bipro.model.*;
import com.ejemplo.bipro.service.*;
import com.ejemplo.bipro.ui.*;
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
public class ProductoDialogos {
    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final CompraService compraService;
    private final Sesion sesion;
    private final EstadoApp estado;

    public boolean formulario(Producto p) {
        boolean nuevo = p == null;
        TextField txtNombre = new TextField(nuevo ? "" : p.getNombre());
        TextField txtSku = new TextField(nuevo ? "" : p.getSku());
        ComboBox<Categoria> cmbCategoria = new ComboBox<>(FXCollections.observableArrayList(categoriaService.listar()));
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

        GridPane g = Ui.grilla();
        Ui.campo(g, 0, 0, 2, "Nombre *", txtNombre);
        Ui.campo(g, 1, 0, 1, "SKU *", txtSku);
        Ui.campo(g, 1, 1, 1, "Categoría *", cmbCategoria);
        Ui.campo(g, 2, 0, 2, "Descripción", txtDescripcion);
        Ui.campo(g, 3, 0, 1, "Precio de compra (S/) *", txtCompra);
        Ui.campo(g, 3, 1, 1, "Precio de venta (S/) *", txtVenta);
        GridPane stock = new GridPane();
        stock.setHgap(12);
        stock.getStyleClass().add("grupo-stock");
        Ui.campo(stock, 0, 0, 1, "Stock actual *", txtStock);
        Ui.campo(stock, 0, 1, 1, "Stock mínimo *", txtMin);
        Ui.campo(stock, 0, 2, 1, "Stock máximo *", txtMax);
        g.add(stock, 0, 4, 2, 1);

        boolean ok = Ui.formulario(nuevo ? "Nuevo producto" : "Editar producto", g, "Guardar", () -> {
            Producto x = nuevo ? new Producto() : p;
            x.setNombre(txtNombre.getText().trim());
            x.setSku(txtSku.getText().trim());
            x.setCategoria(cmbCategoria.getValue());
            x.setDescripcion(txtDescripcion.getText().trim());
            x.setPrecioCompra(Formato.leerMonto(txtCompra.getText()));
            x.setPrecioVenta(Formato.leerMonto(txtVenta.getText()));
            x.setStockActual(Formato.leerEntero(txtStock.getText()));
            x.setStockMinimo(Formato.leerEntero(txtMin.getText()));
            x.setStockMaximo(Formato.leerEntero(txtMax.getText()));
            productoService.guardar(x);
        });
        if (ok) {
            estado.notificarCambio();
            Alertas.info(nuevo ? "Producto registrado" : "Producto actualizado", txtNombre.getText().trim());
        }
        return ok;
    }

    public void detalle(Producto p) {
        Label lblStock = Ui.texto("", "kpi-valor");
        ProgressBar barra = new ProgressBar();
        barra.setMaxWidth(Double.MAX_VALUE);
        Label lblEstado = new Label();
        TableView<MovimientoStock> movimientos = new TableView<>();
        Ui.prepararTabla(movimientos, "Sin movimientos registrados.");
        Ui.col(movimientos, "Movimiento", 150, m -> m.getTipo().getEtiqueta());
        Ui.col(movimientos, "Referencia", 140, m -> Formato.vacio(m.getReferencia()));
        Ui.col(movimientos, "Fecha", 130, m -> Formato.fechaHora(m.getFecha()));
        Ui.col(movimientos, "Cant.", 60, m -> (m.getCantidad() > 0 ? "+" : "") + m.getCantidad());
        Ui.col(movimientos, "Saldo", 60, m -> String.valueOf(m.getSaldo()));
        movimientos.setPrefHeight(220);

        Runnable refrescar = () -> {
            Producto x = productoService.listarTodos().stream().filter(y -> y.getId().equals(p.getId())).findFirst().orElse(p);
            lblStock.setText(x.getStockActual() + " u.");
            barra.setProgress(Math.min(1.0, x.getStockActual() / (double) x.getStockMaximo()));
            lblEstado.setGraphic(Ui.badge(x.getEstado().getEtiqueta(), x.getEstado().getEstilo()));
            movimientos.getItems().setAll(productoService.movimientos(x));
        };
        refrescar.run();

        BigDecimal margen = p.getPrecioVenta().signum() == 0 ? BigDecimal.ZERO
                : p.getPrecioVenta().subtract(p.getPrecioCompra()).multiply(BigDecimal.valueOf(100))
                .divide(p.getPrecioVenta(), 1, RoundingMode.HALF_UP);
        GridPane stats = new GridPane();
        stats.setHgap(12);
        stats.setVgap(12);
        stats.addRow(0, stat("Precio de compra", Formato.soles(p.getPrecioCompra())), stat("Precio de venta", Formato.soles(p.getPrecioVenta())));
        stats.addRow(1, stat("Margen unitario", margen + "%"),
                stat("Valor del inventario", Formato.soles(p.getPrecioCompra().multiply(BigDecimal.valueOf(p.getStockActual())))));

        Proveedor prov = p.getCategoria().getProveedor();
        boolean enOrden = compraService.productosConOrdenPendiente().contains(p.getId());
        HBox proveedor = new HBox(12, Ui.icono("fth-truck", 18), new VBox(2,
                Ui.texto(prov == null ? "Sin proveedor" : prov.getNombre(), "dato-valor"),
                Ui.texto(prov == null ? "Asigna un proveedor a la categoría" : prov.getContacto() + " · entrega en "
                        + prov.getPlazoEntregaDias() + " días", "caption")), Ui.espaciador());
        if (enOrden) proveedor.getChildren().add(Ui.badge("OC pendiente", "advertencia"));
        proveedor.setAlignment(Pos.CENTER_LEFT);
        proveedor.getStyleClass().add("caja-borde");

        VBox contenido = new VBox(16,
                new HBox(12, Ui.texto(p.getSku() + " · " + p.getCategoria().getNombre(), "caption"), lblEstado),
                new VBox(6, Ui.texto("Stock actual  (mín. " + p.getStockMinimo() + " · máx. " + p.getStockMaximo() + ")", "caption"),
                        lblStock, barra),
                stats, proveedor);

        if (sesion.esAdmin()) {
            TextField txtCant = new TextField();
            txtCant.setPromptText("± cantidad");
            txtCant.setPrefWidth(110);
            ComboBox<String> cmbMotivo = new ComboBox<>(FXCollections.observableArrayList(
                    "Conteo físico", "Merma o vencimiento", "Devolución de cliente", "Consumo interno"));
            cmbMotivo.setValue("Conteo físico");
            Button aplicar = Ui.boton("Aplicar", "fth-sliders", "accent");
            aplicar.setOnAction(e -> {
                try {
                    Integer n = Formato.leerEntero(txtCant.getText());
                    if (n == null) throw new com.ejemplo.bipro.exception.NegocioException("Ingrese un número entero (negativo para salidas).");
                    productoService.ajustar(p.getId(), n, cmbMotivo.getValue());
                    txtCant.clear();
                    refrescar.run();
                    estado.notificarCambio();
                } catch (Exception ex) {
                    Alertas.manejar(ex);
                }
            });
            HBox ajuste = new HBox(10, txtCant, cmbMotivo, aplicar);
            ajuste.setAlignment(Pos.CENTER_LEFT);
            contenido.getChildren().add(new VBox(8, Ui.texto("Ajuste de inventario", "h3"),
                    Ui.texto("Usa valores negativos para salidas.", "caption"), ajuste));
        }
        contenido.getChildren().add(new VBox(8, Ui.texto("Movimientos de stock", "h3"), movimientos));
        contenido.setPrefWidth(640);

        ButtonType editar = new ButtonType("Editar producto", ButtonBar.ButtonData.LEFT);
        ButtonType r = sesion.esAdmin()
                ? Ui.ventana(p.getNombre(), new ScrollPane(contenido), editar, ButtonType.CLOSE)
                : Ui.ventana(p.getNombre(), new ScrollPane(contenido), ButtonType.CLOSE);
        if (r == editar) formulario(p);
    }

    private static VBox stat(String etiqueta, String valor) {
        VBox v = new VBox(4, Ui.texto(etiqueta, "caption"), Ui.texto(valor, "h3"));
        v.getStyleClass().add("caja-borde");
        v.setPrefWidth(300);
        return v;
    }
}
