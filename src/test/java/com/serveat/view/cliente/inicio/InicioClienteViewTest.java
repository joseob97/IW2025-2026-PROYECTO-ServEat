package com.serveat.view.cliente.inicio;

import com.serveat.domain.menu.Producto;
import com.serveat.service.menu.ProductoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InicioClienteViewTest {

    @Mock
    private ProductoService productoService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void constructor_renderiza_saludo_con_username_y_bloques_principales_y_limita_a_4_destacados() {
        setAuthenticatedUser("pepe");

        List<Producto> productos = List.of(
                producto("P1", new BigDecimal("1.00"), null),
                producto("P2", new BigDecimal("2.00"), "/img/p2.jpg"),
                producto("P3", new BigDecimal("3.00"), "/img/p3.jpg"),
                producto("P4", new BigDecimal("4.00"), "/img/p4.jpg"),
                producto("P5", new BigDecimal("5.00"), "/img/p5.jpg")
        );

        when(productoService.buscarPorNombreParcial("")).thenReturn(productos);

        InicioClienteView view = new InicioClienteView(productoService);

        verify(productoService).buscarPorNombreParcial("");
        verifyNoMoreInteractions(productoService);

        H2 saludo = findFirst(view, H2.class).orElseThrow();
        assertThat(saludo.getText()).contains("Hola, pepe");

        Span subtitulo = findFirst(view, Span.class)
                .filter(s -> "¿Qué te apetece hoy?".equals(s.getText()))
                .orElseThrow();
        assertThat(subtitulo.getText()).isEqualTo("¿Qué te apetece hoy?");

        HorizontalLayout acciones = findHorizontalLayoutWithButtons(view, 2).orElseThrow();
        List<Button> botones = acciones.getChildren()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .toList();

        assertThat(botones).extracting(Button::getText)
                .containsExactly("🛍️ Hacer pedido", "📖 Ver carta");

        H3 tituloDestacados = findFirst(view, H3.class)
                .filter(h -> "Productos destacados".equals(h.getText()))
                .orElseThrow();
        assertThat(tituloDestacados.getText()).isEqualTo("Productos destacados");

        HorizontalLayout destacados = findHorizontalLayoutWithCards(view, 4).orElseThrow();
        assertThat(destacados.getChildren().count()).isEqualTo(4);

        VerticalLayout card1 = destacados.getChildren()
                .filter(VerticalLayout.class::isInstance)
                .map(VerticalLayout.class::cast)
                .findFirst()
                .orElseThrow();

        Image img1 = card1.getChildren()
                .filter(Image.class::isInstance)
                .map(Image.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(img1.getSrc()).isEqualTo("/images/producto-default.jpg");
        assertThat(img1.getAlt()).contains("P1"); // getAlt() es Optional<String>
    }

    @Test
    void constructor_si_servicio_devuelve_menos_de_4_crea_solo_esas_tarjetas() {
        setAuthenticatedUser("ana");

        List<Producto> productos = List.of(
                producto("Solo1", new BigDecimal("1.20"), null),
                producto("Solo2", new BigDecimal("2.50"), "/img/s2.jpg")
        );

        when(productoService.buscarPorNombreParcial("")).thenReturn(productos);

        InicioClienteView view = new InicioClienteView(productoService);

        verify(productoService).buscarPorNombreParcial("");
        verifyNoMoreInteractions(productoService);

        HorizontalLayout destacados = findHorizontalLayoutWithCards(view, 2).orElseThrow();
        assertThat(destacados.getChildren().count()).isEqualTo(2);
    }

    @Test
    void botones_no_fallan_al_no_haber_ui_adjunta() {
        setAuthenticatedUser("maria");

        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of());

        InicioClienteView view = new InicioClienteView(productoService);

        HorizontalLayout acciones = findHorizontalLayoutWithButtons(view, 2).orElseThrow();

        List<Button> botones = acciones.getChildren()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .toList();

        assertThatCode(() -> botones.forEach(Button::click)).doesNotThrowAnyException();
    }

    private static void setAuthenticatedUser(String username) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, "N/A", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static Producto producto(String nombre, BigDecimal precio, String imagenUrl) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setImagenUrl(imagenUrl);
        p.setCodigo("X-" + nombre);
        return p;
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

    private static Optional<HorizontalLayout> findHorizontalLayoutWithButtons(Component root, int expectedButtons) {
        return findAll(root, HorizontalLayout.class).stream()
                .filter(hl -> hl.getChildren().filter(Button.class::isInstance).count() == expectedButtons)
                .findFirst();
    }

    private static Optional<HorizontalLayout> findHorizontalLayoutWithCards(Component root, int expectedCards) {
        return findAll(root, HorizontalLayout.class).stream()
                .filter(hl -> hl.getChildren().filter(VerticalLayout.class::isInstance).count() == expectedCards)
                .findFirst();
    }

    private static <T extends Component> List<T> findAll(Component root, Class<T> type) {
        java.util.ArrayList<T> out = new java.util.ArrayList<>();
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