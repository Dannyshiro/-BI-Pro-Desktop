package com.ejemplo.bipro.controller;

import com.ejemplo.bipro.exception.NegocioException;
import com.ejemplo.bipro.model.Empresa;
import com.ejemplo.bipro.model.Usuario;
import com.ejemplo.bipro.service.AuthService;
import com.ejemplo.bipro.service.EmpresaService;
import com.ejemplo.bipro.service.UsuarioService;
import com.ejemplo.bipro.ui.Ui;
import com.ejemplo.bipro.ui.ViewManager;
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
public class LoginController {
    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final EmpresaService empresaService;
    private final ViewManager vistas;

    @FXML private Label lblEmpresa;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtClave;
    @FXML private Label lblError;
    @FXML private VBox boxDemo;

    @FXML
    public void initialize() {
        Empresa e = empresaService.obtener();
        lblEmpresa.setText(e.getRazonSocial() + " · RUC " + e.getRuc());

        for (Usuario u : usuarioService.listar()) {
            if (!u.isActivo()) continue;
            Label avatar = new Label(u.getIniciales());
            avatar.getStyleClass().addAll("avatar", "avatar-claro");
            HBox fila = new HBox(10, avatar, Ui.texto(u.getNombre(), "demo-nombre"), Ui.espaciador(),
                    Ui.texto(u.getRol().getEtiqueta(), "caption"));
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
        } catch (NegocioException e) {
            lblError.setText(e.getMessage());
            txtClave.clear();
        }
    }
}
