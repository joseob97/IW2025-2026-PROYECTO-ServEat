package com.serveat.service.menu;

import com.serveat.domain.menu.Menu;

import java.util.List;
import java.util.UUID;

public interface MenuService {

    Menu crearMenu(Menu menu);

    // 🔹 Para ADMIN (no necesita productos)
    List<Menu> obtenerMenusActivos();

    // 🔹 PARA CLIENTE (con productos cargados)
    List<Menu> obtenerMenusActivosConProductos();

    Menu obtenerPorId(UUID id);
}
