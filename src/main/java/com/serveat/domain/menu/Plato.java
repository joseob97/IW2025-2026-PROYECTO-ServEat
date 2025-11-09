package com.serveat.domain.menu;

import com.serveat.domain.comun.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class Plato extends BaseEntity {

    @Column(nullable = false)
    private String nombre;

    @Column(name = "imagen")
    private String imagen;

    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Categoria categoria;

    protected Plato() { }

    public Plato(String nombre, String descripcion, BigDecimal precio, Categoria categoria) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.categoria = categoria;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public String getImagen() { return imagen; }
    public void setImagen(String imageUrl) { this.imagen = imageUrl; }
}