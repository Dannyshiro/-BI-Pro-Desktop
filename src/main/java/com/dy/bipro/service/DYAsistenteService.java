package com.dy.bipro.service;

import com.dy.bipro.dto.DYIntervalo;
import com.dy.bipro.dto.DYKpis;
import com.dy.bipro.dto.DYRespuesta;
import com.dy.bipro.model.*;
import com.dy.bipro.ui.DYFormato;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DYAsistenteService {
    private final DYProductoService productoService;
    private final DYClienteService clienteService;
    private final DYTareaService tareaService;
    private final DYReporteService reporteService;
    private final DYCajaService cajaService;

    public DYRespuesta responder(String pregunta) {
        String q = pregunta == null ? "" : pregunta.toLowerCase();

        if (q.matches(".*(repon|stock|invent|agot|producto).*")) {
            List<DYProducto> bajos = productoService.listarTodos().stream()
                    .filter(p -> p.getStockActual() < p.getStockMinimo())
                    .sorted(Comparator.comparing(DYProducto::getStockActual)).toList();
            if (bajos.isEmpty()) return new DYRespuesta("Todo tu inventario está sobre el stock mínimo.", List.of(), "INVENTARIO");
            return new DYRespuesta("Tienes " + bajos.size() + " productos bajo el stock mínimo. Reposición sugerida hasta el máximo:",
                    bajos.stream().map(p -> p.getNombre() + ": stock " + p.getStockActual() + " / mín. "
                            + p.getStockMinimo() + " → pedir " + (p.getStockMaximo() - p.getStockActual()) + " u.").toList(),
                    "INVENTARIO");
        }
        if (q.matches(".*(venta|vend|ingreso|factur).*")) {
            LocalDate hoy = LocalDate.now();
            DYKpis mes = reporteService.kpis(new DYIntervalo(hoy.withDayOfMonth(1), hoy));
            DYKpis ant = reporteService.kpis(new DYIntervalo(hoy.withDayOfMonth(1).minusMonths(1), hoy.minusMonths(1)));
            Map<String, BigDecimal> cats = reporteService.ventasPorCategoria(new DYIntervalo(hoy.withDayOfMonth(1), hoy));
            String lider = cats.isEmpty() ? "-" : cats.keySet().iterator().next();
            return new DYRespuesta("Este mes llevas " + DYFormato.soles(mes.ventas()) + " en ventas ("
                    + DYFormato.variacion(DYReporteService.variacion(mes.ventas(), ant.ventas())) + " vs. el mismo tramo del mes anterior).",
                    List.of(mes.transacciones() + " transacciones, ticket promedio " + DYFormato.soles(mes.ticketPromedio()) + ".",
                            "Margen bruto: " + mes.margenBruto() + "%.",
                            "Categoría líder: " + lider + "."),
                    "VENTAS");
        }
        if (q.matches(".*(gast|financ|utilidad|margen|cost|egreso).*")) {
            LocalDate hoy = LocalDate.now();
            DYIntervalo mes = new DYIntervalo(hoy.withDayOfMonth(1), hoy);
            DYKpis k = reporteService.kpis(mes);
            return new DYRespuesta("Este mes llevas " + DYFormato.soles(k.gastos()) + " en gastos operativos. Utilidad neta: "
                    + DYFormato.soles(k.utilidad()) + " (margen neto " + k.margenNeto() + "%).",
                    reporteService.gastosPorCategoria(mes).entrySet().stream()
                            .sorted(Map.Entry.<DYCategoriaGasto, BigDecimal>comparingByValue().reversed())
                            .limit(3).map(e -> e.getKey() + ": " + DYFormato.soles(e.getValue())).toList(),
                    "FINANZAS");
        }
        if (q.contains("client")) {
            List<DYCliente> inactivos = clienteService.buscar(null, DYSegmento.INACTIVO);
            return new DYRespuesta("Te recomiendo contactar a estos " + inactivos.size() + " clientes inactivos:",
                    inactivos.stream().sorted(Comparator.comparing(DYCliente::getTotalComprado).reversed())
                            .map(c -> c.getNombre() + ": " + DYFormato.soles(c.getTotalComprado())
                                    + " comprados, última compra " + DYFormato.fecha(c.getUltimaCompra())).toList(),
                    "CLIENTES");
        }
        if (q.matches(".*(tarea|pendiente|organiz|hacer).*")) {
            List<DYTarea> abiertas = tareaService.porEstado(DYEstadoTarea.PENDIENTE);
            return new DYRespuesta("Tienes " + abiertas.size() + " tareas pendientes. Las prioritarias:",
                    abiertas.stream().filter(t -> t.getPrioridad() == DYPrioridad.ALTA || t.isVencida())
                            .map(t -> t.getTitulo() + (t.getFechaLimite() != null ? " · vence " + DYFormato.fecha(t.getFechaLimite()) : ""))
                            .toList(),
                    "ORGANIZADOR");
        }
        if (q.matches(".*(caja|turno|efectivo).*")) {
            return cajaService.turnoAbierto()
                    .map(t -> {
                        var r = cajaService.resumen(t);
                        return new DYRespuesta("La caja está abierta desde las " + DYFormato.hora(t.getApertura()) + ".",
                                List.of(r.numeroVentas() + " ventas por " + DYFormato.soles(r.totalVentas()) + ".",
                                        "Efectivo esperado en caja: " + DYFormato.soles(r.efectivoEsperado()) + "."), "CAJA");
                    })
                    .orElse(new DYRespuesta("La caja está cerrada. Ábrela para registrar ventas.", List.of(), "CAJA"));
        }
        return new DYRespuesta("Puedo analizar tu inventario, ventas, gastos, clientes, caja y tareas. "
                + "Prueba con alguna de las sugerencias.", List.of(), null);
    }
}
