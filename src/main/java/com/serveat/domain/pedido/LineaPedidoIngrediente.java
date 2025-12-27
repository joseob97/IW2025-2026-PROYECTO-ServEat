package com.serveat.domain.pedido;

import com.serveat.domain.menu.Ingrediente;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "LINEA_PEDIDO_INGREDIENTES")
public class LineaPedidoIngrediente {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linea_id", nullable = false)
    private LineaPedido linea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    @Column(nullable = false)
    private boolean incluido = true;

    @Column(nullable = false)
    private int extraCantidad = 0;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precioExtra = BigDecimal.ZERO;

    protected LineaPedidoIngrediente() {
    }

    public LineaPedidoIngrediente(LineaPedido linea,
                                  Ingrediente ingrediente,
                                  boolean incluido,
                                  int extraCantidad,
                                  BigDecimal precioExtra) {
        this.linea = linea;
        this.ingrediente = ingrediente;
        this.incluido = incluido;
        this.extraCantidad = Math.max(extraCantidad, 0);
        this.precioExtra = (precioExtra == null) ? BigDecimal.ZERO : precioExtra;
    }


    public LineaPedido getLinea() {
        return linea;
    }

    public void setLinea(LineaPedido linea) {
        this.linea = linea;
    }

    public Ingrediente getIngrediente() {
        return ingrediente;
    }

    public void setIngrediente(Ingrediente ingrediente) {
        this.ingrediente = ingrediente;
    }

    public boolean isIncluido() {
        return incluido;
    }

    public void setIncluido(boolean incluido) {
        this.incluido = incluido;
    }

    public int getExtraCantidad() {
        return extraCantidad;
    }

    public void setExtraCantidad(int extraCantidad) {
        this.extraCantidad = Math.max(extraCantidad, 0);
    }

    public BigDecimal getPrecioExtra() {
        return precioExtra;
    }

    public void setPrecioExtra(BigDecimal precioExtra) {
        this.precioExtra = (precioExtra == null) ? BigDecimal.ZERO : precioExtra;
    }

    // La colección es Set
    // Usamos id cuando exista. Si id == null (objeto nuevo)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineaPedidoIngrediente other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return (id != null) ? Objects.hash(id) : System.identityHashCode(this);
    }
}