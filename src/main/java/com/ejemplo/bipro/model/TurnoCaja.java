package com.ejemplo.bipro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "turno_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class TurnoCaja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime apertura;

    @NotNull
    @PositiveOrZero(message = "El monto inicial no puede ser negativo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial;

    private LocalDateTime cierre;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalVentas;

    private Integer numeroVentas;

    @Column(precision = 12, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    @Column(precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoCaja estado;
}
