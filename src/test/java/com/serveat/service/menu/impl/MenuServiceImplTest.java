package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Menu;
import com.serveat.domain.seguridad.Feature;
import com.serveat.repository.menu.MenuRepository;
import com.serveat.service.seguridad.FeatureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private FeatureService featureService;

    @InjectMocks
    private MenuServiceImpl service;

    @Test
    void crearMenu_si_feature_no_activa_lanza_y_no_interactua_con_repo() {
        Menu menu = new Menu();
        menu.setNombre("Menu 1");
        menu.setPrecioFijo(new BigDecimal("12.50"));

        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(false);

        assertThatThrownBy(() -> service.crearMenu(menu))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La funcionalidad de menús y ofertas no está activa");

        verify(featureService).tieneFeature(Feature.MENUS_OFERTAS);
        verifyNoInteractions(menuRepository);
        verifyNoMoreInteractions(featureService);
    }

    @Test
    void crearMenu_si_feature_activa_guarda_y_devuelve() {
        Menu menu = new Menu();
        menu.setNombre("Menu 1");
        menu.setPrecioFijo(new BigDecimal("12.50"));

        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(true);
        when(menuRepository.save(menu)).thenReturn(menu);

        Menu res = service.crearMenu(menu);

        assertThat(res).isSameAs(menu);

        verify(featureService).tieneFeature(Feature.MENUS_OFERTAS);
        verify(menuRepository).save(menu);
        verifyNoMoreInteractions(featureService, menuRepository);
    }

    @Test
    void obtenerMenusActivos_si_feature_no_activa_lanza_y_no_interactua_con_repo() {
        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(false);

        assertThatThrownBy(() -> service.obtenerMenusActivos())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La funcionalidad de menús y ofertas no está activa");

        verify(featureService).tieneFeature(Feature.MENUS_OFERTAS);
        verifyNoInteractions(menuRepository);
        verifyNoMoreInteractions(featureService);
    }

    @Test
    void obtenerMenusActivos_si_feature_activa_devuelve_lista_del_repo() {
        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(true);

        Menu m1 = new Menu();
        m1.setNombre("A");
        m1.setPrecioFijo(new BigDecimal("10.00"));

        Menu m2 = new Menu();
        m2.setNombre("B");
        m2.setPrecioFijo(new BigDecimal("12.00"));

        when(menuRepository.findByActivoTrue()).thenReturn(List.of(m1, m2));

        List<Menu> res = service.obtenerMenusActivos();

        assertThat(res).containsExactly(m1, m2);

        verify(featureService).tieneFeature(Feature.MENUS_OFERTAS);
        verify(menuRepository).findByActivoTrue();
        verifyNoMoreInteractions(featureService, menuRepository);
    }

    @Test
    void obtenerMenusActivosConProductos_si_feature_no_activa_lanza_y_no_interactua_con_repo() {
        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(false);

        assertThatThrownBy(() -> service.obtenerMenusActivosConProductos())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La funcionalidad de menús y ofertas no está activa");

        verify(featureService).tieneFeature(Feature.MENUS_OFERTAS);
        verifyNoInteractions(menuRepository);
        verifyNoMoreInteractions(featureService);
    }

    @Test
    void obtenerMenusActivosConProductos_si_feature_activa_devuelve_lista_del_repo() {
        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(true);

        Menu m1 = new Menu();
        m1.setNombre("Menu con productos 1");
        m1.setPrecioFijo(new BigDecimal("15.00"));

        when(menuRepository.findMenusActivosConProductos()).thenReturn(List.of(m1));

        List<Menu> res = service.obtenerMenusActivosConProductos();

        assertThat(res).containsExactly(m1);

        verify(featureService).tieneFeature(Feature.MENUS_OFERTAS);
        verify(menuRepository).findMenusActivosConProductos();
        verifyNoMoreInteractions(featureService, menuRepository);
    }

    @Test
    void obtenerPorId_si_feature_no_activa_lanza_y_no_interactua_con_repo() {
        UUID id = UUID.randomUUID();
        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(false);

        assertThatThrownBy(() -> service.obtenerPorId(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La funcionalidad de menús y ofertas no está activa");

        verify(featureService).tieneFeature(Feature.MENUS_OFERTAS);
        verifyNoInteractions(menuRepository);
        verifyNoMoreInteractions(featureService);
    }

    @Test
    void obtenerPorId_si_no_existe_lanza_illegalArgument() {
        UUID id = UUID.randomUUID();
        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(true);
        when(menuRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Menú no encontrado");

        verify(featureService).tieneFeature(Feature.MENUS_OFERTAS);
        verify(menuRepository).findById(id);
        verifyNoMoreInteractions(featureService, menuRepository);
    }

    @Test
    void obtenerPorId_si_existe_devuelve_menu() {
        UUID id = UUID.randomUUID();

        Menu menu = new Menu();
        menu.setNombre("Menu X");
        menu.setPrecioFijo(new BigDecimal("9.99"));

        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(true);
        when(menuRepository.findById(id)).thenReturn(Optional.of(menu));

        Menu res = service.obtenerPorId(id);

        assertThat(res).isSameAs(menu);

        verify(featureService).tieneFeature(Feature.MENUS_OFERTAS);
        verify(menuRepository).findById(id);
        verifyNoMoreInteractions(featureService, menuRepository);
    }
}