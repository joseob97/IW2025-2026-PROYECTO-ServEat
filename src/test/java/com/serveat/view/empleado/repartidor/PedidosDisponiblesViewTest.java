package com.serveat.view.empleado.repartidor;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.repartidor.RepartidorService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PedidosDisponiblesViewTest {

    @BeforeEach
    void setup() {
        MockVaadin.setup();

        var auth = new UsernamePasswordAuthenticationToken(
                "repartidor1",
                "pass",
                List.of(new SimpleGrantedAuthority("ROLE_REPARTIDOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MockVaadin.tearDown();
    }

    @Test
    void vista_se_inicializa_y_carga_pedidos() {
        RepartidorService repartidorService = mock(RepartidorService.class);

        Pedido pedido = new Pedido();
        pedido.setCodigo("PED-001");
        pedido.setEstadoReparto(EstadoReparto.PENDIENTE_ASIGNACION);

        Page<Pedido> page = new PageImpl<>(
                List.of(pedido),
                PageRequest.of(0, 10),
                1
        );

        when(repartidorService.buscarPedidosDisponibles(any(), any(), any()))
                .thenReturn(page);

        PedidosDisponiblesView view = new PedidosDisponiblesView(repartidorService);
        UI.getCurrent().add(view);

        assertNotNull(findH3(view, "Pedidos disponibles"));

        Grid<?> grid = findGrid(view);
        assertNotNull(grid);
        assertEquals(1, grid.getDataProvider().size(new com.vaadin.flow.data.provider.Query<>()));

        verify(repartidorService, atLeastOnce())
                .buscarPedidosDisponibles(any(), any(), any());
    }

    // Helpers

    private static Grid<?> findGrid(Component root) {
        return flatten(root).stream()
                .filter(c -> c instanceof Grid)
                .map(c -> (Grid<?>) c)
                .findFirst()
                .orElse(null);
    }

    private static Button findButton(Component root, String text) {
        return flatten(root).stream()
                .filter(c -> c instanceof Button)
                .map(c -> (Button) c)
                .filter(b -> text.equals(b.getText()))
                .findFirst()
                .orElse(null);
    }

    private static H3 findH3(Component root, String text) {
        return flatten(root).stream()
                .filter(c -> c instanceof H3)
                .map(c -> (H3) c)
                .filter(h -> text.equals(h.getText()))
                .findFirst()
                .orElse(null);
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        if (c == null) return out;
        out.add(c);
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }
}