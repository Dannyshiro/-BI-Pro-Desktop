package com.dy.bipro.service;

import com.dy.bipro.exception.DYNegocioException;
import com.dy.bipro.model.DYEstadoTarea;
import com.dy.bipro.model.DYTarea;
import com.dy.bipro.repository.DYTareaRepository;
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
public class DYTareaService {
    private final DYTareaRepository repository;
    private final DYSesion sesion;

    public List<DYTarea> porEstado(DYEstadoTarea estado) {
        return repository.findByEstadoOrderByOrdenAscIdAsc(estado);
    }

    public List<DYTarea> vencidas() {
        return repository.findByEstadoNotAndFechaLimiteBefore(DYEstadoTarea.HECHO, LocalDate.now());
    }

    @Transactional
    public DYTarea guardar(@Valid DYTarea tarea) {
        if (tarea.getFechaLimite() != null && tarea.getEstado() != DYEstadoTarea.HECHO
                && tarea.getFechaLimite().isBefore(LocalDate.now()))
            throw new DYNegocioException("La fecha límite no puede ser anterior a hoy para una tarea abierta.");
        if (tarea.getId() == null) tarea.setOrden(repository.findByEstadoOrderByOrdenAscIdAsc(tarea.getEstado()).size());
        return repository.save(tarea);
    }

    @Transactional
    public void mover(Long id, DYEstadoTarea destino, int indice) {
        DYTarea t = repository.findById(id).orElseThrow();
        List<DYTarea> columna = new ArrayList<>(repository.findByEstadoOrderByOrdenAscIdAsc(destino));
        columna.removeIf(x -> x.getId().equals(id));
        t.setEstado(destino);
        columna.add(Math.max(0, Math.min(indice, columna.size())), t);
        for (int i = 0; i < columna.size(); i++) columna.get(i).setOrden(i);
        repository.saveAll(columna);
    }

    @Transactional
    public void eliminar(DYTarea tarea) {
        sesion.exigirAdmin();
        repository.delete(tarea);
    }
}
