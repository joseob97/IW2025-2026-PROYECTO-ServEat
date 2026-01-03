package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Categoria;
import com.serveat.repository.menu.CategoriaRepository;
import com.serveat.service.menu.CategoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private static final Logger log = LoggerFactory.getLogger(CategoriaServiceImpl.class);

    private final CategoriaRepository categoriaRepo;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepo) {
        this.categoriaRepo = categoriaRepo;
    }

    @Override
    @CacheEvict(value = "categorias", allEntries = true)
    public Categoria crearCategoria(String nombre) {

        log.info("Creando categoría con nombre='{}'", nombre);

        Categoria c = new Categoria();
        c.setNombre(nombre);

        Categoria guardada = categoriaRepo.save(c);

        log.info("Categoría creada correctamente con nombre='{}'",
                guardada.getNombre());

        return guardada;
    }

    @Override
    public Categoria obtenerPorNombre(String nombre) {

        log.debug("Buscando categoría por nombre='{}'", nombre);

        return categoriaRepo.findByNombre(nombre)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada: '{}'", nombre);
                    return new IllegalArgumentException("Categoría no encontrada");
                });
    }

    @Override
    @Cacheable("categorias")
    public List<Categoria> listarCategorias() {

        log.debug("Listando todas las categorías");

        List<Categoria> categorias = categoriaRepo.findAll();

        log.debug("Se han recuperado {} categorías", categorias.size());

        return categorias;
    }
}
