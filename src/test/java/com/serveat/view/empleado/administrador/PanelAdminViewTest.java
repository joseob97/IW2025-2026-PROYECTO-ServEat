package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PanelAdminViewTest {

    @Test
    void constructor_no_revienta_y_consulta_features_para_ordenar_y_renderizar_premium() {
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(any())).thenReturn(false);

        PanelAdminView view = new PanelAdminView(featureService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.NOTIFICACIONES);
        verify(featureService, atLeastOnce()).tieneFeature(Feature.INGREDIENTES);
        verify(featureService, atLeastOnce()).tieneFeature(Feature.PROMOCIONES);
        verify(featureService, atLeastOnce()).tieneFeature(Feature.MENUS_OFERTAS);
        verify(featureService, atLeastOnce()).tieneFeature(Feature.ESTADISTICAS);
        verify(featureService, atLeastOnce()).tieneFeature(Feature.EXPORTAR_DATOS);

        assertNotNull(findH2ByText(view, "Panel de administración"));
    }

    @Test
    void constructor_con_alguna_feature_activa_sigue_creando_vista() {
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(any())).thenReturn(false);
        when(featureService.tieneFeature(Feature.ESTADISTICAS)).thenReturn(true);
        when(featureService.tieneFeature(Feature.EXPORTAR_DATOS)).thenReturn(true);

        PanelAdminView view = new PanelAdminView(featureService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.ESTADISTICAS);
        verify(featureService, atLeastOnce()).tieneFeature(Feature.EXPORTAR_DATOS);

        assertNotNull(findH2ByText(view, "Panel de administración"));
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

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        out.add(c);
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }
}