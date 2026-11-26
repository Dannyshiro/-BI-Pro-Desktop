package com.ejemplo.bipro.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ViewManager {
    private final ApplicationContext context;

    @Getter
    private Stage stage;
    private String css;

    public void setStage(Stage stage) {
        this.stage = stage;
        this.css = Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm();
        Ui.configurar(stage, css);
    }

    public <T> Vista<T> cargar(String nombre) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/" + nombre + ".fxml"));

        loader.setControllerFactory(context::getBean);
        try {
            Parent root = loader.load();
            return new Vista<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + nombre, e);
        }
    }

    public void mostrarLogin() {
        mostrar(cargar("login-view").root());
        stage.setMaximized(false);
    }

    public void mostrarPrincipal() {
        mostrar(cargar("main-view").root());
        stage.setMaximized(true);
    }

    private void mostrar(Parent root) {
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(css);
        stage.setScene(scene);
    }

    public record Vista<T>(Parent root, T controller) {
    }
}
