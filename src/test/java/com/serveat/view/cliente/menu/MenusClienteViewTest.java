package com.serveat.view.cliente.menu;

import com.serveat.domain.menu.Menu;
import com.serveat.domain.menu.Producto;
import com.serveat.service.menu.MenuService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenusClienteViewTest {

    @Mock
    private MenuService menuService;

    @Test
    void constructor_si_no_hay_menus_muestra_mensaje_y_no_pinta_cards() {
        when(menuService.obtenerMenusActivosConProductos()).thenReturn(List.of());

        MenusClienteView view = new MenusClienteView(menuService);

        verify(menuService).obtenerMenusActivosConProductos();
        verifyNoMoreInteractions(menuService);

        H2 titulo = findFirst(view, H2.class).orElseThrow();
        assertThat(titulo.getText()).isEqualTo("Menús y ofertas");

        Paragraph msg = findFirst(view, Paragraph.class)
                .filter(p -> "No hay menús disponibles actualmente.".equals(p.getText()))
                .orElseThrow();

        assertThat(msg.getText()).isEqualTo("No hay menús disponibles actualmente.");

        // En este caso solo deberían existir el H2 y el mensaje
        assertThat(view.getChildren().count()).isEqualTo(2);
    }

    @Test
    void constructor_con_menus_pinta_una_card_por_menu_con_datos_y_productos() {
        Menu m1 = menu("Menu 1", "Desc 1", new BigDecimal("9.90"),
                List.of(producto("P1"), producto("P2")));
        Menu m2 = menu("Menu 2", "Desc 2", new BigDecimal("12.50"),
                List.of(producto("X1")));

        when(menuService.obtenerMenusActivosConProductos()).thenReturn(List.of(m1, m2));

        MenusClienteView view = new MenusClienteView(menuService);

        verify(menuService).obtenerMenusActivosConProductos();
        verifyNoMoreInteractions(menuService);

        H2 titulo = findFirst(view, H2.class).orElseThrow();
        assertThat(titulo.getText()).isEqualTo("Menús y ofertas");

        // 2 cards (VerticalLayout) además del título
        List<VerticalLayout> cards = findAll(view, VerticalLayout.class).stream()
                .filter(v -> v != view) // excluir el propio root
                .toList();

        assertThat(cards).hasSize(2);

        // Card 1: estructura y textos
        VerticalLayout card1 = cards.get(0);

        H3 card1Title = findFirst(card1, H3.class).orElseThrow();
        assertThat(card1Title.getText()).isEqualTo("Menu 1");

        assertThat(findParagraphTexts(card1)).containsExactlyInAnyOrder(
                "Desc 1",
                "Precio: 9.90 €",
                "Incluye:",
                "• P1",
                "• P2"
        );

        // Card 2
        VerticalLayout card2 = cards.get(1);

        H3 card2Title = findFirst(card2, H3.class).orElseThrow();
        assertThat(card2Title.getText()).isEqualTo("Menu 2");

        assertThat(findParagraphTexts(card2)).containsExactlyInAnyOrder(
                "Desc 2",
                "Precio: 12.50 €",
                "Incluye:",
                "• X1"
        );
    }

    private static List<String> findParagraphTexts(Component root) {
        return findAll(root, Paragraph.class).stream()
                .map(Paragraph::getText)
                .toList();
    }

    private static Producto producto(String nombre) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setCodigo("C-" + nombre);
        return p;
    }

    private static Menu menu(String nombre, String descripcion, BigDecimal precio, List<Producto> productos) {
        Menu m = new Menu();
        m.setNombre(nombre);
        m.setDescripcion(descripcion);
        m.setPrecioFijo(precio);
        m.setProductos(productos);
        m.setActivo(true);
        return m;
    }

    private static <T extends Component> Optional<T> findFirst(Component root, Class<T> type) {
        if (type.isInstance(root)) {
            return Optional.of(type.cast(root));
        }
        return root.getChildren()
                .map(child -> findFirst(child, type))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private static <T extends Component> List<T> findAll(Component root, Class<T> type) {
        ArrayList<T> out = new ArrayList<>();
        collectAll(root, type, out);
        return out;
    }

    private static <T extends Component> void collectAll(Component root, Class<T> type, List<T> out) {
        if (type.isInstance(root)) {
            out.add(type.cast(root));
        }
        root.getChildren().forEach(child -> collectAll(child, type, out));
    }
}