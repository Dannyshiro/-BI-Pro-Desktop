package com.dy.bipro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DYProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    @Column(nullable = false, length = 80)
    private String nombre;

    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String sku;

    @NotNull(message = "La categoría es obligatoria")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private DYCategoria categoria;

    @Size(max = 300, message = "La descripción no puede superar 300 caracteres")
    @Column(length = 300)
    private String descripcion;

    @NotNull(message = "El precio de compra es obligatorio")
    @PositiveOrZero(message = "El precio de compra no puede ser negativo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCompra;

    @NotNull(message = "El precio de venta es obligatorio")
    @PositiveOrZero(message = "El precio de venta no puede ser negativo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;

    @NotNull(message = "El stock actual es obligatorio")
    @PositiveOrZero(message = "El stock actual no puede ser negativo")
    @Column(nullable = false)
    private Integer stockActual;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Positive(message = "El stock mínimo debe ser mayor que 0")
    @Column(nullable = false)
    private Integer stockMinimo;

    @NotNull(message = "El stock máximo es obligatorio")
    @Positive(message = "El stock máximo debe ser mayor que 0")
    @Column(nullable = false)
    private Integer stockMaximo;

    public DYEstadoStock getEstado() {
        if (stockActual == 0) return DYEstadoStock.AGOTADO;
        if (stockActual < stockMinimo) return DYEstadoStock.CRITICO;
        if (stockActual > stockMaximo) return DYEstadoStock.EXCESO;
        return DYEstadoStock.NORMAL;
    }

    @Override
    public String toString() {
        return nombre + " (stock " + stockActual + ")";
    }
}
