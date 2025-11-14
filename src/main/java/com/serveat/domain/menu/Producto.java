package com.serveat.domain.menu;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "PRODUCTOS")
public class Producto {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    @Column(unique = true, nullable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categorias;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public Categoria getCategoria() { return categorias; }
    public void setCategoria(Categoria categoria) { this.categorias = categorias; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}