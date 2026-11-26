package com.dy.bipro;

import javafx.application.Application;

import java.nio.file.Files;
import java.nio.file.Path;

public class DYLauncher {
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of(System.getProperty("user.home"), ".bi-pro"));
        Application.launch(DYJavaFxApplication.class, args);
    }
}
