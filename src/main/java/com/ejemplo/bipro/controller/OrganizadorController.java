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
import javafx.scene.input.*;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class OrganizadorController {
    private final TareaService tareaService;
    private final FormDialogos formDialogos;
    private final Sesion sesion;
    private final EstadoApp estado;

    @FXML private VBox raiz;
    private final HBox tablero = new HBox(16);

    @FXML
    public void initialize() {
        Button nueva = Ui.boton("Nueva tarea", "fth-plus", "accent");
        nueva.setOnAction(e -> { if (formDialogos.tarea(null)) pintar(); });
        HBox barra = new HBox(12, Ui.texto("Tablero de tareas", "h3"),
                Ui.texto("Arrastra las tarjetas entre columnas · doble clic para editar", "caption"), Ui.espaciador(), nueva);
        barra.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(tablero, Priority.ALWAYS);
        raiz.getChildren().addAll(barra, tablero);
        pintar();
    }

    private void pintar() {
        tablero.getChildren().clear();
        for (EstadoTarea et : EstadoTarea.values()) {
            ListView<Tarea> lv = new ListView<>(FXCollections.observableArrayList(tareaService.porEstado(et)));
            lv.getStyleClass().add("lista-plana");
            lv.setCellFactory(l -> celda());
            lv.setOnDragOver(e -> { if (e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.MOVE); e.consume(); });
            lv.setOnDragDropped(e -> {
                boolean ok = false;
                if (e.getDragboard().hasString()) {
                    tareaService.mover(Long.parseLong(e.getDragboard().getString()), et, lv.getItems().size());
                    ok = true;
                }
                e.setDropCompleted(ok);
                e.consume();
                if (ok) { estado.notificarCambio(); pintar(); }
            });
            Label titulo = Ui.texto(et.getEtiqueta() + "  ·  " + lv.getItems().size(), "h3");
            VBox col = new VBox(10, titulo, lv);
            col.getStyleClass().add("kanban-col");
            VBox.setVgrow(lv, Priority.ALWAYS);
            HBox.setHgrow(col, Priority.ALWAYS);
            col.setMaxWidth(Double.MAX_VALUE);
            col.setPrefWidth(0);
            tablero.getChildren().add(col);
        }
    }

    private ListCell<Tarea> celda() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Tarea t, boolean empty) {
                super.updateItem(t, empty);
                setText(null);
                if (empty || t == null) { setGraphic(null); setOnDragDetected(null); setOnMouseClicked(null); return; }
                HBox meta = new HBox(8, Ui.badge(t.getPrioridad().getEtiqueta(), t.getPrioridad().getEstilo()));
                if (t.getFechaLimite() != null)
                    meta.getChildren().add(Ui.texto((t.isVencida() ? "Vencida · " : "Vence ") + Formato.fecha(t.getFechaLimite()),
                            t.isVencida() ? "negativo" : "caption"));
                meta.getChildren().add(Ui.espaciador());
                if (t.getResponsable() != null) {
                    Label av = new Label(t.getResponsable().getIniciales());
                    av.getStyleClass().add("avatar");
                    meta.getChildren().add(av);
                }
                VBox card = new VBox(8, Ui.texto(t.getTitulo(), "dato-valor"), meta);
                card.getStyleClass().add("tarea-card");
                setGraphic(card);
                setOnDragDetected(e -> {
                    Dragboard db = startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(String.valueOf(t.getId()));
                    db.setContent(cc);
                    e.consume();
                });
                setOnMouseClicked(e -> { if (e.getClickCount() == 2 && formDialogos.tarea(t)) pintar(); });
                ContextMenu cm = new ContextMenu();
                MenuItem ed = new MenuItem("Editar");
                ed.setOnAction(e -> { if (formDialogos.tarea(t)) pintar(); });
                cm.getItems().add(ed);
                if (sesion.esAdmin()) {
                    MenuItem del = new MenuItem("Eliminar");
                    del.setOnAction(e -> { tareaService.eliminar(t); estado.notificarCambio(); pintar(); });
                    cm.getItems().add(del);
                }
                setContextMenu(cm);
            }
        };
    }
}
