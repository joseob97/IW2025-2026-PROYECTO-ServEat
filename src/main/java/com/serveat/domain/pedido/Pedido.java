package com.serveat.domain.pedido;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.reserva.ReservaMesa;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "PEDIDOS")
public class Pedido {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_mesa_id")
    private ReservaMesa reservaMesa;

    @OneToMany(mappedBy = "pedidos",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    private List<LineaPedido> lineaPedidos = new ArrayList<>();


    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

   public void crearLineaPedido(Producto producto, int cantidad) {
        LineaPedido lineaPedido = new LineaPedido(this, producto, cantidad);
        lineaPedidos.add(lineaPedido);
   }

   public BigDecimal calcularPrecioTotal() {
        return lineaPedidos.stream()
                .map(LineaPedido::calcularPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
   }

   public List<LineaPedido> getLineaPedidos() {
        return lineaPedidos;
   }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public ReservaMesa getReservaMesa() { return reservaMesa; }
    public void setReservaMesa(ReservaMesa reservaMesa) { this.reservaMesa = reservaMesa; }
}