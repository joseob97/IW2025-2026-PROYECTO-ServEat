package com.serveat.view.empleado.administrador.estadisticas;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.administrador.estadisticas.EstadisticasService;
import com.serveat.service.caja.CierreCajaService;
import com.serveat.service.caja.EstadoCajaService;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CierreCajaViewTest {

    @Test
    void constructor_no_revienta_y_consulta_estado_caja() {
        EstadisticasService estadisticasService = mock(EstadisticasService.class);
        CierreCajaService cierreCajaService = mock(CierreCajaService.class);
        EstadoCajaService estadoCajaService = mock(EstadoCajaService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(Feature.CIERRE_CAJA)).thenReturn(false);
        when(estadoCajaService.isCajaAbierta()).thenReturn(true);

        CierreCajaView view = new CierreCajaView(
                estadisticasService,
                cierreCajaService,
                estadoCajaService,
                featureService
        );

        assertNotNull(view);

        verify(estadoCajaService, atLeastOnce()).isCajaAbierta();
        verify(featureService, atLeastOnce()).tieneFeature(Feature.CIERRE_CAJA);

        Button cerrar = findButtonByText(view, "Cerrar Caja");
        Button abrir = findButtonByText(view, "Abrir Caja");

        assertNotNull(cerrar);
        assertNotNull(abrir);

        assertTrue(cerrar.isVisible());
        assertFalse(abrir.isVisible());
    }

    @Test
    void constructor_con_caja_cerrada_muestra_boton_abrir() {
        EstadisticasService estadisticasService = mock(EstadisticasService.class);
        CierreCajaService cierreCajaService = mock(CierreCajaService.class);
        EstadoCajaService estadoCajaService = mock(EstadoCajaService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(Feature.CIERRE_CAJA)).thenReturn(false);
        when(estadoCajaService.isCajaAbierta()).thenReturn(false);

        CierreCajaView view = new CierreCajaView(
                estadisticasService,
                cierreCajaService,
                estadoCajaService,
                featureService
        );

        Button cerrar = findButtonByText(view, "Cerrar Caja");
        Button abrir = findButtonByText(view, "Abrir Caja");

        assertNotNull(cerrar);
        assertNotNull(abrir);

        assertFalse(cerrar.isVisible());
        assertTrue(abrir.isVisible());
    }

    // Helpers

    private static Button findButtonByText(VerticalLayout root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) {
                return b;
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