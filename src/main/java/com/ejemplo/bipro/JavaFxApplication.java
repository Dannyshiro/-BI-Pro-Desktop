package com.ejemplo.bipro;

import atlantafx.base.theme.PrimerLight;
import com.ejemplo.bipro.ui.ViewManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {
    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        this.context = new SpringApplicationBuilder(BiProApplication.class)
                .web(WebApplicationType.NONE)
                .run();
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        ViewManager vistas = context.getBean(ViewManager.class);
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
