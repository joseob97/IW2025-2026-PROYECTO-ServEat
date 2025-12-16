package com.serveat.domain.reserva;

import com.serveat.domain.pedido.Pedido;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "RESERVAS_MESA")
public class ReservaMesa {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Column(nullable = false)
    private Integer numeroMesa;

    @Column(nullable = false)
    private LocalDateTime inicio = LocalDateTime.now();

    private LocalDateTime fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReservaMesa estado;

    @OneToMany(mappedBy = "reservaMesa", fetch = FetchType.LAZY)
    private List<Pedido> pedidos = new ArrayList<>();

    protected ReservaMesa() {}

    public ReservaMesa(Integer numeroMesa) {
        this.numeroMesa = numeroMesa;
        this.estado = EstadoReservaMesa.ABIERTA;
    }

    // getters/setters
    public UUID getId() { return id; }

    public Integer getNumeroMesa() { return numeroMesa; }
    public void setNumeroMesa(Integer numeroMesa) { this.numeroMesa = numeroMesa; }

    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }

    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime fin) { this.fin = fin; }


    public List<Pedido> getPedidos() { return pedidos; }

    public void cerrar() {
        this.estado = EstadoReservaMesa.CERRADA;
        this.fin = LocalDateTime.now();
    }
}