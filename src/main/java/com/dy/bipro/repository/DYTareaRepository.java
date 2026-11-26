package com.dy.bipro.repository;

import com.dy.bipro.model.DYEstadoTarea;
import com.dy.bipro.model.DYTarea;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYTareaRepository extends JpaRepository<DYTarea, Long> {
    List<DYTarea> findByEstadoOrderByOrdenAscIdAsc(DYEstadoTarea estado);

    List<DYTarea> findByEstadoNotAndFechaLimiteBefore(DYEstadoTarea estado, LocalDate fecha);
}
