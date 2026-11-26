package com.ejemplo.bipro.service;

import com.ejemplo.bipro.dto.Intervalo;
import com.ejemplo.bipro.dto.Notificacion;
import com.ejemplo.bipro.model.*;
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
public class DiagnosticoService {
    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final CajaService cajaService;
    private final CompraService compraService;
    private final TareaService tareaService;
    private final ReporteService reporteService;
    private final Sesion sesion;

    public List<Notificacion> generar() {
        List<Notificacion> lista = new ArrayList<>();
        List<Producto> productos = productoService.listarTodos();

        if (cajaService.turnoAbierto().isEmpty()) {
            lista.add(new Notificacion("caja-" + LocalDate.now(), Severidad.ADVERTENCIA, "CAJA",
                    "La caja no está abierta", "Abre la caja con el monto inicial para poder registrar ventas."));
        }
        List<Producto> agotados = filtrar(productos, EstadoStock.AGOTADO);
        if (!agotados.isEmpty()) {
            lista.add(new Notificacion("ago-" + ids(agotados), Severidad.CRITICO, "INVENTARIO",
                    agotados.size() + (agotados.size() == 1 ? " producto agotado" : " productos agotados"),
                    nombres(agotados) + " sin stock. Genera una orden de compra para reponerlos."));
        }
        List<Producto> criticos = filtrar(productos, EstadoStock.CRITICO);
        if (!criticos.isEmpty()) {
            lista.add(new Notificacion("crit-" + ids(criticos), Severidad.ADVERTENCIA, "INVENTARIO",
                    criticos.size() + " producto(s) bajo stock mínimo", nombres(criticos) + "."));
        }
        long pendientes = compraService.pendientes();
        if (pendientes > 0) {
            lista.add(new Notificacion("oc-" + pendientes, Severidad.INFO, "COMPRAS",
                    pendientes + " orden(es) de compra por recibir",
                    "Registra la recepción para actualizar el stock."));
        }
        List<Tarea> vencidas = tareaService.vencidas();
        if (!vencidas.isEmpty()) {
            lista.add(new Notificacion("tar-" + vencidas.size(), Severidad.ADVERTENCIA, "ORGANIZADOR",
                    vencidas.size() + " tarea(s) vencida(s)",
                    vencidas.stream().map(Tarea::getTitulo).collect(Collectors.joining(", ")) + "."));
        }

        LocalDate hoy = LocalDate.now();
        BigDecimal mes = reporteService.kpis(new Intervalo(hoy.withDayOfMonth(1), hoy)).gastos();
        BigDecimal tres = reporteService.kpis(new Intervalo(hoy.withDayOfMonth(1).minusMonths(3),
                hoy.withDayOfMonth(1).minusDays(1))).gastos();
        if (tres.signum() > 0) {
            double esperado = tres.doubleValue() / 3 * hoy.getDayOfMonth() / hoy.lengthOfMonth();
            double pct = (mes.doubleValue() / esperado - 1) * 100;
            if (pct > 5) {
                lista.add(new Notificacion("gas-" + Math.round(pct), Severidad.ADVERTENCIA, "FINANZAS",
                        String.format("Gastos operativos +%.0f%%", pct),
                        "Los gastos del mes superan el promedio de los últimos 3 meses a la fecha."));
            }
        }

        List<Cliente> inactivos = clienteService.buscar(null, Segmento.INACTIVO);
        if (!inactivos.isEmpty()) {
            lista.add(new Notificacion("ina-" + inactivos.size(), Severidad.ADVERTENCIA, "CLIENTES",
                    inactivos.size() + " cliente(s) inactivo(s)",
                    "Sin compras en más de 60 días: " + inactivos.stream().map(Cliente::getNombre)
                            .collect(Collectors.joining(", ")) + "."));
        }
        List<Producto> exceso = filtrar(productos, EstadoStock.EXCESO);
        if (!exceso.isEmpty()) {
            lista.add(new Notificacion("exc-" + ids(exceso), Severidad.INFO, "INVENTARIO",
                    "Exceso de inventario: " + nombres(exceso),
                    "Stock por encima del máximo. Considera una promoción para liberar capital."));
        }

        return lista.stream()
                .filter(n -> sesion.esAdmin() || !(n.modulo().equals("FINANZAS") || n.modulo().equals("COMPRAS")))
                .sorted(Comparator.comparing(Notificacion::severidad))
                .toList();
    }

    public long sinLeer() {
        return generar().stream().filter(n -> !sesion.leida(n.id())).count();
    }

    private static List<Producto> filtrar(List<Producto> productos, EstadoStock estado) {
        return productos.stream().filter(p -> p.getEstado() == estado).toList();
    }

    private static String nombres(List<Producto> productos) {
        return productos.stream().map(Producto::getNombre).collect(Collectors.joining(", "));
    }

    private static String ids(List<Producto> productos) {
        return productos.stream().map(p -> String.valueOf(p.getId())).collect(Collectors.joining("."));
    }
}
