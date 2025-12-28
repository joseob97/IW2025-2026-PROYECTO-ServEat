package com.serveat.domain.notificaciones;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones_admin")
public class NotificacionAdmin {

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

    protected NotificacionAdmin() {}

    public NotificacionAdmin(String titulo, String mensaje) {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.creadaEn = LocalDateTime.now();
    }

    // GETTERS & SETTERS

    public Long getId() { return id; }

    public String getTitulo() { return titulo; }

    public String getMensaje() { return mensaje; }

    public LocalDateTime getCreadaEn() { return creadaEn; }

    public boolean isLeida() { return leida; }

    public void setLeida(boolean leida) { this.leida = leida; }
}

