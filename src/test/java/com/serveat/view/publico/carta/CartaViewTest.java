package com.serveat.view.publico.carta;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CartaViewTest {

    private ProductoService productoService;
    private CategoriaService categoriaService;

    @BeforeEach
    void setUp() {
        productoService = mock(ProductoService.class);
        categoriaService = mock(CategoriaService.class);

        Categoria catBebidas = new Categoria();
        catBebidas.setNombre("Bebidas");

        Categoria catComida = new Categoria();
        catComida.setNombre("Comida");

        when(categoriaService.listarCategorias()).thenReturn(List.of(catBebidas, catComida));
        when(productoService.buscarPorNombreParcial(anyString())).thenReturn(List.of());
        when(productoService.buscarPorCategoria(anyString())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void construye_ui_basica_y_configura_filtros() {
        CartaView view = new CartaView(productoService, categoriaService);

        H2 titulo = firstChildOfType(view, H2.class);
        assertNotNull(titulo);

        HorizontalLayout filtros = firstChildOfType(view, HorizontalLayout.class);
        assertNotNull(filtros);

        ComboBox<String> combo = firstChildOfType(filtros, ComboBox.class);
        assertNotNull(combo);
        assertTrue(combo.isClearButtonVisible());

        TextField buscador = firstChildOfType(filtros, TextField.class);
        assertNotNull(buscador);
        assertTrue(buscador.isClearButtonVisible());
        assertEquals(ValueChangeMode.EAGER, buscador.getValueChangeMode());

        assertNotNull(getDataProviderItems(combo));
        verify(categoriaService, times(1)).listarCategorias();
        verify(productoService, times(1)).buscarPorNombreParcial("");
    }

    @Test
    void si_no_hay_productos_muestra_mensaje_vacio() {
        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of());

        CartaView view = new CartaView(productoService, categoriaService);

        VerticalLayout contenido = findContenido(view);
        assertNotNull(contenido);

        assertTrue(contenido.getChildren().anyMatch(c -> c instanceof Span));
    }

    @Test
    void renderiza_productos_agrupados_por_categoria_y_ordenados_por_nombre() {
        Categoria bebidas = new Categoria();
        bebidas.setNombre("Bebidas");

        Categoria comida = new Categoria();
        comida.setNombre("Comida");

        Producto agua = new Producto();
        agua.setNombre("Agua");
        agua.setDescripcion("Botella 50cl");
        agua.setPrecio(new BigDecimal("1.50"));
        agua.setCategoria(bebidas);

        Producto cola = new Producto();
        cola.setNombre("Cola");
        cola.setDescripcion("Lata 33cl");
        cola.setPrecio(new BigDecimal("2.20"));
        cola.setCategoria(bebidas);

        Producto pizza = new Producto();
        pizza.setNombre("Pizza");
        pizza.setDescripcion("Margarita");
        pizza.setPrecio(new BigDecimal("9.90"));
        pizza.setCategoria(comida);

        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of(pizza, cola, agua));

        CartaView view = new CartaView(productoService, categoriaService);

        VerticalLayout contenido = findContenido(view);

        assertTrue(contenido.getChildren().anyMatch(c -> c instanceof H3 && ((H3) c).getText().equals("Bebidas")));
        assertTrue(contenido.getChildren().anyMatch(c -> c instanceof H3 && ((H3) c).getText().equals("Comida")));

        FlexLayout gridBebidas = gridDespuesDeCategoria(contenido, "Bebidas");
        assertNotNull(gridBebidas);

        List<Component> cardsBebidas = gridBebidas.getChildren().toList();
        assertEquals(2, cardsBebidas.size());

        String firstNombre = cardNombre(cardsBebidas.get(0));
        String secondNombre = cardNombre(cardsBebidas.get(1));
        assertEquals("Agua", firstNombre);
        assertEquals("Cola", secondNombre);
    }

    @Test
    void al_seleccionar_categoria_usa_buscarPorCategoria_y_filtra_por_texto() {
        Categoria bebidas = new Categoria();
        bebidas.setNombre("Bebidas");

        Producto cola = new Producto();
        cola.setNombre("Cola");
        cola.setCategoria(bebidas);

        Producto agua = new Producto();
        agua.setNombre("Agua");
        agua.setCategoria(bebidas);

        when(productoService.buscarPorCategoria("Bebidas")).thenReturn(List.of(cola, agua));
        when(productoService.buscarPorNombreParcial("co")).thenReturn(List.of());

        CartaView view = new CartaView(productoService, categoriaService);

        HorizontalLayout filtros = firstChildOfType(view, HorizontalLayout.class);
        ComboBox<String> combo = firstChildOfType(filtros, ComboBox.class);
        TextField buscador = firstChildOfType(filtros, TextField.class);

        buscador.setValue("co");
        combo.setValue("Bebidas");

        verify(productoService, atLeastOnce()).buscarPorCategoria("Bebidas");

        VerticalLayout contenido = findContenido(view);
        FlexLayout gridBebidas = gridDespuesDeCategoria(contenido, "Bebidas");
        assertNotNull(gridBebidas);

        List<Component> cards = gridBebidas.getChildren().toList();
        assertEquals(1, cards.size());
        assertEquals("Cola", cardNombre(cards.get(0)));
    }

    @Test
    void crearCardProducto_usa_placeholder_si_no_hay_imagen() {
        Producto p = new Producto();
        p.setNombre("Test");
        p.setDescripcion("Desc");
        p.setPrecio(new BigDecimal("3.00"));

        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of(p));

        CartaView view = new CartaView(productoService, categoriaService);

        VerticalLayout contenido = findContenido(view);

        FlexLayout gridOtros = firstGrid(contenido);
        assertNotNull(gridOtros);

        Component card = gridOtros.getChildren().findFirst().orElse(null);
        assertNotNull(card);
        assertTrue(card instanceof VerticalLayout);

        Image img = firstChildOfType((VerticalLayout) card, Image.class);
        assertNotNull(img);
        assertEquals("/images/productos/placeholder.png", img.getSrc());
    }

    private static <T extends Component> T firstChildOfType(Component parent, Class<T> type) {
        return parent.getChildren()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElse(null);
    }

    private static VerticalLayout findContenido(CartaView view) {
        List<Component> children = view.getChildren().toList();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i) instanceof VerticalLayout) {
                return (VerticalLayout) children.get(i);
            }
        }
        return null;
    }

    private static FlexLayout gridDespuesDeCategoria(VerticalLayout contenido, String nombreCategoria) {
        List<Component> children = contenido.getChildren().toList();
        for (int i = 0; i < children.size() - 1; i++) {
            if (children.get(i) instanceof H3 h3 && nombreCategoria.equals(h3.getText())) {
                Component next = children.get(i + 1);
                if (next instanceof FlexLayout) return (FlexLayout) next;
            }
        }
        return null;
    }

    private static FlexLayout firstGrid(VerticalLayout contenido) {
        return contenido.getChildren()
                .filter(c -> c instanceof FlexLayout)
                .map(c -> (FlexLayout) c)
                .findFirst()
                .orElse(null);
    }

    private static String cardNombre(Component card) {
        if (!(card instanceof VerticalLayout vl)) return null;
        return vl.getChildren()
                .filter(c -> c instanceof Span)
                .map(c -> ((Span) c).getText())
                .findFirst()
                .orElse(null);
    }

    private static List<String> getDataProviderItems(ComboBox<String> combo) {
        if (!(combo.getDataProvider() instanceof ListDataProvider<?> dp)) return null;
        return dp.getItems().stream().map(Object::toString).toList();
    }
}