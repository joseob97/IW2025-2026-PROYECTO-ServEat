package com.serveat.service;

import com.serveat.domain.menu.Plato;
import com.serveat.domain.menu.Categoria;

import java.util.List;
import java.util.UUID;

public interface PlatoService {

    /**
     * Lista todos los platos junto con su categoría asociada.
     */
    List<Plato> listarConCategoria();

    /**
     * Lista los platos filtrados por categoría.
     */
    List<Plato> listarPorCategoria(Categoria categoria);

    /**
     * Guarda o actualiza un plato en la base de datos.
     */
    Plato guardar(Plato plato);

    /**
     * Elimina un plato por su identificador UUID.
     */
    void eliminar(UUID id);
}