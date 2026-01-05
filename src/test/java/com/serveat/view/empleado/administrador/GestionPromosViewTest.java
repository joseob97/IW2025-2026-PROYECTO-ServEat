package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestionPromosViewTest {

    @Test
    void constructor_no_revienta_y_con_feature_desactivada_muestra_textos_pro() {
        FeatureService featureService = mock(FeatureService.class);
        when(featureService.tieneFeature(Feature.PROMOCIONES)).thenReturn(false);

        GestionPromosView view = new GestionPromosView(featureService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.PROMOCIONES);

        assertNotNull(findH2ByText(view, "Gestión de promociones"));

        // El texto exacto en la vista usa "plan PRO." (sin "requiere el").
        assertNotNull(findParagraphContainingText(view, "plan PRO"));

        assertNotNull(findParagraphContainingText(view, "no está activado"));
        assertNull(findParagraphContainingText(view, "sprint siguiente"));
    }

    @Test
    void constructor_no_revienta_y_con_feature_activada_muestra_texto_sprint_siguiente() {
        FeatureService featureService = mock(FeatureService.class);
        when(featureService.tieneFeature(Feature.PROMOCIONES)).thenReturn(true);

        GestionPromosView view = new GestionPromosView(featureService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.PROMOCIONES);

        assertNotNull(findH2ByText(view, "Gestión de promociones"));
        assertNotNull(findParagraphContainingText(view, "sprint siguiente"));

        // Con feature activa, no deben aparecer los textos del plan PRO.
        assertNull(findParagraphContainingText(view, "plan PRO"));
    }

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

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        out.add(c);
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }
}