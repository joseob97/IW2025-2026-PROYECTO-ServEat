package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Categoria;
import com.serveat.repository.menu.CategoriaRepository;
import com.serveat.service.menu.CategoriaService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepo;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepo) {
        this.categoriaRepo = categoriaRepo;
    }

    @Override
    @CacheEvict(value = "categorias", allEntries = true)
    public Categoria crearCategoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return categoriaRepo.save(c);
    }

    @Override
    public Categoria obtenerPorNombre(String nombre) {
        return categoriaRepo.findByNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
    }

    @Override
    @Cacheable("categorias")
    public List<Categoria> listarCategorias() {
        return categoriaRepo.findAll();
    }
}
