package com.dy.bipro.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DYModulo {
    DASHBOARD("Dashboard", "fth-grid", "dy-dashboard-view", false),
    VENTAS("Ventas", "fth-shopping-cart", "dy-ventas-view", false),
    CAJA("Caja", "fth-dollar-sign", "dy-caja-view", false),
    INVENTARIO("Inventario", "fth-package", "dy-inventario-view", false),
    COMPRAS("Compras", "fth-truck", "dy-compras-view", true),
    CLIENTES("Clientes", "fth-users", "dy-clientes-view", false),
    FINANZAS("Finanzas", "fth-pie-chart", "dy-finanzas-view", true),
    ORGANIZADOR("Organizador", "fth-trello", "dy-organizador-view", false),
    ASISTENTE("Asistente", "fth-message-circle", "dy-asistente-view", false),
    CONFIGURACION("Configuración", "fth-settings", "dy-configuracion-view", true);

    private final String titulo;
    private final String icono;
    private final String vista;
    private final boolean soloAdmin;
}
