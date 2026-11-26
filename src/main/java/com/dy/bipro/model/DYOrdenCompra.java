package com.dy.bipro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orden_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DYOrdenCompra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 14)
    private String numero;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private DYProveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DYEstadoOrden estado;

    private LocalDate fechaRecepcion;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DYDetalleOrden> detalles = new ArrayList<>();

    public void agregarDetalle(DYDetalleOrden d) {
        d.setOrden(this);
        detalles.add(d);
    }

    public BigDecimal getTotal() {
        return detalles.stream().map(DYDetalleOrden::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
