package com.dy.bipro.repository;

import com.dy.bipro.model.DYEstadoCaja;
import com.dy.bipro.model.DYTurnoCaja;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYTurnoCajaRepository extends JpaRepository<DYTurnoCaja, Long> {
    Optional<DYTurnoCaja> findFirstByEstado(DYEstadoCaja estado);

    List<DYTurnoCaja> findTop30ByEstadoOrderByAperturaDesc(DYEstadoCaja estado);

    boolean existsByEstadoAndCierreBetween(DYEstadoCaja estado, LocalDateTime desde, LocalDateTime hasta);
}
