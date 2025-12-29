package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Menu;
import com.serveat.domain.seguridad.Feature;
import com.serveat.repository.menu.MenuRepository;
import com.serveat.service.menu.MenuService;
import com.serveat.service.seguridad.FeatureService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final FeatureService featureService;

    public MenuServiceImpl(MenuRepository menuRepository,
                           FeatureService featureService) {
        this.menuRepository = menuRepository;
        this.featureService = featureService;
    }

    @Override
    public Menu crearMenu(Menu menu) {
        comprobarFeatureMenus();
        return menuRepository.save(menu);
    }

    @Override
    public List<Menu> obtenerMenusActivos() {
        comprobarFeatureMenus();
        return menuRepository.findByActivoTrue();
    }

    @Override
    public Menu obtenerPorId(UUID id) {
        comprobarFeatureMenus();
        return menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menú no encontrado"));
    }

    private void comprobarFeatureMenus() {
        if (!featureService.tieneFeature(Feature.MENUS_OFERTAS)) {
            throw new IllegalStateException(
                    "La funcionalidad de menús y ofertas no está activa"
            );
        }
    }
}
