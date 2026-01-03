package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Menu;
import com.serveat.domain.seguridad.Feature;
import com.serveat.repository.menu.MenuRepository;
import com.serveat.service.menu.MenuService;
import com.serveat.service.seguridad.FeatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MenuServiceImpl implements MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuServiceImpl.class);

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

        log.info(
                "Creando menú: nombre='{}', precio={}, productos={}",
                menu.getNombre(),
                menu.getPrecioFijo(),
                menu.getProductos() != null ? menu.getProductos().size() : 0
        );

        Menu guardado = menuRepository.save(menu);

        log.info("Menú creado correctamente con ID={}", guardado.getId());
        return guardado;
    }

    // Usado por ADMIN (no necesita productos)
    @Override
    public List<Menu> obtenerMenusActivos() {
        comprobarFeatureMenus();

        log.debug("Consultando menús activos (ADMIN)");

        List<Menu> menus = menuRepository.findByActivoTrue();

        log.debug("Se han recuperado {} menús activos", menus.size());
        return menus;
    }

    // Usado por CLIENTE (con productos cargados)
    @Override
    public List<Menu> obtenerMenusActivosConProductos() {
        comprobarFeatureMenus();

        log.debug("Consultando menús activos con productos (CLIENTE)");

        List<Menu> menus = menuRepository.findMenusActivosConProductos();

        log.debug("Se han recuperado {} menús activos con productos", menus.size());
        return menus;
    }

    @Override
    public Menu obtenerPorId(UUID id) {
        comprobarFeatureMenus();

        log.debug("Buscando menú por ID={}", id);

        return menuRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Menú no encontrado con ID={}", id);
                    return new IllegalArgumentException("Menú no encontrado");
                });
    }

    private void comprobarFeatureMenus() {
        if (!featureService.tieneFeature(Feature.MENUS_OFERTAS)) {
            log.warn("Acceso bloqueado a MENUS_OFERTAS: feature no activa");
            throw new IllegalStateException(
                    "La funcionalidad de menús y ofertas no está activa"
            );
        }
    }
}
