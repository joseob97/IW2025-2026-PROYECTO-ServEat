package com.serveat.service.menu;

import com.serveat.domain.menu.Menu;

import java.util.List;
import java.util.UUID;

public interface MenuService {

    Menu crearMenu(Menu menu);

    List<Menu> obtenerMenusActivos();

    Menu obtenerPorId(UUID id);
}
