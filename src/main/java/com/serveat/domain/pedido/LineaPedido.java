package com.serveat.domain.pedido;

import com.serveat.domain.menu.Producto;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "LINEAPEDIDOS")
public class LineaPedido {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private String codigo;

    @Column(name = "precio_unitario", precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private int cantidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedidos;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto productos;


    @OneToMany(mappedBy = "linea", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LineaPedidoIngrediente> ingredientes = new LinkedHashSet<>();

    protected LineaPedido() {
    }

    public LineaPedido(Pedido pedido, Producto producto, int cantidad) {
        this.codigo = "LP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.pedidos = pedido;
        this.productos = producto;
        this.cantidad = cantidad;
        this.precioUnitario = (producto != null) ? producto.getPrecio() : BigDecimal.ZERO;
    }

    // LÓGICA DE DOMINIO

    public BigDecimal calcularPrecio() {
        BigDecimal base = (precioUnitario != null) ? precioUnitario : BigDecimal.ZERO;
        
        // Sumar extras de ingredientes si los hubiera
        BigDecimal extras = BigDecimal.ZERO;
        if (ingredientes != null) {
            for (LineaPedidoIngrediente ing : ingredientes) {
                if (ing.getPrecioExtra() != null) {
                    extras = extras.add(ing.getPrecioExtra());
                }
            }
        }

        return base.add(extras).multiply(BigDecimal.valueOf(cantidad));
    }

    // GETTERS Y SETTERS

    public UUID getId() {
        return id;
    }


    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public Pedido getPedidos() {
        return pedidos;
    }

    public void setPedidos(Pedido pedidos) {
        this.pedidos = pedidos;
    }

    public Producto getProductos() {
        return productos;
    }

    public void setProductos(Producto productos) {
        this.productos = productos;
    }


    public Set<LineaPedidoIngrediente> getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(Set<LineaPedidoIngrediente> ingredientes) {
        this.ingredientes = (ingredientes == null) ? new LinkedHashSet<>() : ingredientes;
    }


    @Transient
    public Producto getProducto() {
        return productos;
    }

    public void setProducto(Producto producto) {
        this.productos = producto;
    }

    @Transient
    public Pedido getPedido() {
        return pedidos;
    }

    public void setPedido(Pedido pedido) {
        this.pedidos = pedido;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineaPedido other)) return false;
        return codigo != null && codigo.equals(other.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}
