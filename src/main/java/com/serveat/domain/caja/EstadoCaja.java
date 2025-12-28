package com.serveat.domain.caja;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ESTADOS_CAJA")
public class EstadoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEstadoCaja tipo;

    private String usuario;

    protected EstadoCaja() {}

    public EstadoCaja(TipoEstadoCaja tipo, String usuario) {
        this.fechaHora = LocalDateTime.now();
        this.tipo = tipo;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public TipoEstadoCaja getTipo() {
        return tipo;
    }

    public String getUsuario() {
        return usuario;
    }
}
