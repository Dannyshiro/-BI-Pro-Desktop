package com.dy.bipro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import com.dy.bipro.ui.DYFormato;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DYUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 30)
    @Column(nullable = false, unique = true, length = 30)
    private String usuario;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 64)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DYRol rol;

    @Builder.Default
    private boolean activo = true;

    public String getIniciales() {
        return DYFormato.iniciales(nombre);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
