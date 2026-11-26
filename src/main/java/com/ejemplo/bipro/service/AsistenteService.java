package com.ejemplo.bipro.service;

import com.ejemplo.bipro.dto.Intervalo;
import com.ejemplo.bipro.dto.Kpis;
import com.ejemplo.bipro.dto.Respuesta;
import com.ejemplo.bipro.model.*;
import com.ejemplo.bipro.ui.Formato;
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
public class AsistenteService {
    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final TareaService tareaService;
    private final ReporteService reporteService;
    private final CajaService cajaService;

    public Respuesta responder(String pregunta) {
        String q = pregunta == null ? "" : pregunta.toLowerCase();

        if (q.matches(".*(repon|stock|invent|agot|producto).*")) {
            List<Producto> bajos = productoService.listarTodos().stream()
                    .filter(p -> p.getStockActual() < p.getStockMinimo())
                    .sorted(Comparator.comparing(Producto::getStockActual)).toList();
            if (bajos.isEmpty()) return new Respuesta("Todo tu inventario está sobre el stock mínimo.", List.of(), "INVENTARIO");
            return new Respuesta("Tienes " + bajos.size() + " productos bajo el stock mínimo. Reposición sugerida hasta el máximo:",
                    bajos.stream().map(p -> p.getNombre() + ": stock " + p.getStockActual() + " / mín. "
                            + p.getStockMinimo() + " → pedir " + (p.getStockMaximo() - p.getStockActual()) + " u.").toList(),
                    "INVENTARIO");
        }
        if (q.matches(".*(venta|vend|ingreso|factur).*")) {
            LocalDate hoy = LocalDate.now();
            Kpis mes = reporteService.kpis(new Intervalo(hoy.withDayOfMonth(1), hoy));
            Kpis ant = reporteService.kpis(new Intervalo(hoy.withDayOfMonth(1).minusMonths(1), hoy.minusMonths(1)));
            Map<String, BigDecimal> cats = reporteService.ventasPorCategoria(new Intervalo(hoy.withDayOfMonth(1), hoy));
            String lider = cats.isEmpty() ? "-" : cats.keySet().iterator().next();
            return new Respuesta("Este mes llevas " + Formato.soles(mes.ventas()) + " en ventas ("
                    + Formato.variacion(ReporteService.variacion(mes.ventas(), ant.ventas())) + " vs. el mismo tramo del mes anterior).",
                    List.of(mes.transacciones() + " transacciones, ticket promedio " + Formato.soles(mes.ticketPromedio()) + ".",
                            "Margen bruto: " + mes.margenBruto() + "%.",
                            "Categoría líder: " + lider + "."),
                    "VENTAS");
        }
        if (q.matches(".*(gast|financ|utilidad|margen|cost|egreso).*")) {
            LocalDate hoy = LocalDate.now();
            Intervalo mes = new Intervalo(hoy.withDayOfMonth(1), hoy);
            Kpis k = reporteService.kpis(mes);
            return new Respuesta("Este mes llevas " + Formato.soles(k.gastos()) + " en gastos operativos. Utilidad neta: "
                    + Formato.soles(k.utilidad()) + " (margen neto " + k.margenNeto() + "%).",
                    reporteService.gastosPorCategoria(mes).entrySet().stream()
                            .sorted(Map.Entry.<CategoriaGasto, BigDecimal>comparingByValue().reversed())
                            .limit(3).map(e -> e.getKey() + ": " + Formato.soles(e.getValue())).toList(),
                    "FINANZAS");
        }
        if (q.contains("client")) {
            List<Cliente> inactivos = clienteService.buscar(null, Segmento.INACTIVO);
            return new Respuesta("Te recomiendo contactar a estos " + inactivos.size() + " clientes inactivos:",
                    inactivos.stream().sorted(Comparator.comparing(Cliente::getTotalComprado).reversed())
                            .map(c -> c.getNombre() + ": " + Formato.soles(c.getTotalComprado())
                                    + " comprados, última compra " + Formato.fecha(c.getUltimaCompra())).toList(),
                    "CLIENTES");
        }
        if (q.matches(".*(tarea|pendiente|organiz|hacer).*")) {
            List<Tarea> abiertas = tareaService.porEstado(EstadoTarea.PENDIENTE);
            return new Respuesta("Tienes " + abiertas.size() + " tareas pendientes. Las prioritarias:",
                    abiertas.stream().filter(t -> t.getPrioridad() == Prioridad.ALTA || t.isVencida())
                            .map(t -> t.getTitulo() + (t.getFechaLimite() != null ? " · vence " + Formato.fecha(t.getFechaLimite()) : ""))
                            .toList(),
                    "ORGANIZADOR");
        }
        if (q.matches(".*(caja|turno|efectivo).*")) {
            return cajaService.turnoAbierto()
                    .map(t -> {
                        var r = cajaService.resumen(t);
                        return new Respuesta("La caja está abierta desde las " + Formato.hora(t.getApertura()) + ".",
                                List.of(r.numeroVentas() + " ventas por " + Formato.soles(r.totalVentas()) + ".",
                                        "Efectivo esperado en caja: " + Formato.soles(r.efectivoEsperado()) + "."), "CAJA");
                    })
                    .orElse(new Respuesta("La caja está cerrada. Ábrela para registrar ventas.", List.of(), "CAJA"));
        }
        return new Respuesta("Puedo analizar tu inventario, ventas, gastos, clientes, caja y tareas. "
                + "Prueba con alguna de las sugerencias.", List.of(), null);
    }
}
