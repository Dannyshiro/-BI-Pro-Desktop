package com.dy.bipro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tarea")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DYTarea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 100, message = "El título no puede superar 100 caracteres")
    @Column(nullable = false, length = 100)
    private String titulo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DYEstadoTarea estado;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private DYPrioridad prioridad;

    private LocalDate fechaLimite;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_id")
    private DYUsuario responsable;

    private int orden;

    public boolean isVencida() {
        return fechaLimite != null && estado != DYEstadoTarea.HECHO && fechaLimite.isBefore(LocalDate.now());
    }
}
