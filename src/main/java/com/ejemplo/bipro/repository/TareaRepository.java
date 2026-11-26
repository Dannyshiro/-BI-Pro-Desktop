package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.EstadoTarea;
import com.ejemplo.bipro.model.Tarea;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {
    List<Tarea> findByEstadoOrderByOrdenAscIdAsc(EstadoTarea estado);

    List<Tarea> findByEstadoNotAndFechaLimiteBefore(EstadoTarea estado, LocalDate fecha);
}
