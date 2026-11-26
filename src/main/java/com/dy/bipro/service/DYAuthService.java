package com.dy.bipro.service;

import com.dy.bipro.exception.DYNegocioException;
import com.dy.bipro.model.DYUsuario;
import com.dy.bipro.repository.DYUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DYAuthService {
    private final DYUsuarioRepository repository;
    private final DYSesion sesion;

    public DYUsuario login(String usuario, String clave) {
        if (usuario == null || usuario.isBlank()) throw new DYNegocioException("Ingrese su usuario");
        if (clave == null || clave.isBlank()) throw new DYNegocioException("Ingrese su contraseña");

        DYUsuario u = repository.findByUsuarioIgnoreCase(usuario.trim())
                .orElseThrow(() -> new DYNegocioException("Usuario no registrado"));
        if (!u.isActivo()) throw new DYNegocioException("Usuario desactivado. Contacte al administrador.");
        if (!u.getPasswordHash().equals(hash(clave))) throw new DYNegocioException("Contraseña incorrecta");

        sesion.iniciar(u);
        return u;
    }

    public static String hash(String clave) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(clave.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
