package com.dy.bipro.service;

import com.dy.bipro.dto.DYResumenCaja;
import com.dy.bipro.exception.DYNegocioException;
import com.dy.bipro.model.*;
import com.dy.bipro.repository.DYTurnoCajaRepository;
import com.dy.bipro.repository.DYVentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DYCajaService {
    private final DYTurnoCajaRepository repository;
    private final DYVentaRepository ventaRepository;
    private final DYSesion sesion;

    public Optional<DYTurnoCaja> turnoAbierto() {
        return repository.findFirstByEstado(DYEstadoCaja.ABIERTA);
    }

    public boolean cerradaHoy() {
        LocalDate hoy = LocalDate.now();
        return repository.existsByEstadoAndCierreBetween(DYEstadoCaja.CERRADA, hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX));
    }

    @Transactional
    public DYTurnoCaja abrir(BigDecimal montoInicial) {
        if (montoInicial == null || montoInicial.signum() < 0)
            throw new DYNegocioException("El monto inicial debe ser mayor o igual a 0.");
        if (turnoAbierto().isPresent())
            throw new DYNegocioException("Ya existe una caja abierta. Ciérrala antes de abrir otra.");
        return repository.save(DYTurnoCaja.builder()
                .usuario(sesion.getUsuario()).apertura(LocalDateTime.now())
                .montoInicial(montoInicial).estado(DYEstadoCaja.ABIERTA).build());
    }

    public DYResumenCaja resumen(DYTurnoCaja turno) {
        Map<DYMetodoPago, BigDecimal> montos = new EnumMap<>(DYMetodoPago.class);
        Map<DYMetodoPago, Long> conteo = new EnumMap<>(DYMetodoPago.class);
        BigDecimal total = BigDecimal.ZERO;
        long n = 0;
        for (Object[] fila : ventaRepository.resumenPorMetodo(turno, DYEstadoVenta.EMITIDA)) {
            DYMetodoPago m = (DYMetodoPago) fila[0];
            long cant = ((Number) fila[1]).longValue();
            BigDecimal monto = new BigDecimal(fila[2].toString());
            montos.put(m, monto);
            conteo.put(m, cant);
            total = total.add(monto);
            n += cant;
        }
        return new DYResumenCaja(turno, total, n, montos, conteo);
    }

    @Transactional
    public DYTurnoCaja cerrar(BigDecimal efectivoContado) {
        DYTurnoCaja turno = turnoAbierto().orElseThrow(() -> new DYNegocioException("No hay una caja abierta."));
        if (efectivoContado == null || efectivoContado.signum() < 0)
            throw new DYNegocioException("Ingrese el efectivo contado (mayor o igual a 0).");
        DYResumenCaja r = resumen(turno);
        turno.setCierre(LocalDateTime.now());
        turno.setTotalVentas(r.totalVentas());
        turno.setNumeroVentas((int) r.numeroVentas());
        turno.setEfectivoEsperado(r.efectivoEsperado());
        turno.setEfectivoContado(efectivoContado);
        turno.setDiferencia(efectivoContado.subtract(r.efectivoEsperado()));
        turno.setEstado(DYEstadoCaja.CERRADA);
        return repository.save(turno);
    }

    public List<DYTurnoCaja> historial() {
        return repository.findTop30ByEstadoOrderByAperturaDesc(DYEstadoCaja.CERRADA);
    }
}
