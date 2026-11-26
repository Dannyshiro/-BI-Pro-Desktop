package com.ejemplo.bipro.service;

import com.ejemplo.bipro.exception.NegocioException;
import com.ejemplo.bipro.model.Rol;
import com.ejemplo.bipro.model.Usuario;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class Sesion {
    @Getter
    private Usuario usuario;
    private final Set<String> leidas = new HashSet<>();

    public void iniciar(Usuario usuario) {
        this.usuario = usuario;
        leidas.clear();
    }

    public void cerrar() {
        this.usuario = null;
    }

    public boolean esAdmin() {
        return usuario != null && usuario.getRol() == Rol.ADMIN;
    }

    public void exigirAdmin() {
        if (!esAdmin()) {
            throw new NegocioException("Acción reservada para administradores. Tu rol no tiene este permiso.");
        }
    }

    public boolean leida(String idNotificacion) {
        return leidas.contains(idNotificacion);
    }

    public void marcarLeida(String idNotificacion) {
        leidas.add(idNotificacion);
    }
}
