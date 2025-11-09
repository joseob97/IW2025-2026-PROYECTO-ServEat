package com.serveat.domain.menu;

import com.serveat.domain.comun.BaseEntity;
import jakarta.persistence.*;

@Entity
public class Categoria extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String nombre;

    protected Categoria() {}
    public Categoria(String nombre) { this.nombre = nombre; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}