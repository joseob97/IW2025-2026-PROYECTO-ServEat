package com.serveat.domain.menu;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "PRODUCTO_INGREDIENTES")
public class ProductoIngrediente {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingrediente_id")
    private Ingrediente ingrediente;

    @Column(nullable = false)
    private boolean porDefecto = true;

    @Column(nullable = false)
    private boolean opcional = true;

    @Column(precision = 10, scale = 2)
    private BigDecimal precioExtra = BigDecimal.ZERO;

    protected ProductoIngrediente() {}

    public ProductoIngrediente(Producto producto,
                               Ingrediente ingrediente,
                               boolean porDefecto,
                               boolean opcional,
                               BigDecimal precioExtra) {
        this.producto = producto;
        this.ingrediente = ingrediente;
        this.porDefecto = porDefecto;
        this.opcional = opcional;
        this.precioExtra = (precioExtra == null) ? BigDecimal.ZERO : precioExtra;
    }

    public UUID getId() { return id; }

    public Producto getProducto() { return producto; }

    public Ingrediente getIngrediente() { return ingrediente; }

    public boolean isPorDefecto() { return porDefecto; }

    public void setPorDefecto(boolean porDefecto) { this.porDefecto = porDefecto; }

    public boolean isOpcional() { return opcional; }

    public void setOpcional(boolean opcional) { this.opcional = opcional; }

    public BigDecimal getPrecioExtra() { return precioExtra; }

    public void setPrecioExtra(BigDecimal precioExtra) {
        this.precioExtra = (precioExtra == null) ? BigDecimal.ZERO : precioExtra;
    }
}