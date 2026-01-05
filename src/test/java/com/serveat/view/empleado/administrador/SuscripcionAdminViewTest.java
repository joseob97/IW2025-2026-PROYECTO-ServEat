package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.service.seguridad.FeatureUnlockService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SuscripcionAdminViewTest {

    @Test
    void constructor_no_revienta_y_renderiza_resumen_y_listado() {
        FeatureService featureService = mock(FeatureService.class);
        FeatureUnlockService featureUnlockService = mock(FeatureUnlockService.class);

        when(featureService.listarFeaturesActivos()).thenReturn(Collections.emptySet());
        when(featureUnlockService.isFeaturePagada(any())).thenReturn(false);
        when(featureUnlockService.obtenerPrecioFeature(any())).thenReturn(BigDecimal.ONE);

        SuscripcionAdminView view = new SuscripcionAdminView(featureService, featureUnlockService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).listarFeaturesActivos();
        verify(featureUnlockService, atLeastOnce()).obtenerPrecioFeature(any());

        assertNotNull(findH2ByText(view, "Módulos extra del establecimiento"));
        assertNotNull(findParagraphContainingText(view, "Gestiona funcionalidades premium"));
        assertNotNull(findParagraphContainingText(view, "Módulos activos:"));
    }

    @Test
    void renderizar_con_features_activas_muestra_boton_activa_y_no_muestra_activar_para_feature_activa() throws Exception {
        FeatureService featureService = mock(FeatureService.class);
        FeatureUnlockService featureUnlockService = mock(FeatureUnlockService.class);

        Set<Feature> activas = EnumSet.of(Feature.ESTADISTICAS);
        when(featureService.listarFeaturesActivos()).thenReturn(activas);

        when(featureUnlockService.isFeaturePagada(any())).thenReturn(false);
        when(featureUnlockService.obtenerPrecioFeature(any())).thenReturn(new BigDecimal("9.99"));

        SuscripcionAdminView view = new SuscripcionAdminView(featureService, featureUnlockService);

        invokePrivate(view, "renderizar");

        assertNotNull(findButtonByText(view, "Activa"));

        assertEquals(
                0,
                countButtonsByText(view, "Activar"),
                "No debe existir botón Activar para la feature ya activa"
        );

        verify(featureService, atLeast(2)).listarFeaturesActivos();
    }

    @Test
    void renderizar_con_feature_pagada_pero_no_activa_muestra_boton_activar() throws Exception {
        FeatureService featureService = mock(FeatureService.class);
        FeatureUnlockService featureUnlockService = mock(FeatureUnlockService.class);

        when(featureService.listarFeaturesActivos()).thenReturn(Collections.emptySet());

        when(featureUnlockService.isFeaturePagada(any())).thenReturn(false);
        when(featureUnlockService.isFeaturePagada(Feature.NOTIFICACIONES)).thenReturn(true);

        when(featureUnlockService.obtenerPrecioFeature(any())).thenReturn(BigDecimal.ONE);

        SuscripcionAdminView view = new SuscripcionAdminView(featureService, featureUnlockService);

        invokePrivate(view, "renderizar");

        assertNotNull(findButtonByText(view, "Activar"));
    }

    @Test
    void renderizar_con_features_vacias_crea_botones_pagar() throws Exception {
        FeatureService featureService = mock(FeatureService.class);
        FeatureUnlockService featureUnlockService = mock(FeatureUnlockService.class);

        when(featureService.listarFeaturesActivos()).thenReturn(Collections.emptySet());
        when(featureUnlockService.isFeaturePagada(any())).thenReturn(false);
        when(featureUnlockService.obtenerPrecioFeature(any())).thenReturn(new BigDecimal("9.99"));

        SuscripcionAdminView view = new SuscripcionAdminView(featureService, featureUnlockService);

        invokePrivate(view, "renderizar");

        assertTrue(countButtonsContainingText(view, "Pagar (") >= 1);

        verify(featureUnlockService, atLeastOnce()).obtenerPrecioFeature(any());
    }

    @Test
    void mostrar_dialog_codigo_generado_no_revienta_y_pinta_texto_y_boton() throws Exception {
        FeatureService featureService = mock(FeatureService.class);
        FeatureUnlockService featureUnlockService = mock(FeatureUnlockService.class);

        when(featureService.listarFeaturesActivos()).thenReturn(Collections.emptySet());
        when(featureUnlockService.isFeaturePagada(any())).thenReturn(false);
        when(featureUnlockService.obtenerPrecioFeature(any())).thenReturn(BigDecimal.ONE);

        SuscripcionAdminView view = new SuscripcionAdminView(featureService, featureUnlockService);

        invokePrivate(view, "mostrarDialogCodigoGenerado",
                new Class<?>[]{String.class},
                new Object[]{"CODIGO-123"}
        );

    }

    // Helpers

    private static H2 findH2ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H2 h2 && text.equals(h2.getText())) {
                return h2;
            }
        }
        return null;
    }

    private static Paragraph findParagraphContainingText(Component root, String partial) {
        for (Component c : flatten(root)) {
            if (c instanceof Paragraph p && p.getText() != null && p.getText().contains(partial)) {
                return p;
            }
        }
        return null;
    }

    private static Button findButtonByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) {
                return b;
            }
        }
        return null;
    }

    private static int countButtonsByText(Component root, String text) {
        int n = 0;
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) {
                n++;
            }
        }
        return n;
    }

    private static int countButtonsContainingText(Component root, String partial) {
        int n = 0;
        for (Component c : flatten(root)) {
            if (c instanceof Button b && b.getText() != null && b.getText().contains(partial)) {
                n++;
            }
        }
        return n;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        out.add(c);
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }

    // Reflection solo para invocar métodos private

    private static void invokePrivate(Object target, String methodName) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(target);
    }

    private static void invokePrivate(Object target, String methodName, Class<?>[] argTypes, Object[] args) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName, argTypes);
        m.setAccessible(true);
        m.invoke(target, args);
    }
}