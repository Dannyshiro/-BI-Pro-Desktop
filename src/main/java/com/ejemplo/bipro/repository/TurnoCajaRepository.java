package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.EstadoCaja;
import com.ejemplo.bipro.model.TurnoCaja;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {
    Optional<TurnoCaja> findFirstByEstado(EstadoCaja estado);

    List<TurnoCaja> findTop30ByEstadoOrderByAperturaDesc(EstadoCaja estado);

    boolean existsByEstadoAndCierreBetween(EstadoCaja estado, LocalDateTime desde, LocalDateTime hasta);
}
