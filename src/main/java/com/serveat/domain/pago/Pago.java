package com.serveat.domain.pago;

import com.serveat.domain.pedido.Pedido;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAGOS")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaConfirmacion;

    private String referenciaProveedor;

    private String motivoFallo;

    protected Pago() {}

    public Pago(Pedido pedido, MetodoPago metodo, BigDecimal importe) {
        this.pedido = pedido;
        this.metodo = metodo;
        this.importe = importe;
        this.estado = EstadoPago.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public String getReferenciaProveedor() {
        return referenciaProveedor;
    }

    public String getMotivoFallo() {
        return motivoFallo;
    }

    public void confirmar(String referencia) {
        this.estado = EstadoPago.CONFIRMADO;
        this.referenciaProveedor = referencia;
        this.fechaConfirmacion = LocalDateTime.now();
        this.motivoFallo = null;
    }

    public void fallar(String motivo) {
        this.estado = EstadoPago.FALLIDO;
        this.motivoFallo = motivo;
    }
}