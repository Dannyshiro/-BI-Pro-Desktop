package com.dy.bipro.controller.dialogo;

import com.dy.bipro.dto.DYLineaVenta;
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
import javafx.print.PrinterJob;
import javafx.scene.Node;

@Component
@RequiredArgsConstructor
public class DYVentaDialogos {
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final DYVentaService ventaService;
    private final DYClienteService clienteService;
    private final DYProductoService productoService;
    private final DYCajaService cajaService;
    private final DYEmpresaService empresaService;
    private final DYSesion sesion;
    private final DYEstadoApp estado;

    public Optional<DYVenta> nuevaVenta(DYCliente preseleccionado) {
        if (cajaService.turnoAbierto().isEmpty()) {
            if (DYAlertas.confirmar("La caja está cerrada",
                    "Cada venta se asocia al turno de caja abierto. ¿Ir a Caja para abrirla?")) {
                estado.ir(DYModulo.CAJA);
            }
            return Optional.empty();
        }

        DYCliente generico = DYCliente.builder().nombre("Cliente genérico").documento("").build();
        ComboBox<DYCliente> cmbCliente = new ComboBox<>();
        cmbCliente.getItems().add(generico);
        cmbCliente.getItems().addAll(clienteService.listar());
        cmbCliente.setConverter(convertidor(c -> c == generico ? "Cliente genérico" : c.getNombre() + " · " + c.getDocumento()));
        cmbCliente.setValue(preseleccionado == null ? generico : cmbCliente.getItems().stream()
                .filter(c -> preseleccionado.getId().equals(c.getId())).findFirst().orElse(generico));

        ComboBox<DYMetodoPago> cmbMetodo = new ComboBox<>(FXCollections.observableArrayList(DYMetodoPago.values()));
        cmbMetodo.setValue(DYMetodoPago.EFECTIVO);

        ComboBox<DYProducto> cmbProducto = new ComboBox<>(FXCollections.observableArrayList(
                productoService.listarTodos().stream().filter(p -> p.getStockActual() > 0).toList()));
        cmbProducto.setPromptText("Seleccione un producto");
        cmbProducto.setPrefWidth(360);
        Spinner<Integer> spnCantidad = new Spinner<>(1, 9999, 1);
        spnCantidad.setEditable(true);
        spnCantidad.setPrefWidth(110);
        Button btnAgregar = DYUi.boton("Agregar", "fth-plus", "secundario");

        ObservableList<DYLineaVenta> lineas = FXCollections.observableArrayList();
        TableView<DYLineaVenta> tabla = new TableView<>(lineas);
        DYUi.prepararTabla(tabla, "Sin productos. Agrega al menos uno.");
        DYUi.col(tabla, "Producto", 260, l -> l.producto().getNombre());
        DYUi.col(tabla, "Cant.", 70, l -> String.valueOf(l.cantidad()));
        DYUi.col(tabla, "P. unit.", 100, l -> DYFormato.soles(l.producto().getPrecioVenta()));
        DYUi.col(tabla, "Importe", 110, l -> DYFormato.soles(l.importe()));
        DYUi.colNodo(tabla, "", 50, l -> DYUi.botonIcono("fth-x", "Quitar", () -> lineas.remove(l)));
        tabla.setPrefHeight(230);

        Label lblTotal = DYUi.texto("S/ 0.00", "total-grande");
        lineas.addListener((javafx.collections.ListChangeListener<DYLineaVenta>) c -> lblTotal.setText(
                DYFormato.soles(lineas.stream().map(DYLineaVenta::importe).reduce(BigDecimal.ZERO, BigDecimal::add))));

        btnAgregar.setOnAction(e -> {
            if (cmbProducto.getValue() == null) {
                DYAlertas.advertencia("Seleccione un producto", "Elige el producto antes de agregarlo.");
                return;
            }
            lineas.add(new DYLineaVenta(cmbProducto.getValue(), spnCantidad.getValue()));
            cmbProducto.setValue(null);
            spnCantidad.getValueFactory().setValue(1);
        });

        GridPane g = DYUi.grilla();
        DYUi.campo(g, 0, 0, 1, "Cliente *", cmbCliente);
        DYUi.campo(g, 0, 1, 1, "Método de pago *", cmbMetodo);
        HBox agregar = new HBox(10, cmbProducto, spnCantidad, btnAgregar);
        agregar.setAlignment(Pos.CENTER_LEFT);
        HBox total = new HBox(10, DYUi.texto("Total", "caption"), lblTotal);
        total.setAlignment(Pos.CENTER_RIGHT);
        VBox contenido = new VBox(14, g, DYUi.texto("Detalle de venta *", "etiqueta-campo"), agregar, tabla, total);
        contenido.setPrefWidth(720);

        DYVenta[] resultado = new DYVenta[1];
        boolean ok = DYUi.formulario("Nueva venta", contenido, "Registrar venta", () -> {
            DYCliente c = cmbCliente.getValue() == generico ? null : cmbCliente.getValue();
            resultado[0] = ventaService.registrar(c, cmbMetodo.getValue(), new ArrayList<>(lineas));
        });
        if (!ok || resultado[0] == null) return Optional.empty();

        estado.notificarCambio();
        DYVenta v = resultado[0];
        if (DYAlertas.confirmar("Venta " + v.getNumero() + " registrada",
                v.getClienteNombre() + " · " + DYFormato.soles(v.getTotal()) + "\n¿Deseas ver el comprobante?")) {
            comprobante(v.getId());
        }
        return Optional.of(v);
    }

    public void comprobante(Long ventaId) {
        DYVenta v = ventaService.buscarConDetalles(ventaId);
        DYEmpresa e = empresaService.obtener();
        BigDecimal factor = BigDecimal.ONE.add(e.getIgv().divide(CIEN, 4, RoundingMode.HALF_UP));
        BigDecimal gravada = v.getTotal().divide(factor, 2, RoundingMode.HALF_UP);
        String numero = e.getSerieBoleta() + "-" + String.format("%08d", v.getId());

        VBox cab = new VBox(3, DYUi.texto(e.getRazonSocial(), "h3"),
                DYUi.texto("RUC " + e.getRuc() + " · " + DYFormato.vacio(e.getDireccion()), "caption"),
                DYUi.texto("BOLETA DE VENTA ELECTRÓNICA", "boleta-tipo"), DYUi.texto(numero, "boleta-numero"));
        cab.setAlignment(Pos.CENTER);

        GridPane meta = new GridPane();
        meta.setHgap(24);
        meta.setVgap(8);
        meta.addRow(0, dato("Fecha", DYFormato.fechaHora(v.getFecha())), dato("Cliente", v.getClienteNombre()));
        meta.addRow(1, dato("Atendido por", v.getUsuario().getNombre()), dato("Forma de pago", v.getMetodoPago().getEtiqueta()));
        meta.getStyleClass().add("boleta-meta");

        GridPane lineas = new GridPane();
        lineas.setHgap(16);
        lineas.setVgap(6);
        lineas.addRow(0, DYUi.texto("Cant.", "caption"), DYUi.texto("Descripción", "caption"),
                DYUi.texto("P. unit.", "caption"), DYUi.texto("Importe", "caption"));
        int fila = 1;
        for (DYDetalleVenta d : v.getDetalles()) {
            lineas.addRow(fila++, new Label(String.valueOf(d.getCantidad())), new Label(d.getProducto().getNombre()),
                    new Label(DYFormato.numero(d.getPrecioUnitario())), new Label(DYFormato.numero(d.getImporte())));
        }

        VBox totales = new VBox(6,
                filaTotal("Op. gravada", DYFormato.soles(gravada), false),
                filaTotal("IGV (" + e.getIgv().stripTrailingZeros().toPlainString() + "%)", DYFormato.soles(v.getTotal().subtract(gravada)), false),
                filaTotal("Total", DYFormato.soles(v.getTotal()), true));

        VBox contenido = new VBox(14, cab, meta, lineas, new Separator(), totales);
        contenido.setPrefWidth(480);
        contenido.getStyleClass().add("boleta");
        if (v.getEstado() == DYEstadoVenta.ANULADA) contenido.getChildren().add(1, DYUi.badge("VENTA ANULADA", "peligro"));

        ButtonType anular = new ButtonType("Anular venta", ButtonBar.ButtonData.LEFT);
        ButtonType imprimir = new ButtonType("Imprimir", ButtonBar.ButtonData.OTHER);
        List<ButtonType> botones = new ArrayList<>();
        if (v.getEstado() == DYEstadoVenta.EMITIDA && sesion.esAdmin()) botones.add(anular);
        botones.add(imprimir);
        botones.add(ButtonType.CLOSE);

        ButtonType r = DYUi.ventana("Comprobante " + numero, contenido, botones.toArray(new ButtonType[0]));
        if (r == anular && DYAlertas.confirmar("¿Anular la venta " + v.getNumero() + "?",
                "El stock de los productos se repondrá y el importe se descontará del turno de caja.")) {
            try {
                ventaService.anular(v.getId());
                estado.notificarCambio();
                DYAlertas.info("Venta anulada", v.getNumero() + " quedó anulada y el stock fue repuesto.");
            } catch (Exception ex) {
                DYAlertas.manejar(ex);
            }
        } else if (r == imprimir) {
            imprimir(contenido);
        }
    }

    private void imprimir(Node nodo) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(null)) {
            if (job.printPage(nodo)) job.endJob();
        }
    }

    private static VBox dato(String etiqueta, String valor) {
        return new VBox(2, DYUi.texto(etiqueta, "caption"), DYUi.texto(valor, "dato-valor"));
    }

    private static HBox filaTotal(String etiqueta, String valor, boolean fuerte) {
        HBox h = new HBox(DYUi.texto(etiqueta, fuerte ? "h3" : "caption"), DYUi.espaciador(), DYUi.texto(valor, fuerte ? "h3" : "caption"));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    static <T> StringConverter<T> convertidor(java.util.function.Function<T, String> f) {
        return new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? "" : f.apply(t);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }
}
