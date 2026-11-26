package com.dy.bipro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DYProveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos")
    @Column(nullable = false, length = 11)
    private String ruc;

    @Size(max = 80)
    private String contacto;

    @Pattern(regexp = "|9\\d{8}", message = "El teléfono debe tener 9 dígitos y empezar con 9")
    @Column(length = 9)
    private String telefono;

    @Min(value = 1, message = "El plazo de entrega debe ser al menos 1 día")
    @Builder.Default
    private int plazoEntregaDias = 3;

    @Override
    public String toString() {
        return nombre;
    }
}
