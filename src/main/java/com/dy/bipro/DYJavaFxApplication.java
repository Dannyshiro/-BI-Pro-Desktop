package com.dy.bipro;

import atlantafx.base.theme.PrimerLight;
import com.dy.bipro.ui.DYViewManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class DYJavaFxApplication extends Application {
    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        this.context = new SpringApplicationBuilder(DYBiProApplication.class)
                .web(WebApplicationType.NONE)
                .run();
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        DYViewManager vistas = context.getBean(DYViewManager.class);
        vistas.setStage(stage);

        stage.setTitle("BI Pro | Business Intelligence");
        stage.setMinWidth(1200);
        stage.setMinHeight(720);
        vistas.mostrarLogin();
        stage.show();
    }

    @Override
    public void stop() {
        context.close();
        Platform.exit();
    }
}
