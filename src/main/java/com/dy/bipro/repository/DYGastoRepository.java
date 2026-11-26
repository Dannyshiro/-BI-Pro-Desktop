package com.dy.bipro.repository;

import com.dy.bipro.model.DYGasto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DYGastoRepository extends JpaRepository<DYGasto, Long> {
    List<DYGasto> findAllByOrderByFechaDescIdDesc();

    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM DYGasto g WHERE g.fecha BETWEEN :desde AND :hasta")
    Number totalEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT g.categoria, SUM(g.monto) FROM DYGasto g WHERE g.fecha BETWEEN :desde AND :hasta GROUP BY g.categoria")
    List<Object[]> porCategoria(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
