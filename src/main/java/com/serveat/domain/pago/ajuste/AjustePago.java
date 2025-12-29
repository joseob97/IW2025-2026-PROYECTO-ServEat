package com.serveat.domain.pago.ajuste;

import com.serveat.domain.pedido.Pedido;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "AJUSTES_PAGO")
public class AjustePago {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Column(unique = true, nullable = false, updatable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAjustePago tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAjustePago estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaCompletado;

    @Column(length = 120)
    private String referenciaProveedor;

    @Column(length = 255)
    private String motivoFallo;

    protected AjustePago() {}

    public AjustePago(Pedido pedido, String codigo, TipoAjustePago tipo, BigDecimal importe) {
        this.pedido = pedido;
        this.codigo = codigo;
        this.tipo = tipo;
        this.importe = importe;
        this.estado = EstadoAjustePago.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
    }

    public UUID getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public TipoAjustePago getTipo() { return tipo; }
    public void setTipo(TipoAjustePago tipo) { this.tipo = tipo; }

    public EstadoAjustePago getEstado() { return estado; }
    public void setEstado(EstadoAjustePago estado) { this.estado = estado; }

    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaCompletado() { return fechaCompletado; }
    public void setFechaCompletado(LocalDateTime fechaCompletado) { this.fechaCompletado = fechaCompletado; }

    public String getReferenciaProveedor() { return referenciaProveedor; }
    public void setReferenciaProveedor(String referenciaProveedor) { this.referenciaProveedor = referenciaProveedor; }

    public String getMotivoFallo() { return motivoFallo; }
    public void setMotivoFallo(String motivoFallo) { this.motivoFallo = motivoFallo; }
}