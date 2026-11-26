package com.ejemplo.bipro.dto;

import com.ejemplo.bipro.model.Severidad;

public record Notificacion(String id, Severidad severidad, String modulo, String titulo, String descripcion) {
}
