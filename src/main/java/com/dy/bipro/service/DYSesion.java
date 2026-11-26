package com.dy.bipro.service;

import com.dy.bipro.exception.DYNegocioException;
import com.dy.bipro.model.DYRol;
import com.dy.bipro.model.DYUsuario;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DYSesion {
    @Getter
    private DYUsuario usuario;
    private final Set<String> leidas = new HashSet<>();

    public void iniciar(DYUsuario usuario) {
        this.usuario = usuario;
        leidas.clear();
    }

    public void cerrar() {
        this.usuario = null;
    }

    public boolean esAdmin() {
        return usuario != null && usuario.getRol() == DYRol.ADMIN;
    }

    public void exigirAdmin() {
        if (!esAdmin()) {
            throw new DYNegocioException("Acción reservada para administradores. Tu rol no tiene este permiso.");
        }
    }

    public boolean leida(String idNotificacion) {
        return leidas.contains(idNotificacion);
    }

    public void marcarLeida(String idNotificacion) {
        leidas.add(idNotificacion);
    }
}
