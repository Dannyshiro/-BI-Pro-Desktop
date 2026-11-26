package com.dy.bipro.service;

import com.dy.bipro.exception.DYNegocioException;
import com.dy.bipro.model.DYCliente;
import com.dy.bipro.model.DYEstadoVenta;
import com.dy.bipro.model.DYSegmento;
import com.dy.bipro.model.DYVenta;
import com.dy.bipro.repository.DYClienteRepository;
import com.dy.bipro.repository.DYVentaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DYClienteService {
    private final DYClienteRepository repository;
    private final DYVentaRepository ventaRepository;
    private final DYSesion sesion;

    public List<DYCliente> listar() {
        return repository.findAllByOrderByNombreAsc();
    }

    public List<DYCliente> buscar(String texto, DYSegmento segmento) {
        String t = texto == null ? "" : texto.trim().toLowerCase();
        return repository.findAllByOrderByNombreAsc().stream()
                .filter(c -> t.isEmpty() || c.getNombre().toLowerCase().contains(t) || c.getDocumento().contains(t))
                .filter(c -> segmento == null || c.getSegmento() == segmento)
                .toList();
    }

    @Transactional
    public DYCliente guardar(@Valid DYCliente cliente) {
        repository.findByDocumento(cliente.getDocumento()).ifPresent(existente -> {
            if (cliente.getId() == null || !existente.getId().equals(cliente.getId()))
                throw new DYNegocioException("Ya existe un cliente con documento " + cliente.getDocumento() + ".");
        });
        return repository.save(cliente);
    }

    @Transactional
    public void eliminar(DYCliente cliente) {
        sesion.exigirAdmin();
        if (ventaRepository.existsByClienteAndEstado(cliente, DYEstadoVenta.EMITIDA))
            throw new DYNegocioException("No se puede eliminar a " + cliente.getNombre()
                    + ": tiene ventas registradas y se conserva para el historial.");
        repository.delete(cliente);
    }

    public List<DYVenta> historial(DYCliente cliente) {
        return ventaRepository.findByClienteOrderByFechaDesc(cliente);
    }
}
