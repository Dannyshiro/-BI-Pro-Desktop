package com.dy.bipro.service;

import com.dy.bipro.dto.DYIntervalo;
import com.dy.bipro.dto.DYNotificacion;
import com.dy.bipro.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DYDiagnosticoService {
    private final DYProductoService productoService;
    private final DYClienteService clienteService;
    private final DYCajaService cajaService;
    private final DYCompraService compraService;
    private final DYTareaService tareaService;
    private final DYReporteService reporteService;
    private final DYSesion sesion;

    public List<DYNotificacion> generar() {
        List<DYNotificacion> lista = new ArrayList<>();
        List<DYProducto> productos = productoService.listarTodos();

        if (cajaService.turnoAbierto().isEmpty()) {
            lista.add(new DYNotificacion("caja-" + LocalDate.now(), DYSeveridad.ADVERTENCIA, "CAJA",
                    "La caja no está abierta", "Abre la caja con el monto inicial para poder registrar ventas."));
        }
        List<DYProducto> agotados = filtrar(productos, DYEstadoStock.AGOTADO);
        if (!agotados.isEmpty()) {
            lista.add(new DYNotificacion("ago-" + ids(agotados), DYSeveridad.CRITICO, "INVENTARIO",
                    agotados.size() + (agotados.size() == 1 ? " producto agotado" : " productos agotados"),
                    nombres(agotados) + " sin stock. Genera una orden de compra para reponerlos."));
        }
        List<DYProducto> criticos = filtrar(productos, DYEstadoStock.CRITICO);
        if (!criticos.isEmpty()) {
            lista.add(new DYNotificacion("crit-" + ids(criticos), DYSeveridad.ADVERTENCIA, "INVENTARIO",
                    criticos.size() + " producto(s) bajo stock mínimo", nombres(criticos) + "."));
        }
        long pendientes = compraService.pendientes();
        if (pendientes > 0) {
            lista.add(new DYNotificacion("oc-" + pendientes, DYSeveridad.INFO, "COMPRAS",
                    pendientes + " orden(es) de compra por recibir",
                    "Registra la recepción para actualizar el stock."));
        }
        List<DYTarea> vencidas = tareaService.vencidas();
        if (!vencidas.isEmpty()) {
            lista.add(new DYNotificacion("tar-" + vencidas.size(), DYSeveridad.ADVERTENCIA, "ORGANIZADOR",
                    vencidas.size() + " tarea(s) vencida(s)",
                    vencidas.stream().map(DYTarea::getTitulo).collect(Collectors.joining(", ")) + "."));
        }

        LocalDate hoy = LocalDate.now();
        BigDecimal mes = reporteService.kpis(new DYIntervalo(hoy.withDayOfMonth(1), hoy)).gastos();
        BigDecimal tres = reporteService.kpis(new DYIntervalo(hoy.withDayOfMonth(1).minusMonths(3),
                hoy.withDayOfMonth(1).minusDays(1))).gastos();
        if (tres.signum() > 0) {
            double esperado = tres.doubleValue() / 3 * hoy.getDayOfMonth() / hoy.lengthOfMonth();
            double pct = (mes.doubleValue() / esperado - 1) * 100;
            if (pct > 5) {
                lista.add(new DYNotificacion("gas-" + Math.round(pct), DYSeveridad.ADVERTENCIA, "FINANZAS",
                        String.format("Gastos operativos +%.0f%%", pct),
                        "Los gastos del mes superan el promedio de los últimos 3 meses a la fecha."));
            }
        }

        List<DYCliente> inactivos = clienteService.buscar(null, DYSegmento.INACTIVO);
        if (!inactivos.isEmpty()) {
            lista.add(new DYNotificacion("ina-" + inactivos.size(), DYSeveridad.ADVERTENCIA, "CLIENTES",
                    inactivos.size() + " cliente(s) inactivo(s)",
                    "Sin compras en más de 60 días: " + inactivos.stream().map(DYCliente::getNombre)
                            .collect(Collectors.joining(", ")) + "."));
        }
        List<DYProducto> exceso = filtrar(productos, DYEstadoStock.EXCESO);
        if (!exceso.isEmpty()) {
            lista.add(new DYNotificacion("exc-" + ids(exceso), DYSeveridad.INFO, "INVENTARIO",
                    "Exceso de inventario: " + nombres(exceso),
                    "Stock por encima del máximo. Considera una promoción para liberar capital."));
        }

        return lista.stream()
                .filter(n -> sesion.esAdmin() || !(n.modulo().equals("FINANZAS") || n.modulo().equals("COMPRAS")))
                .sorted(Comparator.comparing(DYNotificacion::severidad))
                .toList();
    }

    public long sinLeer() {
        return generar().stream().filter(n -> !sesion.leida(n.id())).count();
    }

    private static List<DYProducto> filtrar(List<DYProducto> productos, DYEstadoStock estado) {
        return productos.stream().filter(p -> p.getEstado() == estado).toList();
    }

    private static String nombres(List<DYProducto> productos) {
        return productos.stream().map(DYProducto::getNombre).collect(Collectors.joining(", "));
    }

    private static String ids(List<DYProducto> productos) {
        return productos.stream().map(p -> String.valueOf(p.getId())).collect(Collectors.joining("."));
    }
}
