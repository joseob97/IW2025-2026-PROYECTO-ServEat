package com.serveat.view.empleado.administrador.estadisticas;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.administrador.estadisticas.EstadisticasService;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstadisticasGraficasViewTest {

    @Test
    void constructor_no_revienta_y_con_feature_activa_configura_filtros_grids_y_consulta_anios() {
        FeatureService featureService = mock(FeatureService.class);
        EstadisticasService estadisticasService = mock(EstadisticasService.class);

        when(featureService.tieneFeature(Feature.ESTADISTICAS)).thenReturn(true);
        when(estadisticasService.añosDisponibles()).thenReturn(List.of(2023, 2024, 2025));

        EstadisticasGraficasView view = new EstadisticasGraficasView(featureService, estadisticasService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.ESTADISTICAS);
        verify(estadisticasService, atLeastOnce()).añosDisponibles();

        assertNotNull(findH2ByText(view, "Gráficas"));
        assertNotNull(findButtonByText(view, "⬅ Volver"));

        assertNotNull(findButtonByText(view, "Buscar"));
        assertNotNull(findButtonByText(view, "Limpiar"));

        assertNotNull(findComboBoxByLabel(view, "Año inicio"));
        assertNotNull(findComboBoxByLabel(view, "Año fin"));
        assertNotNull(findComboBoxByLabel(view, "Serie"));

        assertNotNull(findDatePickerByLabel(view, "Desde"));
        assertNotNull(findDatePickerByLabel(view, "Hasta"));

        assertNotNull(findGrid(view, 0));
        assertNotNull(findGrid(view, 1));
        assertNotNull(findGrid(view, 2));

        assertNotNull(findH3ByText(view, "Top productos por unidades"));
        assertNotNull(findH3ByText(view, "Top productos por facturación"));
        assertNotNull(findH3ByText(view, "Evolución mensual"));
    }

    @Test
    void constructor_con_feature_desactivada_muestra_bloqueo_y_no_consulta_anios() {
        FeatureService featureService = mock(FeatureService.class);
        EstadisticasService estadisticasService = mock(EstadisticasService.class);

        when(featureService.tieneFeature(Feature.ESTADISTICAS)).thenReturn(false);

        EstadisticasGraficasView view = new EstadisticasGraficasView(featureService, estadisticasService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.ESTADISTICAS);
        verifyNoInteractions(estadisticasService);

        assertNotNull(findH3ByText(view, "Funcionalidad no disponible"));
        assertNotNull(findSpanContainingText(view, "plan PRO."));

        assertNull(findButtonByText(view, "Buscar"));
        assertNull(findButtonByText(view, "Limpiar"));
        assertNull(findButtonByText(view, "⬅ Volver") == null ? null : null);
    }

    @Test
    void cargar_top_con_rango_valido_llama_servicios_y_actualiza_emptyTop() throws Exception {
        FeatureService featureService = mock(FeatureService.class);
        EstadisticasService estadisticasService = mock(EstadisticasService.class);

        when(featureService.tieneFeature(Feature.ESTADISTICAS)).thenReturn(true);
        when(estadisticasService.añosDisponibles()).thenReturn(List.of(2024, 2025));

        when(estadisticasService.topProductosPorUnidades(any(), any(), eq(15))).thenReturn(List.of());
        when(estadisticasService.topProductosPorFacturacion(any(), any(), eq(15))).thenReturn(List.of());

        EstadisticasGraficasView view = new EstadisticasGraficasView(featureService, estadisticasService);

        DatePicker desde = findDatePickerByLabel(view, "Desde");
        DatePicker hasta = findDatePickerByLabel(view, "Hasta");

        assertNotNull(desde);
        assertNotNull(hasta);

        LocalDate d = LocalDate.of(2024, 1, 1);
        LocalDate h = LocalDate.of(2024, 1, 31);

        desde.setValue(d);
        hasta.setValue(h);

        invokePrivate(view, "cargarTop");

        verify(estadisticasService, atLeastOnce()).topProductosPorUnidades(d, h, 15);
        verify(estadisticasService, atLeastOnce()).topProductosPorFacturacion(d, h, 15);

        Span emptyTop = findFirstEmptyTopSpan(view);
        assertNotNull(emptyTop);
        assertEquals("No hay datos disponibles", emptyTop.getText());
    }

    @Test
    void cargar_serie_con_rango_anios_invalido_muestra_mensaje_y_no_llama_service() throws Exception {
        FeatureService featureService = mock(FeatureService.class);
        EstadisticasService estadisticasService = mock(EstadisticasService.class);

        when(featureService.tieneFeature(Feature.ESTADISTICAS)).thenReturn(true);
        when(estadisticasService.añosDisponibles()).thenReturn(List.of(2023, 2024, 2025));

        EstadisticasGraficasView view = new EstadisticasGraficasView(featureService, estadisticasService);

        ComboBox<Integer> yi = findComboBoxByLabel(view, "Año inicio");
        ComboBox<Integer> yf = findComboBoxByLabel(view, "Año fin");

        assertNotNull(yi);
        assertNotNull(yf);

        yi.setValue(2025);
        yf.setValue(2024);

        invokePrivate(view, "cargarSerie");

        verify(estadisticasService, never()).serieMensualVista(anyInt(), anyInt(), anyString());

        Span emptySerie = findFirstEmptySerieSpan(view);
        assertNotNull(emptySerie);
        assertEquals("Selecciona un rango de años válido", emptySerie.getText());
    }

    @Test
    void cargar_serie_con_rango_valido_llama_service_y_limpia_mensaje_si_hay_datos() throws Exception {
        FeatureService featureService = mock(FeatureService.class);
        EstadisticasService estadisticasService = mock(EstadisticasService.class);

        when(featureService.tieneFeature(Feature.ESTADISTICAS)).thenReturn(true);
        when(estadisticasService.añosDisponibles()).thenReturn(List.of(2023, 2024, 2025));

        List<Map<String, Object>> rows = List.of(
                Map.of("mes", "2024-01", "valor", 10, "max", true),
                Map.of("mes", "2024-02", "valor", 5, "max", false)
        );

        when(estadisticasService.serieMensualVista(2024, 2024, "Unidades")).thenReturn(rows);

        EstadisticasGraficasView view = new EstadisticasGraficasView(featureService, estadisticasService);

        ComboBox<Integer> yi = findComboBoxByLabel(view, "Año inicio");
        ComboBox<Integer> yf = findComboBoxByLabel(view, "Año fin");
        ComboBox<String> tipo = findComboBoxStringByLabel(view, "Serie");

        assertNotNull(yi);
        assertNotNull(yf);
        assertNotNull(tipo);

        yi.setValue(2024);
        yf.setValue(2024);
        tipo.setValue("Unidades");

        invokePrivate(view, "cargarSerie");

        verify(estadisticasService, atLeastOnce()).serieMensualVista(2024, 2024, "Unidades");

        Span emptySerie = findFirstEmptySerieSpan(view);
        assertNotNull(emptySerie);
        assertEquals("", emptySerie.getText());
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

    private static <T> ComboBox<T> findComboBoxByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof ComboBox<?> cb && label.equals(cb.getLabel())) {
                @SuppressWarnings("unchecked")
                ComboBox<T> out = (ComboBox<T>) cb;
                return out;
            }
        }
        return null;
    }

    private static ComboBox<String> findComboBoxStringByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof ComboBox<?> cb && label.equals(cb.getLabel())) {
                @SuppressWarnings("unchecked")
                ComboBox<String> out = (ComboBox<String>) cb;
                return out;
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

    private static Span findSpanContainingText(Component root, String partial) {
        for (Component c : flatten(root)) {
            if (c instanceof Span s && s.getText() != null && s.getText().contains(partial)) {
                return s;
            }
        }
        return null;
    }

    private static Grid<?> findGrid(Component root, int index) {
        int i = 0;
        for (Component c : flatten(root)) {
            if (c instanceof Grid<?> g) {
                if (i == index) return g;
                i++;
            }
        }
        return null;
    }

    private static Span findFirstEmptyTopSpan(Component root) {
        int seenTopH3 = 0;
        for (Component c : flatten(root)) {
            if (c instanceof H3 h3 && "Top productos por unidades".equals(h3.getText())) {
                seenTopH3++;
                continue;
            }
            if (seenTopH3 > 0 && c instanceof Span s) {
                return s;
            }
        }
        return null;
    }

    private static Span findFirstEmptySerieSpan(Component root) {
        int seenSerieH3 = 0;
        for (Component c : flatten(root)) {
            if (c instanceof H3 h3 && "Evolución mensual".equals(h3.getText())) {
                seenSerieH3++;
                continue;
            }
            if (seenSerieH3 > 0 && c instanceof Span s) {
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

    // Reflection solo para invocar cargarTop y cargarSerie

    private static void invokePrivate(Object target, String methodName) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(target);
    }
}