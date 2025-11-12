package com.serveat.domain.pedido;

import com.serveat.domain.comun.BaseEntity;
import com.serveat.domain.usuario.Cliente;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Pedido extends BaseEntity {

    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha;

    private String estado;  //  "PENDIENTE", "PREPARANDO", "ENTREGADO"

    @Column(precision = 12, scale = 2)
    private BigDecimal importe = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;  // null si es un usuario invitado

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();


    protected Pedido() {
        this.fecha = new Date();
        this.estado = "PENDIENTE";
    }

    public Pedido(Cliente cliente) {
        this();
        this.cliente = cliente;
    }


    public void addDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        detalles.add(detalle);
        calcularImporte();
    }

    public void calcularImporte() {
        this.importe = detalles.stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Date getFecha() { return fecha; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) {
        this.detalles = detalles;
        calcularImporte();
    }
}
