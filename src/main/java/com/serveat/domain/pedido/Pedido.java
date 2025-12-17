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

    // IDENTIDAD

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Column(unique = true, nullable = false, updatable = false)
    private String codigo;

    // ESTADOS

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCocina estadoCocina = EstadoCocina.PENDIENTE_ACEPTACION;

    // FECHAS

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "ultima_modificacion")
    private LocalDateTime fechaUltimaModificacion;

    @Column(name = "modificado_por")
    private String modificadoPor;

    // CANCELACIÓN

    private String canceladoPor;
    private String motivoCancelacion;
    private LocalDateTime fechaCancelacion;

    //  RELACIONES

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_mesa_id")
    private ReservaMesa reservaMesa;

    @OneToMany(
            mappedBy = "pedidos",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<LineaPedido> lineaPedidos = new ArrayList<>();

    // LÓGICA DE DOMINIO

    /**
     * Marca el pedido como modificado por un usuario.
     * Se usa al confirmar cambios desde la vista de edición.
     */
    public void marcarModificado(String username) {
        this.modificadoPor = username;
        this.fechaUltimaModificacion = LocalDateTime.now();
    }

    /**
     * Añade una línea al pedido.
     */
    public void crearLineaPedido(Producto producto, int cantidad) {
        LineaPedido lineaPedido = new LineaPedido(this, producto, cantidad);
        lineaPedidos.add(lineaPedido);
    }

    /**
     * Calcula el total del pedido.
     */
    public BigDecimal calcularPrecioTotal() {
        return lineaPedidos.stream()
                .map(LineaPedido::calcularPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public EstadoCocina getEstadoCocina() {
        return estadoCocina;
    }

    public void setEstadoCocina(EstadoCocina estadoCocina) {
        this.estadoCocina = estadoCocina;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaUltimaModificacion() {
        return fechaUltimaModificacion;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public String getCanceladoPor() {
        return canceladoPor;
    }

    public void setCanceladoPor(String canceladoPor) {
        this.canceladoPor = canceladoPor;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public LocalDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(LocalDateTime fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }

    public ReservaMesa getReservaMesa() {
        return reservaMesa;
    }

    public void setReservaMesa(ReservaMesa reservaMesa) {
        this.reservaMesa = reservaMesa;
    }

    public List<LineaPedido> getLineaPedidos() {
        return lineaPedidos;
    }
}