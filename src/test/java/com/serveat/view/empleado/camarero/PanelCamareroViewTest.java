package com.serveat.view.empleado.camarero;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PanelCamareroViewTest {

    @Test
    void constructor_no_revienta_y_titulo_presente() {
        PanelCamareroView view = new PanelCamareroView();

        assertNotNull(view);

        assertNotNull(findH2ByText(view, "Panel Camarero"));
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