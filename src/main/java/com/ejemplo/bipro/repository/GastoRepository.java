package com.ejemplo.bipro.repository;

import com.ejemplo.bipro.model.Gasto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {
    List<Gasto> findAllByOrderByFechaDescIdDesc();

    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM Gasto g WHERE g.fecha BETWEEN :desde AND :hasta")
    Number totalEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT g.categoria, SUM(g.monto) FROM Gasto g WHERE g.fecha BETWEEN :desde AND :hasta GROUP BY g.categoria")
    List<Object[]> porCategoria(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
