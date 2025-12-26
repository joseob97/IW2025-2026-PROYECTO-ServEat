package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.repository.menu.IngredienteRepository;
import com.serveat.service.menu.IngredienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class IngredienteServiceImpl implements IngredienteService {

    private final IngredienteRepository ingredienteRepo;

    public IngredienteServiceImpl(IngredienteRepository ingredienteRepo) {
        this.ingredienteRepo = ingredienteRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ingrediente> listar() {
        return ingredienteRepo.findAll().stream()
                .sorted((a, b) -> {
                    String na = a.getNombre() == null ? "" : a.getNombre();
                    String nb = b.getNombre() == null ? "" : b.getNombre();
                    return na.compareToIgnoreCase(nb);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ingrediente> buscarPorNombre(String query) {
        if (query == null || query.isBlank()) {
            return listar();
        }
        return ingredienteRepo.findByNombreContainingIgnoreCaseOrderByNombreAsc(query.trim());
    }

    @Override
    public Ingrediente crear(String nombre, BigDecimal precioExtra) {
        validarNombre(nombre);

        ingredienteRepo.findByNombre(nombre.trim())
                .ifPresent(i -> { throw new IllegalArgumentException("Ya existe un ingrediente con ese nombre"); });

        Ingrediente ing = new Ingrediente();
        ing.setNombre(nombre.trim());
        ing.setPrecioExtra(precioExtra == null ? BigDecimal.ZERO : precioExtra);

        return ingredienteRepo.save(ing);
    }

    @Override
    public Ingrediente actualizar(UUID id, String nombre, BigDecimal precioExtra) {
        if (id == null) throw new IllegalArgumentException("Id inválido");
        validarNombre(nombre);

        Ingrediente actual = ingredienteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingrediente no encontrado"));

        String nuevoNombre = nombre.trim();

        ingredienteRepo.findByNombre(nuevoNombre).ifPresent(otro -> {
            if (!otro.getId().equals(actual.getId())) {
                throw new IllegalArgumentException("Ya existe un ingrediente con ese nombre");
            }
        });

        actual.setNombre(nuevoNombre);
        actual.setPrecioExtra(precioExtra == null ? BigDecimal.ZERO : precioExtra);

        return ingredienteRepo.save(actual);
    }

    @Override
    public void eliminar(UUID id) {
        if (id == null) throw new IllegalArgumentException("Id inválido");

        if (!ingredienteRepo.existsById(id)) {
            throw new IllegalArgumentException("Ingrediente no encontrado");
        }

        ingredienteRepo.deleteById(id);
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (nombre.trim().length() > 60) {
            throw new IllegalArgumentException("Nombre demasiado largo (máx 60)");
        }
    }
}