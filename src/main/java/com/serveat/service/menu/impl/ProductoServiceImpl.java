package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
import com.serveat.repository.menu.CategoriaRepository;
import com.serveat.repository.menu.IngredienteRepository;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.service.menu.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    private static final Logger log = LoggerFactory.getLogger(ProductoServiceImpl.class);

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
        String codigo = "PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.debug("Generado código de producto {}", codigo);
        return codigo;
    }

    @Override
    @CacheEvict(value = {"productos", "productos_categoria"}, allEntries = true)
    public Producto crearProducto(String nombre, String descripcion, BigDecimal precio, String categoriaNombre) {

        log.info("Creando producto: nombre='{}', categoria='{}', precio={}", nombre, categoriaNombre, precio);

        Categoria categoria = categoriaRepo.findByNombre(categoriaNombre)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada: {}", categoriaNombre);
                    return new IllegalArgumentException("La categoría no existe");
                });

        Producto p = new Producto();
        p.setCodigo(generarCodigo());
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setCategoria(categoria);

        Producto guardado = productoRepo.save(p);

        log.info("Producto creado correctamente con código={}", guardado.getCodigo());
        return guardado;
    }

    @Override
    @CacheEvict(value = {"productos", "productos_categoria"}, allEntries = true)
    public Producto crearProductoConIngredientes(
            String nombre,
            String descripcion,
            BigDecimal precio,
            String categoriaNombre,
            Set<String> ingredientesSeleccionados,
            byte[] imagenBytes,
            String nombreArchivo) {

        log.info("Creando producto con ingredientes: nombre='{}', categoria='{}'", nombre, categoriaNombre);

        Categoria categoria = categoriaRepo.findByNombre(categoriaNombre)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada: {}", categoriaNombre);
                    return new IllegalArgumentException("La categoría no existe");
                });

        Producto p = new Producto();
        p.setCodigo(generarCodigo());
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setCategoria(categoria);

        aplicarRecetaIngredientes(p, ingredientesSeleccionados);

        if (imagenBytes != null && nombreArchivo != null && !nombreArchivo.isBlank()) {
            log.debug("Guardando imagen para producto {}", p.getCodigo());
            guardarImagenProducto(p, imagenBytes, nombreArchivo);
        }

        Producto guardado = productoRepo.save(p);

        log.info("Producto con ingredientes creado correctamente: codigo={}", guardado.getCodigo());
        return guardado;
    }

    @Override
    @CacheEvict(value = {"productos", "productos_categoria"}, allEntries = true)
    public Producto actualizarProductoConIngredientes(
            String codigo,
            String nombre,
            String descripcion,
            BigDecimal precio,
            String categoriaNombre,
            Set<String> ingredientesSeleccionados,
            byte[] imagenBytes,
            String nombreArchivo) {

        log.info("Actualizando producto: codigo={}", codigo);

        Producto p = productoRepo.findWithIngredientesByCodigo(codigo)
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado para actualizar: {}", codigo);
                    return new IllegalArgumentException("Producto no encontrado");
                });

        Categoria categoria = categoriaRepo.findByNombre(categoriaNombre)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada: {}", categoriaNombre);
                    return new IllegalArgumentException("La categoría no existe");
                });

        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setCategoria(categoria);

        p.getIngredientes().clear();
        aplicarRecetaIngredientes(p, ingredientesSeleccionados);

        if (imagenBytes != null && nombreArchivo != null && !nombreArchivo.isBlank()) {
            log.debug("Reemplazando imagen del producto {}", codigo);
            guardarImagenProducto(p, imagenBytes, nombreArchivo);
        }

        Producto guardado = productoRepo.save(p);

        log.info("Producto actualizado correctamente: codigo={}", guardado.getCodigo());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Producto obtenerPorCodigo(String codigo) {
        log.debug("Obteniendo producto por código={}", codigo);
        return productoRepo.findByCodigo(codigo)
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado: {}", codigo);
                    return new IllegalArgumentException("Producto no encontrado");
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Producto obtenerConIngredientesPorCodigo(String codigo) {
        log.debug("Obteniendo producto con ingredientes por código={}", codigo);
        return productoRepo.findWithIngredientesByCodigo(codigo)
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado: {}", codigo);
                    return new IllegalArgumentException("Producto no encontrado");
                });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("productos")
    public List<Producto> listarProductos() {
        log.debug("Listando todos los productos");
        return productoRepo.findAll();
    }

    @Override
    @Cacheable(value = "productos_categoria", key = "#categoriaNombre")
    public List<Producto> buscarPorCategoria(String categoriaNombre) {
        log.debug("Buscando productos por categoría={}", categoriaNombre);

        Categoria categoria = categoriaRepo.findByNombre(categoriaNombre)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada: {}", categoriaNombre);
                    return new IllegalArgumentException("Categoría no encontrada");
                });

        return productoRepo.findByCategoria(categoria);
    }

    @Override
    public List<Producto> buscarPorNombreParcial(String nombre) {
        log.debug("Buscando productos por nombre parcial='{}'", nombre);
        return productoRepo.findByNombreLike("%" + nombre + "%");
    }

    @Override
    public List<Producto> buscarPorDescripcionParcial(String descripcion) {
        log.debug("Buscando productos por descripción parcial='{}'", descripcion);
        return productoRepo.findByDescripcionLike("%" + descripcion + "%");
    }

    @Override
    @CacheEvict(value = {"productos", "productos_categoria"}, allEntries = true)
    public void eliminarProducto(String codigo) {
        log.info("Eliminando producto codigo={}", codigo);

        Producto p = productoRepo.findByCodigo(codigo)
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado para eliminar: {}", codigo);
                    return new IllegalArgumentException("Producto no encontrado");
                });

        productoRepo.delete(p);

        log.info("Producto eliminado correctamente: codigo={}", codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listarNombresIngredientes() {
        log.debug("Listando nombres de ingredientes");
        return ingredienteRepo.findAll().stream()
                .map(Ingrediente::getNombre)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private void aplicarRecetaIngredientes(Producto p, Set<String> ingredientesSeleccionados) {
        Set<String> seleccion = (ingredientesSeleccionados == null) ? Set.of() : ingredientesSeleccionados;

        for (String nombreIng : seleccion) {
            if (nombreIng == null || nombreIng.isBlank()) continue;

            Ingrediente ing = ingredienteRepo.findByNombre(nombreIng)
                    .orElseThrow(() -> {
                        log.warn("Ingrediente no encontrado: {}", nombreIng);
                        return new IllegalArgumentException("Ingrediente no encontrado: " + nombreIng);
                    });

            p.getIngredientes().add(new ProductoIngrediente(
                    p,
                    ing,
                    true,
                    true,
                    ing.getPrecioExtra()
            ));
        }
    }

    private void guardarImagenProducto(Producto p, byte[] imagenBytes, String nombreArchivo) {
        try {
            Files.createDirectories(Paths.get(uploadPath));

            String extension = obtenerExtension(nombreArchivo);
            String fileName = p.getCodigo() + extension;

            Path destino = Paths.get(uploadPath, fileName);
            Files.write(destino, imagenBytes);

            p.setImagenUrl(publicUrl + "/" + fileName);

            log.info("Imagen guardada para producto {} en {}", p.getCodigo(), destino);

        } catch (IOException e) {
            log.error("Error guardando imagen del producto {}", p.getCodigo(), e);
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
        if (codigoProducto == null || codigoProducto.isBlank()) {
            log.debug("productoTieneIngredientes: código inválido");
            return false;
        }
        return productoRepo.productoTieneIngredientes(codigoProducto);
    }
}
