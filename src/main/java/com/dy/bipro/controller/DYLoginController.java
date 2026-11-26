package com.dy.bipro.controller;

import com.dy.bipro.exception.DYNegocioException;
import com.dy.bipro.model.DYEmpresa;
import com.dy.bipro.model.DYUsuario;
import com.dy.bipro.service.DYAuthService;
import com.dy.bipro.service.DYEmpresaService;
import com.dy.bipro.service.DYUsuarioService;
import com.dy.bipro.ui.DYUi;
import com.dy.bipro.ui.DYViewManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DYLoginController {
    private final DYAuthService authService;
    private final DYUsuarioService usuarioService;
    private final DYEmpresaService empresaService;
    private final DYViewManager vistas;

    @FXML private Label lblEmpresa;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtClave;
    @FXML private Label lblError;
    @FXML private VBox boxDemo;

    @FXML
    public void initialize() {
        DYEmpresa e = empresaService.obtener();
        lblEmpresa.setText(e.getRazonSocial() + " · RUC " + e.getRuc());

        for (DYUsuario u : usuarioService.listar()) {
            if (!u.isActivo()) continue;
            Label avatar = new Label(u.getIniciales());
            avatar.getStyleClass().addAll("avatar", "avatar-claro");
            HBox fila = new HBox(10, avatar, DYUi.texto(u.getNombre(), "demo-nombre"), DYUi.espaciador(),
                    DYUi.texto(u.getRol().getEtiqueta(), "caption"));
            fila.setAlignment(Pos.CENTER_LEFT);
            Button b = new Button(null, fila);
            b.getStyleClass().add("demo-usuario");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(ev -> {
                txtUsuario.setText(u.getUsuario());
                txtClave.setText("1234");
                lblError.setText("");
            });
            boxDemo.getChildren().add(b);
        }
    }

    @FXML
    private void onIngresar() {
        try {
            authService.login(txtUsuario.getText(), txtClave.getText());
            vistas.mostrarPrincipal();
        } catch (DYNegocioException e) {
            lblError.setText(e.getMessage());
            txtClave.clear();
        }
    }
}
