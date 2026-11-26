package com.ejemplo.bipro.ui;

import com.ejemplo.bipro.dto.Notificacion;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class NotificacionCell extends ListCell<Notificacion> {
    private final Consumer<Notificacion> alAbrir;
    private final Predicate<String> esLeida;

    public NotificacionCell(Consumer<Notificacion> alAbrir, Predicate<String> esLeida) {
        this.alAbrir = alAbrir;
        this.esLeida = esLeida;
    }

    @Override
    protected void updateItem(Notificacion n, boolean empty) {
        super.updateItem(n, empty);
        setText(null);
        if (empty || n == null) {
            setGraphic(null);
            return;
        }
        StackPane icono = new StackPane(Ui.icono(n.severidad().getIcono(), 16));
        icono.getStyleClass().addAll("icono-severidad", "badge-" + n.severidad().getEstilo());

        Label titulo = Ui.texto(n.titulo(), esLeida.test(n.id()) ? "notif-leida" : "notif-titulo");
        Label desc = Ui.texto(n.descripcion(), "caption");
        HBox meta = new HBox(8, Ui.badge(n.severidad().getEtiqueta(), n.severidad().getEstilo()),
                Ui.badge(Modulo.valueOf(n.modulo()).getTitulo(), "neutro"));
        VBox textos = new VBox(4, titulo, desc, meta);
        HBox.setHgrow(textos, Priority.ALWAYS);
        textos.setMaxWidth(Double.MAX_VALUE);

        Button ver = Ui.boton("Ver detalle", "fth-arrow-right", "flat", "enlace");
        ver.setOnAction(e -> alAbrir.accept(n));

        HBox fila = new HBox(12, icono, textos, ver);
        fila.setAlignment(Pos.CENTER_LEFT);
        setGraphic(fila);
        setPrefWidth(0);
    }
}
