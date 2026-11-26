package com.ejemplo.bipro;

import javafx.application.Application;

import java.nio.file.Files;
import java.nio.file.Path;

public class Launcher {
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of(System.getProperty("user.home"), ".bi-pro"));
        Application.launch(JavaFxApplication.class, args);
    }
}
