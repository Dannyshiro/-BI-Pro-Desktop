package com.ejemplo.bipro.service;

import com.ejemplo.bipro.exception.NegocioException;
import com.ejemplo.bipro.model.Cliente;
import com.ejemplo.bipro.model.EstadoVenta;
import com.ejemplo.bipro.model.Segmento;
import com.ejemplo.bipro.model.Venta;
import com.ejemplo.bipro.repository.ClienteRepository;
import com.ejemplo.bipro.repository.VentaRepository;
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
public class ClienteService {
    private final ClienteRepository repository;
    private final VentaRepository ventaRepository;
    private final Sesion sesion;

    public List<Cliente> listar() {
        return repository.findAllByOrderByNombreAsc();
    }

    public List<Cliente> buscar(String texto, Segmento segmento) {
        String t = texto == null ? "" : texto.trim().toLowerCase();
        return repository.findAllByOrderByNombreAsc().stream()
                .filter(c -> t.isEmpty() || c.getNombre().toLowerCase().contains(t) || c.getDocumento().contains(t))
                .filter(c -> segmento == null || c.getSegmento() == segmento)
                .toList();
    }

    @Transactional
    public Cliente guardar(@Valid Cliente cliente) {
        repository.findByDocumento(cliente.getDocumento()).ifPresent(existente -> {
            if (cliente.getId() == null || !existente.getId().equals(cliente.getId()))
                throw new NegocioException("Ya existe un cliente con documento " + cliente.getDocumento() + ".");
        });
        return repository.save(cliente);
    }

    @Transactional
    public void eliminar(Cliente cliente) {
        sesion.exigirAdmin();
        if (ventaRepository.existsByClienteAndEstado(cliente, EstadoVenta.EMITIDA))
            throw new NegocioException("No se puede eliminar a " + cliente.getNombre()
                    + ": tiene ventas registradas y se conserva para el historial.");
        repository.delete(cliente);
    }

    public List<Venta> historial(Cliente cliente) {
        return ventaRepository.findByClienteOrderByFechaDesc(cliente);
    }
}
