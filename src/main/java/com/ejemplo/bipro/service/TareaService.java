package com.ejemplo.bipro.service;

import com.ejemplo.bipro.exception.NegocioException;
import com.ejemplo.bipro.model.EstadoTarea;
import com.ejemplo.bipro.model.Tarea;
import com.ejemplo.bipro.repository.TareaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TareaService {
    private final TareaRepository repository;
    private final Sesion sesion;

    public List<Tarea> porEstado(EstadoTarea estado) {
        return repository.findByEstadoOrderByOrdenAscIdAsc(estado);
    }

    public List<Tarea> vencidas() {
        return repository.findByEstadoNotAndFechaLimiteBefore(EstadoTarea.HECHO, LocalDate.now());
    }

    @Transactional
    public Tarea guardar(@Valid Tarea tarea) {
        if (tarea.getFechaLimite() != null && tarea.getEstado() != EstadoTarea.HECHO
                && tarea.getFechaLimite().isBefore(LocalDate.now()))
            throw new NegocioException("La fecha límite no puede ser anterior a hoy para una tarea abierta.");
        if (tarea.getId() == null) tarea.setOrden(repository.findByEstadoOrderByOrdenAscIdAsc(tarea.getEstado()).size());
        return repository.save(tarea);
    }

    @Transactional
    public void mover(Long id, EstadoTarea destino, int indice) {
        Tarea t = repository.findById(id).orElseThrow();
        List<Tarea> columna = new ArrayList<>(repository.findByEstadoOrderByOrdenAscIdAsc(destino));
        columna.removeIf(x -> x.getId().equals(id));
        t.setEstado(destino);
        columna.add(Math.max(0, Math.min(indice, columna.size())), t);
        for (int i = 0; i < columna.size(); i++) columna.get(i).setOrden(i);
        repository.saveAll(columna);
    }

    @Transactional
    public void eliminar(Tarea tarea) {
        sesion.exigirAdmin();
        repository.delete(tarea);
    }
}
