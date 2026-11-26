package com.dy.bipro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DYEmpresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 120)
    private String razonSocial;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos")
    @Column(length = 11)
    private String ruc;

    @Size(max = 160)
    private String direccion;

    @Size(max = 20)
    private String telefono;

    @NotNull(message = "El IGV es obligatorio")
    @DecimalMin(value = "0", message = "El IGV no puede ser negativo")
    @DecimalMax(value = "30", message = "El IGV no puede superar 30%")
    private BigDecimal igv;

    @NotBlank
    @Pattern(regexp = "[A-Z]\\d{3}", message = "La serie debe tener el formato B001")
    @Column(length = 4)
    private String serieBoleta;
}
