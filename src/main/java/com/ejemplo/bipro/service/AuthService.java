package com.ejemplo.bipro.service;

import com.ejemplo.bipro.exception.NegocioException;
import com.ejemplo.bipro.model.Usuario;
import com.ejemplo.bipro.repository.UsuarioRepository;
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
public class AuthService {
    private final UsuarioRepository repository;
    private final Sesion sesion;

    public Usuario login(String usuario, String clave) {
        if (usuario == null || usuario.isBlank()) throw new NegocioException("Ingrese su usuario");
        if (clave == null || clave.isBlank()) throw new NegocioException("Ingrese su contraseña");

        Usuario u = repository.findByUsuarioIgnoreCase(usuario.trim())
                .orElseThrow(() -> new NegocioException("Usuario no registrado"));
        if (!u.isActivo()) throw new NegocioException("Usuario desactivado. Contacte al administrador.");
        if (!u.getPasswordHash().equals(hash(clave))) throw new NegocioException("Contraseña incorrecta");

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
