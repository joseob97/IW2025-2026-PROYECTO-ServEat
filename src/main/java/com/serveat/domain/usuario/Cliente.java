package com.serveat.domain.usuario;

import com.serveat.domain.comun.BaseEntity;
import jakarta.persistence.Entity;

@Entity
public class Cliente extends BaseEntity {

    private String nombre;
    private String email;

    public Cliente() {}

    public Cliente(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
