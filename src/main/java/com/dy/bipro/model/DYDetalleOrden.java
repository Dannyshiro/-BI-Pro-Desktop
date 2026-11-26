package com.dy.bipro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_orden")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DYDetalleOrden {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private DYOrdenCompra orden;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private DYProducto producto;

    @Positive
    private int cantidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    public BigDecimal getSubtotal() {
        return costoUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
