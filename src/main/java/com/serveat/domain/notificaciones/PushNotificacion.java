package com.serveat.domain.notificaciones;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "push_notificaciones")
public class PushNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    @Column(nullable = false)
    private boolean leida = false;

    protected PushNotificacion() {}

    public PushNotificacion(String titulo, String mensaje) {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.creadaEn = LocalDateTime.now();
    }

    // GETTERS

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }
}

