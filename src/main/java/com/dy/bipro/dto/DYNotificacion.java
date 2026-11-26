package com.dy.bipro.dto;

import com.dy.bipro.model.DYSeveridad;

public record DYNotificacion(String id, DYSeveridad severidad, String modulo, String titulo, String descripcion) {
}
