package com.ejemplo.bipro.service;

import com.ejemplo.bipro.dto.Intervalo;
import com.ejemplo.bipro.dto.Kpis;
import com.ejemplo.bipro.dto.MesResumen;
import com.ejemplo.bipro.model.CategoriaGasto;
import com.ejemplo.bipro.model.EstadoVenta;
import com.ejemplo.bipro.repository.DetalleVentaRepository;
import com.ejemplo.bipro.repository.GastoRepository;
import com.ejemplo.bipro.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {
    private static final EstadoVenta EMITIDA = EstadoVenta.EMITIDA;

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleRepository;
    private final GastoRepository gastoRepository;

    public Kpis kpis(Intervalo i) {
        BigDecimal ventas = bd(ventaRepository.totalEntre(EMITIDA, i.inicio(), i.fin()));
        BigDecimal costo = bd(detalleRepository.costoEntre(EMITIDA, i.inicio(), i.fin()));
        long transacciones = ventaRepository.contarEntre(EMITIDA, i.inicio(), i.fin());
        BigDecimal gastos = bd(gastoRepository.totalEntre(i.desde(), i.hasta()));
        return new Kpis(ventas, costo, transacciones, gastos);
    }

    public Map<LocalDate, BigDecimal> serieDiaria(Intervalo i) {
        Map<LocalDate, BigDecimal> serie = new TreeMap<>();
        for (LocalDate d = i.desde(); !d.isAfter(i.hasta()); d = d.plusDays(1)) serie.put(d, BigDecimal.ZERO);
        for (Object[] fila : ventaRepository.fechasYTotales(EMITIDA, i.inicio(), i.fin())) {
            serie.merge(((LocalDateTime) fila[0]).toLocalDate(), bd(fila[1]), BigDecimal::add);
        }
        return serie;
    }

    public Map<Integer, BigDecimal> serieHoraria(LocalDate dia) {
        Map<Integer, BigDecimal> serie = new TreeMap<>();
        for (int h = 8; h <= 21; h++) serie.put(h, BigDecimal.ZERO);
        Intervalo i = Intervalo.dia(dia);
        for (Object[] fila : ventaRepository.fechasYTotales(EMITIDA, i.inicio(), i.fin())) {
            int h = Math.max(8, Math.min(21, ((LocalDateTime) fila[0]).getHour()));
            serie.merge(h, bd(fila[1]), BigDecimal::add);
        }
        return serie;
    }

    public Map<DayOfWeek, BigDecimal> promedioPorDiaSemana(Intervalo i) {
        Map<DayOfWeek, BigDecimal> suma = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, Integer> dias = new EnumMap<>(DayOfWeek.class);
        serieDiaria(i).forEach((d, v) -> {
            suma.merge(d.getDayOfWeek(), v, BigDecimal::add);
            dias.merge(d.getDayOfWeek(), 1, Integer::sum);
        });
        Map<DayOfWeek, BigDecimal> promedio = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            int n = dias.getOrDefault(d, 0);
            promedio.put(d, n == 0 ? BigDecimal.ZERO
                    : suma.get(d).divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP));
        }
        return promedio;
    }

    public Map<String, BigDecimal> ventasPorCategoria(Intervalo i) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        ventaPorCategoriaOrdenada(i).forEach(f -> m.put((String) f[0], bd(f[1])));
        return m;
    }

    private List<Object[]> ventaPorCategoriaOrdenada(Intervalo i) {
        List<Object[]> filas = new ArrayList<>(detalleRepository.ventasPorCategoria(EMITIDA, i.inicio(), i.fin()));
        filas.sort((a, b) -> bd(b[1]).compareTo(bd(a[1])));
        return filas;
    }

    public Map<CategoriaGasto, BigDecimal> gastosPorCategoria(Intervalo i) {
        Map<CategoriaGasto, BigDecimal> m = new EnumMap<>(CategoriaGasto.class);
        for (Object[] f : gastoRepository.porCategoria(i.desde(), i.hasta())) m.put((CategoriaGasto) f[0], bd(f[1]));
        return m;
    }

    public List<MesResumen> ultimosMeses(int n) {
        List<MesResumen> lista = new ArrayList<>();
        YearMonth actual = YearMonth.now();
        for (int k = n - 1; k >= 0; k--) {
            YearMonth m = actual.minusMonths(k);
            LocalDate hasta = k == 0 ? LocalDate.now() : m.atEndOfMonth();
            Kpis kp = kpis(new Intervalo(m.atDay(1), hasta));
            lista.add(new MesResumen(m, kp.ventas(), kp.costo().add(kp.gastos())));
        }
        return lista;
    }

    public static BigDecimal bd(Object valor) {
        if (valor == null) return BigDecimal.ZERO;
        if (valor instanceof BigDecimal b) return b;
        return new BigDecimal(valor.toString());
    }

    public static double variacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) return 0;
        return actual.subtract(anterior).multiply(BigDecimal.valueOf(100))
                .divide(anterior, 1, RoundingMode.HALF_UP).doubleValue();
    }
}
