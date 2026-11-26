package com.ejemplo.bipro.controller.dialogo;

import com.ejemplo.bipro.dto.Sugerencia;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FormDialogos {
    private final GastoService gastoService;
    private final TareaService tareaService;
    private final ProveedorService proveedorService;
    private final UsuarioService usuarioService;
    private final CompraService compraService;
    private final Sesion sesion;
    private final EstadoApp estado;

    public boolean gasto(Gasto g) {
        boolean nuevo = g == null;
        TextField txtMonto = new TextField(nuevo ? "" : g.getMonto().toPlainString());
        ComboBox<CategoriaGasto> cmbCat = new ComboBox<>(FXCollections.observableArrayList(CategoriaGasto.values()));
        cmbCat.setValue(nuevo ? null : g.getCategoria());
        DatePicker dpFecha = new DatePicker(nuevo ? LocalDate.now() : g.getFecha());
        TextField txtDesc = new TextField(nuevo ? "" : Objects.toString(g.getDescripcion(), ""));
        Label lblArchivo = Ui.texto(nuevo || g.getComprobante() == null ? "Ningún archivo seleccionado" : g.getComprobante(), "caption");
        String[] archivo = {nuevo ? null : g.getComprobante()};
        Button btnArchivo = Ui.boton("Examinar…", "fth-paperclip", "secundario");
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

        GridPane grid = Ui.grilla();
        Ui.campo(grid, 0, 0, 1, "Monto (S/) *", txtMonto);
        Ui.campo(grid, 0, 1, 1, "Categoría *", cmbCat);
        Ui.campo(grid, 1, 0, 1, "Fecha *", dpFecha);
        Ui.campo(grid, 1, 1, 1, "Descripción", txtDesc);
        HBox comp = new HBox(10, btnArchivo, lblArchivo);
        comp.setAlignment(Pos.CENTER_LEFT);
        grid.add(new VBox(6, Ui.texto("Comprobante (opcional)", "etiqueta-campo"), comp), 0, 2, 2, 1);

        boolean ok = Ui.formulario(nuevo ? "Registrar gasto" : "Editar gasto", grid, "Guardar", () -> {
            Gasto x = nuevo ? new Gasto() : g;
            x.setMonto(Formato.leerMonto(txtMonto.getText()));
            x.setCategoria(cmbCat.getValue());
            x.setFecha(dpFecha.getValue());
            x.setDescripcion(txtDesc.getText().trim());
            x.setComprobante(archivo[0]);
            gastoService.guardar(x);
        });
        if (ok) estado.notificarCambio();
        return ok;
    }

    public boolean tarea(Tarea t) {
        boolean nuevo = t == null;
        TextField txtTitulo = new TextField(nuevo ? "" : t.getTitulo());
        ComboBox<Prioridad> cmbPrioridad = new ComboBox<>(FXCollections.observableArrayList(Prioridad.values()));
        cmbPrioridad.setValue(nuevo ? Prioridad.MEDIA : t.getPrioridad());
        ComboBox<EstadoTarea> cmbEstado = new ComboBox<>(FXCollections.observableArrayList(EstadoTarea.values()));
        cmbEstado.setValue(nuevo ? EstadoTarea.PENDIENTE : t.getEstado());
        DatePicker dpLimite = new DatePicker(nuevo ? null : t.getFechaLimite());
        Usuario sinAsignar = Usuario.builder().nombre("Sin asignar").build();
        ComboBox<Usuario> cmbResp = new ComboBox<>();
        cmbResp.getItems().add(sinAsignar);
        cmbResp.getItems().addAll(usuarioService.listar().stream().filter(Usuario::isActivo).toList());
        cmbResp.setValue(nuevo || t.getResponsable() == null ? sinAsignar : cmbResp.getItems().stream()
                .filter(u -> t.getResponsable().getId().equals(u.getId())).findFirst().orElse(sinAsignar));

        GridPane g = Ui.grilla();
        Ui.campo(g, 0, 0, 2, "Título *", txtTitulo);
        Ui.campo(g, 1, 0, 1, "Prioridad *", cmbPrioridad);
        Ui.campo(g, 1, 1, 1, "Estado *", cmbEstado);
        Ui.campo(g, 2, 0, 1, "Fecha límite", dpLimite);
        Ui.campo(g, 2, 1, 1, "Responsable", cmbResp);

        boolean ok = Ui.formulario(nuevo ? "Nueva tarea" : "Editar tarea", g, "Guardar", () -> {
            Tarea x = nuevo ? new Tarea() : t;
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

    public boolean proveedor(Proveedor p) {
        boolean nuevo = p == null;
        TextField txtNombre = new TextField(nuevo ? "" : p.getNombre());
        TextField txtRuc = new TextField(nuevo ? "" : p.getRuc());
        TextField txtContacto = new TextField(nuevo ? "" : Objects.toString(p.getContacto(), ""));
        TextField txtTel = new TextField(nuevo ? "" : Objects.toString(p.getTelefono(), ""));
        Spinner<Integer> spnPlazo = new Spinner<>(1, 60, nuevo ? 3 : p.getPlazoEntregaDias());

        GridPane g = Ui.grilla();
        Ui.campo(g, 0, 0, 2, "Razón social *", txtNombre);
        Ui.campo(g, 1, 0, 1, "RUC *", txtRuc);
        Ui.campo(g, 1, 1, 1, "Plazo de entrega (días) *", spnPlazo);
        Ui.campo(g, 2, 0, 1, "Contacto", txtContacto);
        Ui.campo(g, 2, 1, 1, "Teléfono", txtTel);

        boolean ok = Ui.formulario(nuevo ? "Nuevo proveedor" : "Editar proveedor", g, "Guardar", () -> {
            Proveedor x = nuevo ? new Proveedor() : p;
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
        ComboBox<Rol> cmbRol = new ComboBox<>(FXCollections.observableArrayList(Rol.values()));
        cmbRol.setValue(Rol.VENDEDOR);
        GridPane g = Ui.grilla();
        Ui.campo(g, 0, 0, 2, "Nombre completo *", txtNombre);
        Ui.campo(g, 1, 0, 1, "Usuario *", txtUsuario);
        Ui.campo(g, 1, 1, 1, "Rol *", cmbRol);
        Ui.campo(g, 2, 0, 1, "Clave (mín. 4) *", txtClave);
        return Ui.formulario("Nuevo usuario", g, "Crear usuario",
                () -> usuarioService.crear(txtUsuario.getText(), txtNombre.getText(), txtClave.getText(), cmbRol.getValue()));
    }

    public void ordenSugerida() {
        if (!sesion.esAdmin()) {
            Alertas.error("Permiso denegado", "Solo un administrador puede generar órdenes de compra.");
            return;
        }
        List<Sugerencia> sugerencias = compraService.sugerencia();
        if (sugerencias.isEmpty()) {
            Alertas.info("Sin productos por reponer",
                    "Todos los productos están sobre el mínimo o ya tienen una orden pendiente.");
            return;
        }
        TableView<Sugerencia> tabla = new TableView<>(FXCollections.observableArrayList(sugerencias));
        Ui.prepararTabla(tabla, "");
        Ui.col(tabla, "Producto", 220, s -> s.producto().getNombre());
        Ui.colNodo(tabla, "Estado", 90, s -> Ui.badge(s.producto().getEstado().getEtiqueta(), s.producto().getEstado().getEstilo()));
        Ui.col(tabla, "Proveedor", 180, s -> s.proveedor() == null ? "Sin proveedor" : s.proveedor().getNombre());
        Ui.col(tabla, "Cantidad", 80, s -> String.valueOf(s.cantidad()));
        Ui.col(tabla, "Costo", 100, s -> Formato.soles(s.costo()));
        tabla.setPrefHeight(280);
        BigDecimal total = sugerencias.stream().map(Sugerencia::costo).reduce(BigDecimal.ZERO, BigDecimal::add);
        VBox contenido = new VBox(12,
                Ui.texto("Productos AGOTADOS o CRÍTICOS · cantidad = stock máximo − stock actual · una orden por proveedor", "caption"),
                tabla, new HBox(10, Ui.espaciador(), Ui.texto("Total estimado", "caption"), Ui.texto(Formato.soles(total), "h3")));
        contenido.setPrefWidth(720);

        List<OrdenCompra> creadas = new ArrayList<>();
        if (Ui.formulario("Orden de compra sugerida", contenido, "Generar órdenes",
                () -> creadas.addAll(compraService.generarOrdenes()))) {
            estado.notificarCambio();
            Alertas.info(creadas.size() + " orden(es) de compra generada(s)", creadas.stream()
                    .map(o -> o.getNumero() + " · " + o.getProveedor().getNombre() + " · " + Formato.soles(o.getTotal()))
                    .collect(Collectors.joining("\n")));
        }
    }
}
