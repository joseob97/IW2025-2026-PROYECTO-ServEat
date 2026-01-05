package com.serveat.view.empleado.camarero;

import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditarPedidoViewTest {

    @Test
    void constructor_no_revienta_y_estado_inicial_bloqueado() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        EditarPedidoView view = new EditarPedidoView(pedidoService, calculoService);

        assertNotNull(view);

        assertNotNull(findH3ByText(view, "Editar pedido (Camarero)"));

        Button confirmar = findButtonByText(view, "✅ Confirmar cambios");
        Button volver = findButtonByText(view, "← Volver a pedidos");
        assertNotNull(confirmar);
        assertNotNull(volver);

        assertFalse(confirmar.isEnabled());

        Grid<?> grid = findFirstGrid(view);
        assertNotNull(grid);
        assertFalse(grid.isEnabled());

        Span info = findFirstSpan(view);
        assertNotNull(info);
        assertNotNull(info.getText());
    }

    @Test
    void set_parameter_con_codigo_nulo_limpia_vista_y_deja_edicion_bloqueada() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        EditarPedidoView view = new EditarPedidoView(pedidoService, calculoService);

        BeforeEvent event = mock(BeforeEvent.class);

        view.setParameter(event, null);

        Grid<?> grid = findFirstGrid(view);
        assertNotNull(grid);
        assertFalse(grid.isEnabled());

        Button confirmar = findButtonByText(view, "✅ Confirmar cambios");
        assertNotNull(confirmar);
        assertFalse(confirmar.isEnabled());

        Span info = findFirstSpan(view);
        assertNotNull(info);
        assertTrue(info.getText().contains("No se ha indicado código de pedido."));
    }

    @Test
    void set_parameter_con_codigo_en_blanco_limpia_vista_y_no_llama_servicio() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        EditarPedidoView view = new EditarPedidoView(pedidoService, calculoService);

        BeforeEvent event = mock(BeforeEvent.class);

        view.setParameter(event, "   ");

        verifyNoInteractions(pedidoService);

        Grid<?> grid = findFirstGrid(view);
        assertNotNull(grid);
        assertFalse(grid.isEnabled());

        Span info = findFirstSpan(view);
        assertNotNull(info);
        assertTrue(info.getText().contains("No se ha indicado código de pedido."));
    }

    // Helpers

    private static H3 findH3ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H3 h3 && text.equals(h3.getText())) {
                return h3;
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

    private static Grid<?> findFirstGrid(Component root) {
        for (Component c : flatten(root)) {
            if (c instanceof Grid<?> g) {
                return g;
            }
        }
        return null;
    }

    private static Span findFirstSpan(Component root) {
        for (Component c : flatten(root)) {
            if (c instanceof Span s) {
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
}