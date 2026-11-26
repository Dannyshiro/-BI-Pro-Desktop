package com.ejemplo.bipro.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Modulo {
    DASHBOARD("Dashboard", "fth-grid", "dashboard-view", false),
    VENTAS("Ventas", "fth-shopping-cart", "ventas-view", false),
    CAJA("Caja", "fth-dollar-sign", "caja-view", false),
    INVENTARIO("Inventario", "fth-package", "inventario-view", false),
    COMPRAS("Compras", "fth-truck", "compras-view", true),
    CLIENTES("Clientes", "fth-users", "clientes-view", false),
    FINANZAS("Finanzas", "fth-pie-chart", "finanzas-view", true),
    ORGANIZADOR("Organizador", "fth-trello", "organizador-view", false),
    ASISTENTE("Asistente", "fth-message-circle", "asistente-view", false),
    CONFIGURACION("Configuración", "fth-settings", "configuracion-view", true);

    private final String titulo;
    private final String icono;
    private final String vista;
    private final boolean soloAdmin;
}
