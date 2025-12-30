package com.serveat.domain.pedido;

import com.serveat.domain.pago.Pago;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.domain.usuario.Cliente;
import com.serveat.domain.usuario.Empleado;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "PEDIDOS")
public class Pedido {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Column(unique = true, nullable = false, updatable = false)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCocina estadoCocina = EstadoCocina.PENDIENTE_ACEPTACION;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pedido", nullable = false)
    private TipoPedidoCliente tipoPedido;

    @Column(name = "direccion_entrega", length = 255)
    private String direccionEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_reparto", nullable = false)
    private EstadoReparto estadoReparto = EstadoReparto.NO_APLICA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id")
    private Empleado repartidor;

    @Column(name = "fecha_asignacion_reparto")
    private LocalDateTime fechaAsignacionReparto;

    @Column(name = "fecha_salida_reparto")
    private LocalDateTime fechaSalidaReparto;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "incidencia_reparto", length = 255)
    private String incidenciaReparto;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "ultima_modificacion")
    private LocalDateTime fechaUltimaModificacion;

    @Column(name = "modificado_por")
    private String modificadoPor;

    private String canceladoPor;
    private String motivoCancelacion;
    private LocalDateTime fechaCancelacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_mesa_id")
    private ReservaMesa reservaMesa;

    @OneToMany(mappedBy = "pedidos", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LineaPedido> lineaPedidos = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @OneToOne(mappedBy = "pedido", fetch = FetchType.LAZY)
    private Pago pago;

    public Pedido() {
    }

    // LÓGICA DE DOMINIO

    public void marcarModificado(String username) {
        this.modificadoPor = username;
        this.fechaUltimaModificacion = LocalDateTime.now();
    }

    public BigDecimal calcularPrecioTotal() {
        if (lineaPedidos == null || lineaPedidos.isEmpty()) {
            return BigDecimal.ZERO;
        }
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

    public TipoPedidoCliente getTipoPedido() {
        return tipoPedido;
    }

    public void setTipoPedido(TipoPedidoCliente tipoPedido) {
        this.tipoPedido = tipoPedido;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public EstadoReparto getEstadoReparto() {
        return estadoReparto;
    }

    public void setEstadoReparto(EstadoReparto estadoReparto) {
        this.estadoReparto = estadoReparto;
    }

    public Empleado getRepartidor() {
        return repartidor;
    }

    public void setRepartidor(Empleado repartidor) {
        this.repartidor = repartidor;
    }

    public LocalDateTime getFechaAsignacionReparto() {
        return fechaAsignacionReparto;
    }

    public void setFechaAsignacionReparto(LocalDateTime fechaAsignacionReparto) {
        this.fechaAsignacionReparto = fechaAsignacionReparto;
    }

    public LocalDateTime getFechaSalidaReparto() {
        return fechaSalidaReparto;
    }

    public void setFechaSalidaReparto(LocalDateTime fechaSalidaReparto) {
        this.fechaSalidaReparto = fechaSalidaReparto;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public String getIncidenciaReparto() {
        return incidenciaReparto;
    }

    public void setIncidenciaReparto(String incidenciaReparto) {
        this.incidenciaReparto = incidenciaReparto;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaUltimaModificacion() {
        return fechaUltimaModificacion;
    }

    public void setFechaUltimaModificacion(LocalDateTime fechaUltimaModificacion) {
        this.fechaUltimaModificacion = fechaUltimaModificacion;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(String modificadoPor) {
        this.modificadoPor = modificadoPor;
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

    public Set<LineaPedido> getLineaPedidos() {
        return lineaPedidos;
    }

    public void setLineaPedidos(Set<LineaPedido> lineaPedidos) {
        this.lineaPedidos = (lineaPedidos == null) ? new LinkedHashSet<>() : lineaPedidos;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Pago getPago() {
        return pago;
    }

    public void setPago(Pago pago) {
        this.pago = pago;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pedido other)) return false;
        return codigo != null && codigo.equals(other.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}
