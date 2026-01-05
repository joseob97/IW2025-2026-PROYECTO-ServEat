package com.serveat.view.empleado.administrador.estadisticas;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.administrador.estadisticas.EstadisticasService;
import com.serveat.service.administrador.estadisticas.EstadisticasSnapshot;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstadisticasAdminViewTest {

    @Test
    void constructor_no_revienta_y_con_feature_activa_carga_datos_y_muestra_ui_basica() {
        FeatureService featureService = mock(FeatureService.class);
        EstadisticasService estadisticasService = mock(EstadisticasService.class);

        when(featureService.tieneFeature(Feature.ESTADISTICAS)).thenReturn(true);

        EstadisticasSnapshot snap = mock(EstadisticasSnapshot.class);
        when(snap.isHayDatos()).thenReturn(true);
        when(snap.getTotalPedidos()).thenReturn(10L);
        when(snap.getPedidosConfirmados()).thenReturn(7L);
        when(snap.getPedidosCancelados()).thenReturn(3L);
        when(snap.getPagosConfirmados()).thenReturn(6L);
        when(snap.getTotalFacturado()).thenReturn(new BigDecimal("12.34"));

        when(estadisticasService.snapshotRango(null, null)).thenReturn(snap);

        EstadisticasAdminView view = new EstadisticasAdminView(featureService, estadisticasService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.ESTADISTICAS);
        verify(estadisticasService, atLeastOnce()).snapshotRango(null, null);

        assertNotNull(findH2ByText(view, "Estadísticas"));
        assertNotNull(findButtonByText(view, "Buscar"));
        assertNotNull(findButtonByText(view, "Limpiar"));
        assertNotNull(findButtonByText(view, "📊 Ver gráficas"));
        assertNotNull(findButtonByText(view, "🔄 Refrescar (async)"));

        assertNotNull(findDatePickerByLabel(view, "Desde"));
        assertNotNull(findDatePickerByLabel(view, "Hasta"));

        assertNotNull(findSpanContainingText(view, "Mostrando: Global"));
    }

    @Test
    void constructor_con_feature_desactivada_muestra_bloqueo_y_no_llama_service() {
        FeatureService featureService = mock(FeatureService.class);
        EstadisticasService estadisticasService = mock(EstadisticasService.class);

        when(featureService.tieneFeature(Feature.ESTADISTICAS)).thenReturn(false);

        EstadisticasAdminView view = new EstadisticasAdminView(featureService, estadisticasService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.ESTADISTICAS);
        verifyNoInteractions(estadisticasService);

        assertNotNull(findH3ByText(view, "Funcionalidad no disponible"));
        assertNotNull(findSpanContainingText(view, "plan PRO"));

        assertNull(findButtonByText(view, "Buscar"));
        assertNull(findButtonByText(view, "Limpiar"));
    }

    @Test
    void constructor_con_feature_activa_y_sin_datos_muestra_mensaje_no_hay_datos() {
        FeatureService featureService = mock(FeatureService.class);
        EstadisticasService estadisticasService = mock(EstadisticasService.class);

        when(featureService.tieneFeature(Feature.ESTADISTICAS)).thenReturn(true);

        EstadisticasSnapshot snap = mock(EstadisticasSnapshot.class);
        when(snap.isHayDatos()).thenReturn(false);

        when(estadisticasService.snapshotRango(null, null)).thenReturn(snap);

        EstadisticasAdminView view = new EstadisticasAdminView(featureService, estadisticasService);

        assertNotNull(view);

        verify(estadisticasService, atLeastOnce()).snapshotRango(null, null);

        assertNotNull(findSpanByExactText(view, "No hay datos disponibles"));
    }

    @Test
    void cargar_con_rango_fechas_llama_service_con_parametros() throws Exception {
        FeatureService featureService = mock(FeatureService.class);
        EstadisticasService estadisticasService = mock(EstadisticasService.class);

        when(featureService.tieneFeature(Feature.ESTADISTICAS)).thenReturn(true);

        EstadisticasSnapshot snapInicial = mock(EstadisticasSnapshot.class);
        when(snapInicial.isHayDatos()).thenReturn(false);
        when(estadisticasService.snapshotRango(null, null)).thenReturn(snapInicial);

        EstadisticasAdminView view = new EstadisticasAdminView(featureService, estadisticasService);

        LocalDate desdeFecha = LocalDate.of(2024, 1, 1);
        LocalDate hastaFecha = LocalDate.of(2024, 1, 31);

        DatePicker desde = findDatePickerByLabel(view, "Desde");
        DatePicker hasta = findDatePickerByLabel(view, "Hasta");

        desde.setValue(desdeFecha);
        hasta.setValue(hastaFecha);

        EstadisticasSnapshot snapRango = mock(EstadisticasSnapshot.class);
        when(snapRango.isHayDatos()).thenReturn(false);
        when(estadisticasService.snapshotRango(desdeFecha, hastaFecha)).thenReturn(snapRango);

        invokePrivate(view, "cargar");

        verify(estadisticasService, atLeastOnce())
                .snapshotRango(desdeFecha, hastaFecha);

        assertNotNull(findSpanContainingText(view, "Mostrando: " + desdeFecha));
    }

    // Helpers

    private static Button findButtonByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) {
                return b;
            }
        }
        return null;
    }

    private static DatePicker findDatePickerByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof DatePicker dp && label.equals(dp.getLabel())) {
                return dp;
            }
        }
        return null;
    }

    private static H2 findH2ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H2 h2 && text.equals(h2.getText())) {
                return h2;
            }
        }
        return null;
    }

    private static H3 findH3ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H3 h3 && text.equals(h3.getText())) {
                return h3;
            }
        }
        return null;
    }

    private static Span findSpanByExactText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Span s && text.equals(s.getText())) {
                return s;
            }
        }
        return null;
    }

    private static Span findSpanContainingText(Component root, String partial) {
        for (Component c : flatten(root)) {
            if (c instanceof Span s && s.getText() != null && s.getText().contains(partial)) {
                return s;
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

    // Reflection solo para invocar cargar()

    private static void invokePrivate(Object target, String methodName) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(target);
    }
}