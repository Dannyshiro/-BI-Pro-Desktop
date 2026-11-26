package com.dy.bipro.service;

import com.dy.bipro.exception.DYNegocioException;
import com.dy.bipro.model.DYRol;
import com.dy.bipro.model.DYUsuario;
import com.dy.bipro.repository.DYUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DYUsuarioService {
    private final DYUsuarioRepository repository;
    private final DYSesion sesion;

    public List<DYUsuario> listar() {
        return repository.findAllByOrderByNombreAsc();
    }

    @Transactional
    public DYUsuario crear(String usuario, String nombre, String clave, DYRol rol) {
        sesion.exigirAdmin();
        if (usuario == null || !usuario.trim().matches("[a-zA-Z0-9._]{3,30}"))
            throw new DYNegocioException("El usuario debe tener entre 3 y 30 caracteres (letras, números, punto o guion bajo).");
        if (nombre == null || nombre.isBlank()) throw new DYNegocioException("El nombre es obligatorio.");
        if (clave == null || clave.length() < 4) throw new DYNegocioException("La clave debe tener al menos 4 caracteres.");
        if (rol == null) throw new DYNegocioException("Seleccione un rol.");
        if (repository.existsByUsuarioIgnoreCase(usuario.trim()))
            throw new DYNegocioException("Ya existe el usuario " + usuario.trim() + ".");
        return repository.save(DYUsuario.builder()
                .usuario(usuario.trim().toLowerCase()).nombre(nombre.trim())
                .passwordHash(DYAuthService.hash(clave)).rol(rol).activo(true).build());
    }

    @Transactional
    public DYUsuario cambiarRol(Long id, DYRol nuevoRol) {
        sesion.exigirAdmin();
        DYUsuario u = repository.findById(id).orElseThrow();
        if (u.getRol() == nuevoRol) return u;
        if (u.getId().equals(sesion.getUsuario().getId()))
            throw new DYNegocioException("No puedes cambiar tu propio rol. Pide a otro administrador que lo haga.");
        if (u.getRol() == DYRol.ADMIN && u.isActivo() && repository.countByRolAndActivoTrue(DYRol.ADMIN) <= 1)
            throw new DYNegocioException("Debe existir al menos un administrador activo.");
        u.setRol(nuevoRol);
        return repository.save(u);
    }

    @Transactional
    public DYUsuario alternarActivo(Long id) {
        sesion.exigirAdmin();
        DYUsuario u = repository.findById(id).orElseThrow();
        if (u.getId().equals(sesion.getUsuario().getId()))
            throw new DYNegocioException("No puedes desactivar tu propio usuario.");
        if (u.isActivo() && u.getRol() == DYRol.ADMIN && repository.countByRolAndActivoTrue(DYRol.ADMIN) <= 1)
            throw new DYNegocioException("Debe existir al menos un administrador activo.");
        u.setActivo(!u.isActivo());
        return repository.save(u);
    }
}
