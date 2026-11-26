package com.ejemplo.bipro.controller.dialogo;

import com.ejemplo.bipro.dto.LineaVenta;
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
import javafx.print.PrinterJob;
import javafx.scene.Node;

@Component
@RequiredArgsConstructor
public class VentaDialogos {
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final VentaService ventaService;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final CajaService cajaService;
    private final EmpresaService empresaService;
    private final Sesion sesion;
    private final EstadoApp estado;

    public Optional<Venta> nuevaVenta(Cliente preseleccionado) {
        if (cajaService.turnoAbierto().isEmpty()) {
            if (Alertas.confirmar("La caja está cerrada",
                    "Cada venta se asocia al turno de caja abierto. ¿Ir a Caja para abrirla?")) {
                estado.ir(Modulo.CAJA);
            }
            return Optional.empty();
        }

        Cliente generico = Cliente.builder().nombre("Cliente genérico").documento("").build();
        ComboBox<Cliente> cmbCliente = new ComboBox<>();
        cmbCliente.getItems().add(generico);
        cmbCliente.getItems().addAll(clienteService.listar());
        cmbCliente.setConverter(convertidor(c -> c == generico ? "Cliente genérico" : c.getNombre() + " · " + c.getDocumento()));
        cmbCliente.setValue(preseleccionado == null ? generico : cmbCliente.getItems().stream()
                .filter(c -> preseleccionado.getId().equals(c.getId())).findFirst().orElse(generico));

        ComboBox<MetodoPago> cmbMetodo = new ComboBox<>(FXCollections.observableArrayList(MetodoPago.values()));
        cmbMetodo.setValue(MetodoPago.EFECTIVO);

        ComboBox<Producto> cmbProducto = new ComboBox<>(FXCollections.observableArrayList(
                productoService.listarTodos().stream().filter(p -> p.getStockActual() > 0).toList()));
        cmbProducto.setPromptText("Seleccione un producto");
        cmbProducto.setPrefWidth(360);
        Spinner<Integer> spnCantidad = new Spinner<>(1, 9999, 1);
        spnCantidad.setEditable(true);
        spnCantidad.setPrefWidth(110);
        Button btnAgregar = Ui.boton("Agregar", "fth-plus", "secundario");

        ObservableList<LineaVenta> lineas = FXCollections.observableArrayList();
        TableView<LineaVenta> tabla = new TableView<>(lineas);
        Ui.prepararTabla(tabla, "Sin productos. Agrega al menos uno.");
        Ui.col(tabla, "Producto", 260, l -> l.producto().getNombre());
        Ui.col(tabla, "Cant.", 70, l -> String.valueOf(l.cantidad()));
        Ui.col(tabla, "P. unit.", 100, l -> Formato.soles(l.producto().getPrecioVenta()));
        Ui.col(tabla, "Importe", 110, l -> Formato.soles(l.importe()));
        Ui.colNodo(tabla, "", 50, l -> Ui.botonIcono("fth-x", "Quitar", () -> lineas.remove(l)));
        tabla.setPrefHeight(230);

        Label lblTotal = Ui.texto("S/ 0.00", "total-grande");
        lineas.addListener((javafx.collections.ListChangeListener<LineaVenta>) c -> lblTotal.setText(
                Formato.soles(lineas.stream().map(LineaVenta::importe).reduce(BigDecimal.ZERO, BigDecimal::add))));

        btnAgregar.setOnAction(e -> {
            if (cmbProducto.getValue() == null) {
                Alertas.advertencia("Seleccione un producto", "Elige el producto antes de agregarlo.");
                return;
            }
            lineas.add(new LineaVenta(cmbProducto.getValue(), spnCantidad.getValue()));
            cmbProducto.setValue(null);
            spnCantidad.getValueFactory().setValue(1);
        });

        GridPane g = Ui.grilla();
        Ui.campo(g, 0, 0, 1, "Cliente *", cmbCliente);
        Ui.campo(g, 0, 1, 1, "Método de pago *", cmbMetodo);
        HBox agregar = new HBox(10, cmbProducto, spnCantidad, btnAgregar);
        agregar.setAlignment(Pos.CENTER_LEFT);
        HBox total = new HBox(10, Ui.texto("Total", "caption"), lblTotal);
        total.setAlignment(Pos.CENTER_RIGHT);
        VBox contenido = new VBox(14, g, Ui.texto("Detalle de venta *", "etiqueta-campo"), agregar, tabla, total);
        contenido.setPrefWidth(720);

        Venta[] resultado = new Venta[1];
        boolean ok = Ui.formulario("Nueva venta", contenido, "Registrar venta", () -> {
            Cliente c = cmbCliente.getValue() == generico ? null : cmbCliente.getValue();
            resultado[0] = ventaService.registrar(c, cmbMetodo.getValue(), new ArrayList<>(lineas));
        });
        if (!ok || resultado[0] == null) return Optional.empty();

        estado.notificarCambio();
        Venta v = resultado[0];
        if (Alertas.confirmar("Venta " + v.getNumero() + " registrada",
                v.getClienteNombre() + " · " + Formato.soles(v.getTotal()) + "\n¿Deseas ver el comprobante?")) {
            comprobante(v.getId());
        }
        return Optional.of(v);
    }

    public void comprobante(Long ventaId) {
        Venta v = ventaService.buscarConDetalles(ventaId);
        Empresa e = empresaService.obtener();
        BigDecimal factor = BigDecimal.ONE.add(e.getIgv().divide(CIEN, 4, RoundingMode.HALF_UP));
        BigDecimal gravada = v.getTotal().divide(factor, 2, RoundingMode.HALF_UP);
        String numero = e.getSerieBoleta() + "-" + String.format("%08d", v.getId());

        VBox cab = new VBox(3, Ui.texto(e.getRazonSocial(), "h3"),
                Ui.texto("RUC " + e.getRuc() + " · " + Formato.vacio(e.getDireccion()), "caption"),
                Ui.texto("BOLETA DE VENTA ELECTRÓNICA", "boleta-tipo"), Ui.texto(numero, "boleta-numero"));
        cab.setAlignment(Pos.CENTER);

        GridPane meta = new GridPane();
        meta.setHgap(24);
        meta.setVgap(8);
        meta.addRow(0, dato("Fecha", Formato.fechaHora(v.getFecha())), dato("Cliente", v.getClienteNombre()));
        meta.addRow(1, dato("Atendido por", v.getUsuario().getNombre()), dato("Forma de pago", v.getMetodoPago().getEtiqueta()));
        meta.getStyleClass().add("boleta-meta");

        GridPane lineas = new GridPane();
        lineas.setHgap(16);
        lineas.setVgap(6);
        lineas.addRow(0, Ui.texto("Cant.", "caption"), Ui.texto("Descripción", "caption"),
                Ui.texto("P. unit.", "caption"), Ui.texto("Importe", "caption"));
        int fila = 1;
        for (DetalleVenta d : v.getDetalles()) {
            lineas.addRow(fila++, new Label(String.valueOf(d.getCantidad())), new Label(d.getProducto().getNombre()),
                    new Label(Formato.numero(d.getPrecioUnitario())), new Label(Formato.numero(d.getImporte())));
        }

        VBox totales = new VBox(6,
                filaTotal("Op. gravada", Formato.soles(gravada), false),
                filaTotal("IGV (" + e.getIgv().stripTrailingZeros().toPlainString() + "%)", Formato.soles(v.getTotal().subtract(gravada)), false),
                filaTotal("Total", Formato.soles(v.getTotal()), true));

        VBox contenido = new VBox(14, cab, meta, lineas, new Separator(), totales);
        contenido.setPrefWidth(480);
        contenido.getStyleClass().add("boleta");
        if (v.getEstado() == EstadoVenta.ANULADA) contenido.getChildren().add(1, Ui.badge("VENTA ANULADA", "peligro"));

        ButtonType anular = new ButtonType("Anular venta", ButtonBar.ButtonData.LEFT);
        ButtonType imprimir = new ButtonType("Imprimir", ButtonBar.ButtonData.OTHER);
        List<ButtonType> botones = new ArrayList<>();
        if (v.getEstado() == EstadoVenta.EMITIDA && sesion.esAdmin()) botones.add(anular);
        botones.add(imprimir);
        botones.add(ButtonType.CLOSE);

        ButtonType r = Ui.ventana("Comprobante " + numero, contenido, botones.toArray(new ButtonType[0]));
        if (r == anular && Alertas.confirmar("¿Anular la venta " + v.getNumero() + "?",
                "El stock de los productos se repondrá y el importe se descontará del turno de caja.")) {
            try {
                ventaService.anular(v.getId());
                estado.notificarCambio();
                Alertas.info("Venta anulada", v.getNumero() + " quedó anulada y el stock fue repuesto.");
            } catch (Exception ex) {
                Alertas.manejar(ex);
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
        return new VBox(2, Ui.texto(etiqueta, "caption"), Ui.texto(valor, "dato-valor"));
    }

    private static HBox filaTotal(String etiqueta, String valor, boolean fuerte) {
        HBox h = new HBox(Ui.texto(etiqueta, fuerte ? "h3" : "caption"), Ui.espaciador(), Ui.texto(valor, fuerte ? "h3" : "caption"));
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
