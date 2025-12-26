package com.serveat.service.menu;

import com.serveat.domain.menu.Ingrediente;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface IngredienteService {

    List<Ingrediente> listar();

    List<Ingrediente> buscarPorNombre(String query);

    Ingrediente crear(String nombre, BigDecimal precioExtra);

    Ingrediente actualizar(UUID id, String nombre, BigDecimal precioExtra);

    void eliminar(UUID id);
}