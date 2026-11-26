package com.ejemplo.bipro.service;

import com.ejemplo.bipro.exception.NegocioException;
import com.ejemplo.bipro.model.Rol;
import com.ejemplo.bipro.model.Usuario;
import com.ejemplo.bipro.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository repository;
    private final Sesion sesion;

    public List<Usuario> listar() {
        return repository.findAllByOrderByNombreAsc();
    }

    @Transactional
    public Usuario crear(String usuario, String nombre, String clave, Rol rol) {
        sesion.exigirAdmin();
        if (usuario == null || !usuario.trim().matches("[a-zA-Z0-9._]{3,30}"))
            throw new NegocioException("El usuario debe tener entre 3 y 30 caracteres (letras, números, punto o guion bajo).");
        if (nombre == null || nombre.isBlank()) throw new NegocioException("El nombre es obligatorio.");
        if (clave == null || clave.length() < 4) throw new NegocioException("La clave debe tener al menos 4 caracteres.");
        if (rol == null) throw new NegocioException("Seleccione un rol.");
        if (repository.existsByUsuarioIgnoreCase(usuario.trim()))
            throw new NegocioException("Ya existe el usuario " + usuario.trim() + ".");
        return repository.save(Usuario.builder()
                .usuario(usuario.trim().toLowerCase()).nombre(nombre.trim())
                .passwordHash(AuthService.hash(clave)).rol(rol).activo(true).build());
    }

    @Transactional
    public Usuario cambiarRol(Long id, Rol nuevoRol) {
        sesion.exigirAdmin();
        Usuario u = repository.findById(id).orElseThrow();
        if (u.getRol() == nuevoRol) return u;
        if (u.getId().equals(sesion.getUsuario().getId()))
            throw new NegocioException("No puedes cambiar tu propio rol. Pide a otro administrador que lo haga.");
        if (u.getRol() == Rol.ADMIN && u.isActivo() && repository.countByRolAndActivoTrue(Rol.ADMIN) <= 1)
            throw new NegocioException("Debe existir al menos un administrador activo.");
        u.setRol(nuevoRol);
        return repository.save(u);
    }

    @Transactional
    public Usuario alternarActivo(Long id) {
        sesion.exigirAdmin();
        Usuario u = repository.findById(id).orElseThrow();
        if (u.getId().equals(sesion.getUsuario().getId()))
            throw new NegocioException("No puedes desactivar tu propio usuario.");
        if (u.isActivo() && u.getRol() == Rol.ADMIN && repository.countByRolAndActivoTrue(Rol.ADMIN) <= 1)
            throw new NegocioException("Debe existir al menos un administrador activo.");
        u.setActivo(!u.isActivo());
        return repository.save(u);
    }
}
