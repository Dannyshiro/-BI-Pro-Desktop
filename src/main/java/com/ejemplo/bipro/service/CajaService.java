package com.ejemplo.bipro.service;

import com.ejemplo.bipro.dto.ResumenCaja;
import com.ejemplo.bipro.exception.NegocioException;
import com.ejemplo.bipro.model.*;
import com.ejemplo.bipro.repository.TurnoCajaRepository;
import com.ejemplo.bipro.repository.VentaRepository;
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
public class CajaService {
    private final TurnoCajaRepository repository;
    private final VentaRepository ventaRepository;
    private final Sesion sesion;

    public Optional<TurnoCaja> turnoAbierto() {
        return repository.findFirstByEstado(EstadoCaja.ABIERTA);
    }

    public boolean cerradaHoy() {
        LocalDate hoy = LocalDate.now();
        return repository.existsByEstadoAndCierreBetween(EstadoCaja.CERRADA, hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX));
    }

    @Transactional
    public TurnoCaja abrir(BigDecimal montoInicial) {
        if (montoInicial == null || montoInicial.signum() < 0)
            throw new NegocioException("El monto inicial debe ser mayor o igual a 0.");
        if (turnoAbierto().isPresent())
            throw new NegocioException("Ya existe una caja abierta. Ciérrala antes de abrir otra.");
        return repository.save(TurnoCaja.builder()
                .usuario(sesion.getUsuario()).apertura(LocalDateTime.now())
                .montoInicial(montoInicial).estado(EstadoCaja.ABIERTA).build());
    }

    public ResumenCaja resumen(TurnoCaja turno) {
        Map<MetodoPago, BigDecimal> montos = new EnumMap<>(MetodoPago.class);
        Map<MetodoPago, Long> conteo = new EnumMap<>(MetodoPago.class);
        BigDecimal total = BigDecimal.ZERO;
        long n = 0;
        for (Object[] fila : ventaRepository.resumenPorMetodo(turno, EstadoVenta.EMITIDA)) {
            MetodoPago m = (MetodoPago) fila[0];
            long cant = ((Number) fila[1]).longValue();
            BigDecimal monto = new BigDecimal(fila[2].toString());
            montos.put(m, monto);
            conteo.put(m, cant);
            total = total.add(monto);
            n += cant;
        }
        return new ResumenCaja(turno, total, n, montos, conteo);
    }

    @Transactional
    public TurnoCaja cerrar(BigDecimal efectivoContado) {
        TurnoCaja turno = turnoAbierto().orElseThrow(() -> new NegocioException("No hay una caja abierta."));
        if (efectivoContado == null || efectivoContado.signum() < 0)
            throw new NegocioException("Ingrese el efectivo contado (mayor o igual a 0).");
        ResumenCaja r = resumen(turno);
        turno.setCierre(LocalDateTime.now());
        turno.setTotalVentas(r.totalVentas());
        turno.setNumeroVentas((int) r.numeroVentas());
        turno.setEfectivoEsperado(r.efectivoEsperado());
        turno.setEfectivoContado(efectivoContado);
        turno.setDiferencia(efectivoContado.subtract(r.efectivoEsperado()));
        turno.setEstado(EstadoCaja.CERRADA);
        return repository.save(turno);
    }

    public List<TurnoCaja> historial() {
        return repository.findTop30ByEstadoOrderByAperturaDesc(EstadoCaja.CERRADA);
    }
}
