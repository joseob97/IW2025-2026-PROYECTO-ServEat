package com.serveat.domain.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "clientes",
        indexes = {
                @Index(name = "idx_cliente_email", columnList = "email"),
                @Index(name = "idx_cliente_username", columnList = "username"),
                @Index(name = "idx_cliente_activo", columnList = "activo")
        }
)
public class Cliente extends UsuarioBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -----------------------
    // ESTADO
    // -----------------------
    @Column(nullable = false)
    private boolean activo = true;

    // -----------------------
    // ROL (NO EDITABLE)
    // -----------------------
    @Column(nullable = false)
    private String rol = "CLIENTE";

    public Cliente() {}

    public Long getId() {
        return id;
    }

    public boolean isActivo() {
        return activo;
    }
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getRol() {
        return rol;
    }
    public void setRol(String rol) {
        this.rol = rol;
    }
}