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
public class AsistenteController {
    private final AsistenteService asistente;
    private final EstadoApp estado;

    @FXML private VBox raiz;
    private final VBox chat = new VBox(12);
    private final TextField txt = new TextField();

    @FXML
    public void initialize() {
        ScrollPane sp = new ScrollPane(chat);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-contenido");
        chat.setPadding(new javafx.geometry.Insets(8));
        VBox.setVgrow(sp, Priority.ALWAYS);

        HBox sugerencias = new HBox(8);
        for (String s : List.of("¿Qué debo reponer?", "¿Cómo van las ventas?", "Resumen de gastos", "Clientes a contactar", "Tareas pendientes", "Estado de la caja")) {
            Button b = Ui.boton(s, null, "chip");
            b.setOnAction(e -> preguntar(s, sp));
            sugerencias.getChildren().add(b);
        }
        txt.setPromptText("Pregunta sobre inventario, ventas, gastos, clientes, caja o tareas…");
        HBox.setHgrow(txt, Priority.ALWAYS);
        txt.setOnAction(e -> preguntar(txt.getText(), sp));
        Button enviar = Ui.boton("Enviar", "fth-send", "accent");
        enviar.setOnAction(e -> preguntar(txt.getText(), sp));
        raiz.getChildren().addAll(sp, sugerencias, new HBox(10, txt, enviar));
        burbuja("Hola, soy tu asistente de negocio. Respondo con los datos actuales de tu sistema. ¿Qué quieres saber?", false);
    }

    private void preguntar(String q, ScrollPane sp) {
        if (q == null || q.isBlank()) return;
        txt.clear();
        burbuja(q, true);
        Respuesta r = asistente.responder(q);
        VBox b = new VBox(8, Ui.texto(r.texto(), "dato-valor"));
        r.puntos().forEach(p -> b.getChildren().add(Ui.texto("•  " + p, "caption")));
        if (r.modulo() != null) {
            Button ir = Ui.boton("Ir a " + Modulo.valueOf(r.modulo()).getTitulo(), "fth-arrow-right", "flat", "enlace");
            ir.setOnAction(e -> estado.ir(Modulo.valueOf(r.modulo())));
            b.getChildren().add(ir);
        }
        b.getStyleClass().add("mensaje-bot");
        b.setMaxWidth(620);
        chat.getChildren().add(b);
        sp.layout();
        sp.setVvalue(1);
    }

    private void burbuja(String t, boolean usuario) {
        Label l = Ui.texto(t, usuario ? "mensaje-user" : "mensaje-bot");
        l.setMaxWidth(620);
        HBox h = new HBox(l);
        h.setAlignment(usuario ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chat.getChildren().add(h);
    }
}
