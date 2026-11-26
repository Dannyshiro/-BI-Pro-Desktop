package com.ejemplo.bipro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import com.ejemplo.bipro.ui.Formato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre o razón social es obligatorio")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El DNI o RUC es obligatorio")
    @Pattern(regexp = "\\d{8}|\\d{11}", message = "Debe tener 8 dígitos (DNI) u 11 dígitos (RUC)")
    @Column(nullable = false, unique = true, length = 11)
    private String documento;

    @Pattern(regexp = "|9\\d{8}", message = "El teléfono debe tener 9 dígitos y empezar con 9")
    @Column(length = 9)
    private String telefono;

    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 120)
    private String email;

    @Builder.Default
    private int numeroCompras = 0;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal totalComprado = BigDecimal.ZERO;

    private LocalDate ultimaCompra;

    public Segmento getSegmento() {
        if (ultimaCompra == null || numeroCompras == 0) return Segmento.NUEVO;
        long dias = ChronoUnit.DAYS.between(ultimaCompra, LocalDate.now());
        if (dias > 60) return Segmento.INACTIVO;
        if (numeroCompras <= 2) return Segmento.NUEVO;
        if (numeroCompras >= 20) return Segmento.FRECUENTE;
        return Segmento.REGULAR;
    }

    public String getTipoDocumento() {
        return documento != null && documento.length() == 11 ? "RUC" : "DNI";
    }

    public String getIniciales() {
        return Formato.iniciales(nombre);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
