package com.dy.bipro.controller.dialogo;

import com.dy.bipro.dto.DYSugerencia;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DYFormDialogos {
    private final DYGastoService gastoService;
    private final DYTareaService tareaService;
    private final DYProveedorService proveedorService;
    private final DYUsuarioService usuarioService;
    private final DYCompraService compraService;
    private final DYSesion sesion;
    private final DYEstadoApp estado;

    public boolean gasto(DYGasto g) {
        boolean nuevo = g == null;
        TextField txtMonto = new TextField(nuevo ? "" : g.getMonto().toPlainString());
        ComboBox<DYCategoriaGasto> cmbCat = new ComboBox<>(FXCollections.observableArrayList(DYCategoriaGasto.values()));
        cmbCat.setValue(nuevo ? null : g.getCategoria());
        DatePicker dpFecha = new DatePicker(nuevo ? LocalDate.now() : g.getFecha());
        TextField txtDesc = new TextField(nuevo ? "" : Objects.toString(g.getDescripcion(), ""));
        Label lblArchivo = DYUi.texto(nuevo || g.getComprobante() == null ? "Ningún archivo seleccionado" : g.getComprobante(), "caption");
        String[] archivo = {nuevo ? null : g.getComprobante()};
        Button btnArchivo = DYUi.boton("Examinar…", "fth-paperclip", "secundario");
        btnArchivo.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar comprobante");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF o imagen", "*.pdf", "*.jpg", "*.jpeg", "*.png"));
            File f = fc.showOpenDialog(null);
            if (f != null) {
                archivo[0] = f.getAbsolutePath();
                lblArchivo.setText(f.getName());
            }
        });

        GridPane grid = DYUi.grilla();
        DYUi.campo(grid, 0, 0, 1, "Monto (S/) *", txtMonto);
        DYUi.campo(grid, 0, 1, 1, "Categoría *", cmbCat);
        DYUi.campo(grid, 1, 0, 1, "Fecha *", dpFecha);
        DYUi.campo(grid, 1, 1, 1, "Descripción", txtDesc);
        HBox comp = new HBox(10, btnArchivo, lblArchivo);
        comp.setAlignment(Pos.CENTER_LEFT);
        grid.add(new VBox(6, DYUi.texto("Comprobante (opcional)", "etiqueta-campo"), comp), 0, 2, 2, 1);

        boolean ok = DYUi.formulario(nuevo ? "Registrar gasto" : "Editar gasto", grid, "Guardar", () -> {
            DYGasto x = nuevo ? new DYGasto() : g;
            x.setMonto(DYFormato.leerMonto(txtMonto.getText()));
            x.setCategoria(cmbCat.getValue());
            x.setFecha(dpFecha.getValue());
            x.setDescripcion(txtDesc.getText().trim());
            x.setComprobante(archivo[0]);
            gastoService.guardar(x);
        });
        if (ok) estado.notificarCambio();
        return ok;
    }

    public boolean tarea(DYTarea t) {
        boolean nuevo = t == null;
        TextField txtTitulo = new TextField(nuevo ? "" : t.getTitulo());
        ComboBox<DYPrioridad> cmbPrioridad = new ComboBox<>(FXCollections.observableArrayList(DYPrioridad.values()));
        cmbPrioridad.setValue(nuevo ? DYPrioridad.MEDIA : t.getPrioridad());
        ComboBox<DYEstadoTarea> cmbEstado = new ComboBox<>(FXCollections.observableArrayList(DYEstadoTarea.values()));
        cmbEstado.setValue(nuevo ? DYEstadoTarea.PENDIENTE : t.getEstado());
        DatePicker dpLimite = new DatePicker(nuevo ? null : t.getFechaLimite());
        DYUsuario sinAsignar = DYUsuario.builder().nombre("Sin asignar").build();
        ComboBox<DYUsuario> cmbResp = new ComboBox<>();
        cmbResp.getItems().add(sinAsignar);
        cmbResp.getItems().addAll(usuarioService.listar().stream().filter(DYUsuario::isActivo).toList());
        cmbResp.setValue(nuevo || t.getResponsable() == null ? sinAsignar : cmbResp.getItems().stream()
                .filter(u -> t.getResponsable().getId().equals(u.getId())).findFirst().orElse(sinAsignar));

        GridPane g = DYUi.grilla();
        DYUi.campo(g, 0, 0, 2, "Título *", txtTitulo);
        DYUi.campo(g, 1, 0, 1, "Prioridad *", cmbPrioridad);
        DYUi.campo(g, 1, 1, 1, "Estado *", cmbEstado);
        DYUi.campo(g, 2, 0, 1, "Fecha límite", dpLimite);
        DYUi.campo(g, 2, 1, 1, "Responsable", cmbResp);

        boolean ok = DYUi.formulario(nuevo ? "Nueva tarea" : "Editar tarea", g, "Guardar", () -> {
            DYTarea x = nuevo ? new DYTarea() : t;
            x.setTitulo(txtTitulo.getText().trim());
            x.setPrioridad(cmbPrioridad.getValue());
            x.setEstado(cmbEstado.getValue());
            x.setFechaLimite(dpLimite.getValue());
            x.setResponsable(cmbResp.getValue() == sinAsignar ? null : cmbResp.getValue());
            tareaService.guardar(x);
        });
        if (ok) estado.notificarCambio();
        return ok;
    }

    public boolean proveedor(DYProveedor p) {
        boolean nuevo = p == null;
        TextField txtNombre = new TextField(nuevo ? "" : p.getNombre());
        TextField txtRuc = new TextField(nuevo ? "" : p.getRuc());
        TextField txtContacto = new TextField(nuevo ? "" : Objects.toString(p.getContacto(), ""));
        TextField txtTel = new TextField(nuevo ? "" : Objects.toString(p.getTelefono(), ""));
        Spinner<Integer> spnPlazo = new Spinner<>(1, 60, nuevo ? 3 : p.getPlazoEntregaDias());

        GridPane g = DYUi.grilla();
        DYUi.campo(g, 0, 0, 2, "Razón social *", txtNombre);
        DYUi.campo(g, 1, 0, 1, "RUC *", txtRuc);
        DYUi.campo(g, 1, 1, 1, "Plazo de entrega (días) *", spnPlazo);
        DYUi.campo(g, 2, 0, 1, "Contacto", txtContacto);
        DYUi.campo(g, 2, 1, 1, "Teléfono", txtTel);

        boolean ok = DYUi.formulario(nuevo ? "Nuevo proveedor" : "Editar proveedor", g, "Guardar", () -> {
            DYProveedor x = nuevo ? new DYProveedor() : p;
            x.setNombre(txtNombre.getText().trim());
            x.setRuc(txtRuc.getText().trim());
            x.setContacto(txtContacto.getText().trim());
            x.setTelefono(txtTel.getText().trim());
            x.setPlazoEntregaDias(spnPlazo.getValue());
            proveedorService.guardar(x);
        });
        if (ok) estado.notificarCambio();
        return ok;
    }

    public boolean usuarioNuevo() {
        TextField txtUsuario = new TextField();
        TextField txtNombre = new TextField();
        PasswordField txtClave = new PasswordField();
        ComboBox<DYRol> cmbRol = new ComboBox<>(FXCollections.observableArrayList(DYRol.values()));
        cmbRol.setValue(DYRol.VENDEDOR);
        GridPane g = DYUi.grilla();
        DYUi.campo(g, 0, 0, 2, "Nombre completo *", txtNombre);
        DYUi.campo(g, 1, 0, 1, "Usuario *", txtUsuario);
        DYUi.campo(g, 1, 1, 1, "Rol *", cmbRol);
        DYUi.campo(g, 2, 0, 1, "Clave (mín. 4) *", txtClave);
        return DYUi.formulario("Nuevo usuario", g, "Crear usuario",
                () -> usuarioService.crear(txtUsuario.getText(), txtNombre.getText(), txtClave.getText(), cmbRol.getValue()));
    }

    public void ordenSugerida() {
        if (!sesion.esAdmin()) {
            DYAlertas.error("Permiso denegado", "Solo un administrador puede generar órdenes de compra.");
            return;
        }
        List<DYSugerencia> sugerencias = compraService.sugerencia();
        if (sugerencias.isEmpty()) {
            DYAlertas.info("Sin productos por reponer",
                    "Todos los productos están sobre el mínimo o ya tienen una orden pendiente.");
            return;
        }
        TableView<DYSugerencia> tabla = new TableView<>(FXCollections.observableArrayList(sugerencias));
        DYUi.prepararTabla(tabla, "");
        DYUi.col(tabla, "Producto", 220, s -> s.producto().getNombre());
        DYUi.colNodo(tabla, "Estado", 90, s -> DYUi.badge(s.producto().getEstado().getEtiqueta(), s.producto().getEstado().getEstilo()));
        DYUi.col(tabla, "Proveedor", 180, s -> s.proveedor() == null ? "Sin proveedor" : s.proveedor().getNombre());
        DYUi.col(tabla, "Cantidad", 80, s -> String.valueOf(s.cantidad()));
        DYUi.col(tabla, "Costo", 100, s -> DYFormato.soles(s.costo()));
        tabla.setPrefHeight(280);
        BigDecimal total = sugerencias.stream().map(DYSugerencia::costo).reduce(BigDecimal.ZERO, BigDecimal::add);
        VBox contenido = new VBox(12,
                DYUi.texto("Productos AGOTADOS o CRÍTICOS · cantidad = stock máximo − stock actual · una orden por proveedor", "caption"),
                tabla, new HBox(10, DYUi.espaciador(), DYUi.texto("Total estimado", "caption"), DYUi.texto(DYFormato.soles(total), "h3")));
        contenido.setPrefWidth(720);

        List<DYOrdenCompra> creadas = new ArrayList<>();
        if (DYUi.formulario("Orden de compra sugerida", contenido, "Generar órdenes",
                () -> creadas.addAll(compraService.generarOrdenes()))) {
            estado.notificarCambio();
            DYAlertas.info(creadas.size() + " orden(es) de compra generada(s)", creadas.stream()
                    .map(o -> o.getNumero() + " · " + o.getProveedor().getNombre() + " · " + DYFormato.soles(o.getTotal()))
                    .collect(Collectors.joining("\n")));
        }
    }
}
