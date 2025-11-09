package com.serveat.service.impl;

import com.serveat.domain.menu.Plato;
import com.serveat.domain.menu.Categoria;
import com.serveat.repository.PlatoRepository;
import com.serveat.service.PlatoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PlatoServiceImpl implements PlatoService {

    private final PlatoRepository platoRepository;

    public PlatoServiceImpl(PlatoRepository platoRepository) {
        this.platoRepository = platoRepository;
    }

    @Override
    public List<Plato> listarConCategoria() {
        return platoRepository.findAll(); // usa @EntityGraph en el repositorio
    }

    @Override
    public List<Plato> listarPorCategoria(Categoria categoria) {
        return platoRepository.findByCategoriaOrderByNombreAsc(categoria);
    }

    @Override
    public Plato guardar(Plato plato) {
        return platoRepository.save(plato);
    }

    @Override
    public void eliminar(UUID id) {
        platoRepository.deleteById(id);
    }
}