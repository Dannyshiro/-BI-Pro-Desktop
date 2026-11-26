package com.ejemplo.bipro.service;

import com.ejemplo.bipro.exception.NegocioException;
import com.ejemplo.bipro.model.Categoria;
import com.ejemplo.bipro.model.Proveedor;
import com.ejemplo.bipro.repository.CategoriaRepository;
import com.ejemplo.bipro.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaService {
    private final CategoriaRepository repository;
    private final ProductoRepository productoRepository;
    private final Sesion sesion;

    public List<Categoria> listar() {
        return repository.findAllByOrderByNombreAsc();
    }

    public long contarProductos(Categoria c) {
        return productoRepository.countByCategoria(c);
    }

    @Transactional
    public Categoria crear(String nombre, Proveedor proveedor) {
        sesion.exigirAdmin();
        if (nombre == null || nombre.isBlank()) throw new NegocioException("El nombre de la categoría es obligatorio.");
        if (repository.findByNombreIgnoreCase(nombre.trim()).isPresent())
            throw new NegocioException("Ya existe la categoría \"" + nombre.trim() + "\".");
        return repository.save(Categoria.builder().nombre(nombre.trim()).proveedor(proveedor).build());
    }

    @Transactional
    public Categoria asignarProveedor(Categoria c, Proveedor p) {
        sesion.exigirAdmin();
        c.setProveedor(p);
        return repository.save(c);
    }

    @Transactional
    public void eliminar(Categoria c) {
        sesion.exigirAdmin();
        long n = productoRepository.countByCategoria(c);
        if (n > 0) throw new NegocioException("No se puede eliminar \"" + c.getNombre() + "\": tiene " + n
                + " producto(s) asociado(s). Reasígnalos antes de eliminarla.");
        repository.delete(c);
    }
}
