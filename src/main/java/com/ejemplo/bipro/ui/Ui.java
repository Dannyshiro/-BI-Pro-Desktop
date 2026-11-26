package com.ejemplo.bipro.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Function;

public final class Ui {
    private static Window ventana;
    private static String hojaEstilos;

    private Ui() {
    }

    public static void configurar(Window owner, String css) {
        ventana = owner;
        hojaEstilos = css;
    }

    public static void preparar(DialogPane pane, Dialog<?> dialogo) {
        if (ventana != null) dialogo.initOwner(ventana);
        if (hojaEstilos != null) pane.getStylesheets().add(hojaEstilos);
        pane.getStyleClass().add("dialogo");
    }

    public static FontIcon icono(String codigo, int tamanio) {
        FontIcon i = new FontIcon(codigo);
        i.setIconSize(tamanio);
        return i;
    }

    public static Label badge(String texto, String estilo) {
        Label l = new Label(texto);
        l.getStyleClass().addAll("badge", "badge-" + estilo);
        return l;
    }

    public static Region espaciador() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Button boton(String texto, String icono, String... clases) {
        Button b = new Button(texto, icono == null ? null : icono(icono, 15));
        b.getStyleClass().addAll(clases);
        return b;
    }

    public static Button botonIcono(String icono, String ayuda, Runnable accion) {
        Button b = new Button(null, icono(icono, 15));
        b.getStyleClass().add("boton-icono");
        b.setTooltip(new Tooltip(ayuda));
        b.setOnAction(e -> accion.run());
        return b;
    }

    public static VBox kpi(String titulo, String valor, String detalle, String icono, boolean destacado, Boolean positivo) {
        Label lt = new Label(titulo);
        lt.getStyleClass().add("kpi-titulo");
        StackPane ic = new StackPane(icono(icono, 16));
        ic.getStyleClass().add("kpi-icono");
        HBox cab = new HBox(lt, espaciador(), ic);
        cab.setAlignment(Pos.CENTER_LEFT);
        Label lv = new Label(valor);
        lv.getStyleClass().add("kpi-valor");
        Label ld = new Label(detalle);
        ld.getStyleClass().add(positivo == null ? "kpi-detalle" : positivo ? "positivo" : "negativo");
        ld.setWrapText(true);
        VBox v = new VBox(cab, lv, ld);
        v.getStyleClass().addAll("tarjeta", "kpi");
        if (destacado) v.getStyleClass().add("kpi-destacado");
        v.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    public static VBox tarjeta(String titulo, String subtitulo, Node... contenido) {
        Label t = new Label(titulo);
        t.getStyleClass().add("h3");
        VBox cab = new VBox(2, t);
        if (subtitulo != null) {
            Label s = new Label(subtitulo);
            s.getStyleClass().add("caption");
            cab.getChildren().add(s);
        }
        VBox v = new VBox(cab);
        v.getChildren().addAll(contenido);
        v.getStyleClass().add("tarjeta");
        return v;
    }

    public static Label texto(String texto, String... clases) {
        Label l = new Label(texto);
        l.getStyleClass().addAll(clases);
        l.setWrapText(true);
        return l;
    }

    public static <S> TableColumn<S, String> col(TableView<S> tabla, String titulo, double ancho, Function<S, String> valor) {
        TableColumn<S, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleStringProperty(valor.apply(d.getValue())));
        if (ancho > 0) c.setPrefWidth(ancho);
        tabla.getColumns().add(c);
        return c;
    }

    public static <S> TableColumn<S, S> colNodo(TableView<S> tabla, String titulo, double ancho, Function<S, Node> nodo) {
        TableColumn<S, S> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        c.setCellFactory(x -> new TableCell<>() {
            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : nodo.apply(item));
            }
        });
        c.setSortable(false);
        if (ancho > 0) c.setPrefWidth(ancho);
        tabla.getColumns().add(c);
        return c;
    }

    public static <S> void prepararTabla(TableView<S> tabla, String vacio) {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label(vacio));
    }

    public static GridPane grilla() {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(12);
        ColumnConstraints c = new ColumnConstraints(240);
        c.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c, new ColumnConstraints(240));
        return g;
    }

    public static void campo(GridPane g, int fila, int columna, int columnas, String etiqueta, Control control) {
        Label l = new Label(etiqueta);
        l.getStyleClass().add("etiqueta-campo");
        control.setMaxWidth(Double.MAX_VALUE);
        g.add(new VBox(6, l, control), columna, fila, columnas, 1);
    }

    public static boolean formulario(String titulo, Node contenido, String textoOk, Runnable accion) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(titulo);
        d.setHeaderText(titulo);
        preparar(d.getDialogPane(), d);
        d.getDialogPane().setContent(contenido);
        ButtonType ok = new ButtonType(textoOk, ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        Button b = (Button) d.getDialogPane().lookupButton(ok);
        b.getStyleClass().add("accent");
        b.addEventFilter(ActionEvent.ACTION, e -> {
            try {
                accion.run();
            } catch (Exception ex) {
                Alertas.manejar(ex);
                e.consume();
            }
        });
        return d.showAndWait().filter(r -> r == ok).isPresent();
    }

    public static ButtonType ventana(String titulo, Node contenido, ButtonType... botones) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(titulo);
        preparar(d.getDialogPane(), d);
        d.getDialogPane().setContent(contenido);
        d.getDialogPane().getButtonTypes().addAll(botones);
        return d.showAndWait().orElse(ButtonType.CLOSE);
    }
}
