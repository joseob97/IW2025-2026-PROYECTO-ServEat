package com.serveat.domain.pedido;

import com.serveat.domain.menu.Producto;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "LINEAPEDIDOS")
public class LineaPedido {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    private BigDecimal precioUnitario;
    private int cantidad;

    @ManyToOne(fetch = FetchType.LAZY,
            optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedidos;

    @ManyToOne(fetch = FetchType.LAZY,
            optional = false)
    @JoinColumn(name = "producto_id")
    private Producto productos;

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
    public int getCantidad() {
        return cantidad;
    }

    public LineaPedido(Pedido pedido, Producto producto, int cantidad) {
        this.pedidos = pedido;
        this.productos = producto;
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecio();
    }

    protected LineaPedido() {}

    public BigDecimal calcularPrecio() {
        return precioUnitario.multiply(new BigDecimal(cantidad));
    }

    public Producto getProducto() {
        return productos;
    }
}