package com.serveat.view.empleado.administrador;

import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.Anchor;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExportarDatosViewTest {

    @Test
    void constructor_no_revienta_y_con_feature_desactivada_muestra_textos_y_no_llama_pedido_service() {
        FeatureService featureService = mock(FeatureService.class);
        PedidoService pedidoService = mock(PedidoService.class);

        when(featureService.tieneFeature(Feature.EXPORTAR_DATOS)).thenReturn(false);

        ExportarDatosView view = new ExportarDatosView(featureService, pedidoService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.EXPORTAR_DATOS);
        verifyNoInteractions(pedidoService);

        assertNotNull(findH2ByText(view, "Exportar datos"));
        assertNotNull(findParagraphContainingText(view, "plan PRO"));
        assertNotNull(findParagraphContainingText(view, "Suscripción / Plan"));

        assertNull(findButtonByText(view, "Descargar CSV"));
        assertNull(findButtonByText(view, "Descargar PDF"));
    }

    @Test
    void constructor_no_revienta_y_con_feature_activada_muestra_botones_y_anchors() {
        FeatureService featureService = mock(FeatureService.class);
        PedidoService pedidoService = mock(PedidoService.class);

        when(featureService.tieneFeature(Feature.EXPORTAR_DATOS)).thenReturn(true);

        ExportarDatosView view = new ExportarDatosView(featureService, pedidoService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.EXPORTAR_DATOS);

        assertNotNull(findH2ByText(view, "Exportar datos"));
        assertNotNull(findParagraphContainingText(view, "Descarga el listado completo"));

        Button csv = findButtonByText(view, "Descargar CSV");
        Button pdf = findButtonByText(view, "Descargar PDF");

        assertNotNull(csv);
        assertNotNull(pdf);

        assertTrue(countAnchors(view) >= 2);
    }

    @Test
    void generar_csv_llama_service_y_generar_cabecera() throws Exception {
        FeatureService featureService = mock(FeatureService.class);
        PedidoService pedidoService = mock(PedidoService.class);

        when(featureService.tieneFeature(Feature.EXPORTAR_DATOS)).thenReturn(true);

        Pedido p = mock(Pedido.class);
        when(p.getCodigo()).thenReturn("P-1");
        when(p.getFechaCreacion()).thenReturn(LocalDateTime.of(2025, 1, 1, 10, 30));

        when(p.getEstado()).thenReturn(null);
        when(p.getTipoPedido()).thenReturn(null);
        when(p.getCliente()).thenReturn(null);
        when(p.calcularPrecioTotal()).thenReturn(null);

        when(pedidoService.listarTodosOrdenadosPorFecha()).thenReturn(List.of(p));

        ExportarDatosView view = new ExportarDatosView(featureService, pedidoService);

        InputStream is = invokePrivateAndReturnStream(view, "generarCsvPedidos");
        assertNotNull(is);

        String csv = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        verify(pedidoService, atLeastOnce()).listarTodosOrdenadosPorFecha();

        assertTrue(csv.startsWith("Codigo;Fecha;Estado;Tipo;Cliente;Total"));
        assertTrue(csv.contains("P-1;"));
    }

    @Test
    void generar_pdf_llama_service_y_devuelve_stream_no_nulo() throws Exception {
        FeatureService featureService = mock(FeatureService.class);
        PedidoService pedidoService = mock(PedidoService.class);

        when(featureService.tieneFeature(Feature.EXPORTAR_DATOS)).thenReturn(true);
        when(pedidoService.listarTodosOrdenadosPorFecha()).thenReturn(Collections.emptyList());

        ExportarDatosView view = new ExportarDatosView(featureService, pedidoService);

        InputStream is = invokePrivateAndReturnStream(view, "generarPdfPedidos");
        assertNotNull(is);

        verify(pedidoService, atLeastOnce()).listarTodosOrdenadosPorFecha();

        byte[] bytes = is.readAllBytes();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
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

    private static Button findButtonByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) {
                return b;
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

    private static int countAnchors(Component root) {
        int n = 0;
        for (Component c : flatten(root)) {
            if (c instanceof Anchor) n++;
        }
        return n;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        out.add(c);
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }

    private static InputStream invokePrivateAndReturnStream(Object target, String methodName) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        Object out = m.invoke(target);
        assertTrue(out instanceof InputStream);
        return (InputStream) out;
    }
}