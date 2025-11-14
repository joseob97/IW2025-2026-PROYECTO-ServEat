package com.serveat.service.menu;

import com.serveat.domain.menu.Categoria;
import java.util.List;

public interface CategoriaService {

    Categoria crearCategoria(String nombre);

    Categoria obtenerPorNombre(String nombre);

    List<Categoria> listarCategorias();
}