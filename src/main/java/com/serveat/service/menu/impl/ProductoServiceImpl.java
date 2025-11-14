package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.repository.menu.CategoriaRepository;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.service.menu.ProductoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    public ProductoServiceImpl(ProductoRepository productoRepo,
                               CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    private String generarCodigo() {
        return "PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public Producto crearProducto(String nombre, String descripcion, BigDecimal precio, String categoriaNombre) {

        Categoria categoria = categoriaRepo.findByNombre(categoriaNombre)
                .orElseThrow(() -> new IllegalArgumentException("La categoría no existe"));

        Producto p = new Producto();
        p.setCodigo(generarCodigo());
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setCategoria(categoria);

        return productoRepo.save(p);
    }

    @Override
    public Producto obtenerPorCodigo(String codigo) {
        return productoRepo.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    @Override
    public List<Producto> buscarPorCategoria(String categoriaNombre) {
        Categoria categoria = categoriaRepo.findByNombre(categoriaNombre)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        return productoRepo.findByCategorias(categoria);
    }

    @Override
    public List<Producto> buscarPorNombreParcial(String nombre) {
        return productoRepo.findByNombreLike("%" + nombre + "%");
    }

    @Override
    public List<Producto> buscarPorDescripcionParcial(String descripcion) {
        return productoRepo.findByDescripcionLike("%" + descripcion + "%");
    }

    @Override
    public void eliminarProducto(String codigo) {
        Producto p = obtenerPorCodigo(codigo);
        productoRepo.delete(p);
    }
}