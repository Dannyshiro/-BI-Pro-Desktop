package com.ejemplo.bipro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gasto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Gasto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "La fecha del gasto no puede ser posterior a hoy")
    @Column(nullable = false)
    private LocalDate fecha;

    @NotNull(message = "La categoría es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private CategoriaGasto categoria;

    @Size(max = 120, message = "La descripción no puede superar 120 caracteres")
    private String descripcion;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor que 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Size(max = 255)
    private String comprobante;
}
