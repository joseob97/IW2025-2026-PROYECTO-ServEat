package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
import com.serveat.repository.menu.CategoriaRepository;
import com.serveat.repository.menu.IngredienteRepository;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.service.menu.ProductoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@Transactional
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;
    private final IngredienteRepository ingredienteRepo;

    @Value("${app.upload.productos-path:uploads/images/productos}")
    private String uploadPath;

    @Value("${app.upload.productos-url:/images/productos}")
    private String publicUrl;

    public ProductoServiceImpl(ProductoRepository productoRepo,
                               CategoriaRepository categoriaRepo,
                               IngredienteRepository ingredienteRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
        this.ingredienteRepo = ingredienteRepo;
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
    public Producto crearProductoConIngredientes(
            String nombre,
            String descripcion,
            BigDecimal precio,
            String categoriaNombre,
            Set<String> ingredientesSeleccionados,
            byte[] imagenBytes,
            String nombreArchivo) {

        Categoria categoria = categoriaRepo.findByNombre(categoriaNombre)
                .orElseThrow(() -> new IllegalArgumentException("La categoría no existe"));

        Producto p = new Producto();
        p.setCodigo(generarCodigo());
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setCategoria(categoria);

        aplicarRecetaIngredientes(p, ingredientesSeleccionados);

        if (imagenBytes != null && nombreArchivo != null && !nombreArchivo.isBlank()) {
            guardarImagenProducto(p, imagenBytes, nombreArchivo);
        }

        return productoRepo.save(p);
    }

    @Override
    public Producto actualizarProductoConIngredientes(
            String codigo,
            String nombre,
            String descripcion,
            BigDecimal precio,
            String categoriaNombre,
            Set<String> ingredientesSeleccionados,
            byte[] imagenBytes,
            String nombreArchivo) {

        Producto p = productoRepo.findWithIngredientesByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        Categoria categoria = categoriaRepo.findByNombre(categoriaNombre)
                .orElseThrow(() -> new IllegalArgumentException("La categoría no existe"));

        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setCategoria(categoria);

        /* Reemplaza receta (orphanRemoval limpia la anterior) */
        p.getIngredientes().clear();
        aplicarRecetaIngredientes(p, ingredientesSeleccionados);

        /* Solo se reemplaza imagen si llega una nueva */
        if (imagenBytes != null && nombreArchivo != null && !nombreArchivo.isBlank()) {
            guardarImagenProducto(p, imagenBytes, nombreArchivo);
        }

        return productoRepo.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public Producto obtenerPorCodigo(String codigo) {
        return productoRepo.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public Producto obtenerConIngredientesPorCodigo(String codigo) {
        return productoRepo.findWithIngredientesByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        return productoRepo.findAll();
    }

    @Override
    public List<Producto> buscarPorCategoria(String categoriaNombre) {
        Categoria categoria = categoriaRepo.findByNombre(categoriaNombre)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        return productoRepo.findByCategoria(categoria);
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
        Producto p = productoRepo.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        productoRepo.delete(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listarNombresIngredientes() {
        return ingredienteRepo.findAll().stream()
                .map(Ingrediente::getNombre)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private void aplicarRecetaIngredientes(Producto p, Set<String> ingredientesSeleccionados) {
        Set<String> seleccion = (ingredientesSeleccionados == null) ? Set.of() : ingredientesSeleccionados;

        List<ProductoIngrediente> receta = new ArrayList<>();
        for (String nombreIng : seleccion) {
            if (nombreIng == null || nombreIng.isBlank()) continue;

            Ingrediente ing = ingredienteRepo.findByNombre(nombreIng)
                    .orElseThrow(() -> new IllegalArgumentException("Ingrediente no encontrado: " + nombreIng));

            receta.add(new ProductoIngrediente(
                    p,
                    ing,
                    true,
                    true,
                    ing.getPrecioExtra()
            ));
        }
        p.getIngredientes().addAll(receta);
    }

    private void guardarImagenProducto(Producto p, byte[] imagenBytes, String nombreArchivo) {
        try {
            Files.createDirectories(Paths.get(uploadPath));

            String extension = obtenerExtension(nombreArchivo);
            String fileName = p.getCodigo() + extension;

            Path destino = Paths.get(uploadPath, fileName);
            Files.write(destino, imagenBytes);

            p.setImagenUrl(publicUrl + "/" + fileName);

        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen del producto");
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        int dot = nombreArchivo.lastIndexOf('.');
        if (dot < 0) return "";
        return nombreArchivo.substring(dot).toLowerCase(Locale.ROOT);
    }


    @Override
    public boolean productoTieneIngredientes(String codigoProducto) {
        if (codigoProducto == null || codigoProducto.isBlank()) return false;
        return productoRepo.productoTieneIngredientes(codigoProducto);
    }
}