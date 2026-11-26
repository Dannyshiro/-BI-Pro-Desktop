package com.dy.bipro.service;

import com.dy.bipro.exception.DYNegocioException;
import com.dy.bipro.model.DYCategoria;
import com.dy.bipro.model.DYProveedor;
import com.dy.bipro.repository.DYCategoriaRepository;
import com.dy.bipro.repository.DYProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DYCategoriaService {
    private final DYCategoriaRepository repository;
    private final DYProductoRepository productoRepository;
    private final DYSesion sesion;

    public List<DYCategoria> listar() {
        return repository.findAllByOrderByNombreAsc();
    }

    public long contarProductos(DYCategoria c) {
        return productoRepository.countByCategoria(c);
    }

    @Transactional
    public DYCategoria crear(String nombre, DYProveedor proveedor) {
        sesion.exigirAdmin();
        if (nombre == null || nombre.isBlank()) throw new DYNegocioException("El nombre de la categoría es obligatorio.");
        if (repository.findByNombreIgnoreCase(nombre.trim()).isPresent())
            throw new DYNegocioException("Ya existe la categoría \"" + nombre.trim() + "\".");
        return repository.save(DYCategoria.builder().nombre(nombre.trim()).proveedor(proveedor).build());
    }

    @Transactional
    public DYCategoria asignarProveedor(DYCategoria c, DYProveedor p) {
        sesion.exigirAdmin();
        c.setProveedor(p);
        return repository.save(c);
    }

    @Transactional
    public void eliminar(DYCategoria c) {
        sesion.exigirAdmin();
        long n = productoRepository.countByCategoria(c);
        if (n > 0) throw new DYNegocioException("No se puede eliminar \"" + c.getNombre() + "\": tiene " + n
                + " producto(s) asociado(s). Reasígnalos antes de eliminarla.");
        repository.delete(c);
    }
}
