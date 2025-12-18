package com.serveat.domain.pago;

import com.serveat.domain.pedido.Pedido;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "PAGOS")
public class Pago {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    private MetodoPago metodo;

    private BigDecimal importe;

    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    private EstadoPago estado;

    private String referenciaProveedor;
}