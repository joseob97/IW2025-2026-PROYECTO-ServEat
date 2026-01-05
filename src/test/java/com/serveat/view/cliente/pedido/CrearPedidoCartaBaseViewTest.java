package com.serveat.view.cliente.pedido;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrearPedidoCartaBaseViewTest {

    @Mock PedidoService pedidoService;
    @Mock PedidoCarritoService pedidoCarritoService;
    @Mock PedidoCalculoService pedidoCalculoService;
    @Mock ProductoService productoService;
    @Mock CategoriaService categoriaService;
    @Mock FeatureService featureService;

    private UI ui;

    @BeforeEach
    void setUp() {
        // Seguridad
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cliente1", "pw", List.of())
        );

        ui = new UI();
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        UI.setCurrent(null);
        ui = null;
    }

    // Subclase mínima para instanciar el abstract
    static class TestView extends CrearPedidoCartaBaseView {

        boolean continuarCalled = false;

        TestView(PedidoService pedidoService,
                 PedidoCarritoService pedidoCarritoService,
                 PedidoCalculoService pedidoCalculoService,
                 ProductoService productoService,
                 CategoriaService categoriaService,
                 FeatureService featureService) {
            super(pedidoService, pedidoCarritoService, pedidoCalculoService, productoService, categoriaService, featureService);
        }

        @Override
        protected Component construirBloqueDetalles() {
            return new Span("DETALLES_TEST");
        }

        @Override
        protected boolean puedeContinuar() {
            return carrito != null
                    && carrito.getLineaPedidos() != null
                    && !carrito.getLineaPedidos().isEmpty();
        }

        @Override
        protected void onContinuar() {
            continuarCalled = true;
        }
    }

    @Test
    void construirUI_featureIngredientes_off_no_llama_productoTieneIngredientes_y_renderiza_basico() {

        when(featureService.tieneFeature(Feature.INGREDIENTES)).thenReturn(false);

        when(categoriaService.listarCategorias()).thenReturn(List.of(
                categoria("Bebidas"),
                categoria("Comida")
        ));

        List<Producto> productos = List.of(
                producto("P1", "Coca-Cola", "Bebidas", new BigDecimal("2.00")),
                producto("P2", "Hamburguesa", "Comida", new BigDecimal("8.50"))
        );
        when(productoService.buscarPorNombreParcial("")).thenReturn(productos);

        TestView view = new TestView(pedidoService, pedidoCarritoService, pedidoCalculoService, productoService, categoriaService, featureService);
        ui.add(view);
        view.construirUI("Hacer pedido");

        H3 titulo = findAll(view, H3.class).stream()
                .filter(h -> "Hacer pedido".equals(h.getText()))
                .findFirst().orElseThrow();
        assertThat(titulo.getText()).isEqualTo("Hacer pedido");

        ComboBox<?> combo = findAll(view, ComboBox.class).stream().findFirst().orElseThrow();
        assertThat(combo.getLabel()).isEqualTo("Categoría");

        TextField buscador = findAll(view, TextField.class).stream().findFirst().orElseThrow();
        assertThat(buscador.getLabel()).isEqualTo("Buscar");

        verify(productoService).buscarPorNombreParcial("");
        verify(productoService, never()).productoTieneIngredientes(anyString());

        Span total = findAll(view, Span.class).stream()
                .filter(s -> s.getText() != null && s.getText().startsWith("Total:"))
                .findFirst().orElseThrow();
        assertThat(total.getText()).isEqualTo("Total: 0 €");

        Button continuar = findAll(view, Button.class).stream()
                .filter(b -> "➡ Continuar".equals(b.getText()))
                .findFirst().orElseThrow();
        assertThat(continuar.isEnabled()).isFalse();
    }

    @Test
    void construirUI_featureIngredientes_on_llama_productoTieneIngredientes_por_producto() {

        when(featureService.tieneFeature(Feature.INGREDIENTES)).thenReturn(true);

        when(categoriaService.listarCategorias()).thenReturn(List.of(
                categoria("Bebidas")
        ));

        Producto p1 = producto("P1", "Coca-Cola", "Bebidas", new BigDecimal("2.00"));
        Producto p2 = producto("P2", "Agua", "Bebidas", new BigDecimal("1.00"));

        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of(p1, p2));
        when(productoService.productoTieneIngredientes("P1")).thenReturn(true);
        when(productoService.productoTieneIngredientes("P2")).thenReturn(false);

        TestView view = new TestView(pedidoService, pedidoCarritoService, pedidoCalculoService, productoService, categoriaService, featureService);
        ui.add(view);
        view.construirUI("Hacer pedido");

        verify(featureService).tieneFeature(Feature.INGREDIENTES);
        verify(productoService).buscarPorNombreParcial("");
        verify(productoService).productoTieneIngredientes("P1");
        verify(productoService).productoTieneIngredientes("P2");
    }

    @Test
    void click_anadir_agrega_producto_refresca_total_y_habilita_continuar() {

        when(featureService.tieneFeature(Feature.INGREDIENTES)).thenReturn(false);
        when(categoriaService.listarCategorias()).thenReturn(List.of(categoria("Bebidas")));

        Producto coca = producto("P1", "Coca-Cola", "Bebidas", new BigDecimal("2.00"));
        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of(coca));

        Pedido pedidoConLinea = new Pedido();
        pedidoConLinea.setLineaPedidos(new LinkedHashSet<>());
        LineaPedido lp = new LineaPedido(pedidoConLinea, coca, 2);
        lp.setCodigo("LP1");
        pedidoConLinea.getLineaPedidos().add(lp);

        when(pedidoCarritoService.agregarProducto(any(Pedido.class), eq(coca), eq(2)))
                .thenReturn(pedidoConLinea);

        when(pedidoCalculoService.calcularTotalPedido(any(Pedido.class)))
                .thenReturn(new BigDecimal("4.00"));

        TestView view = new TestView(pedidoService, pedidoCarritoService, pedidoCalculoService, productoService, categoriaService, featureService);
        ui.add(view);
        view.construirUI("Hacer pedido");

        IntegerField qty = findAll(view, IntegerField.class).stream().findFirst().orElseThrow();
        qty.setValue(2);

        Button add = findAll(view, Button.class).stream()
                .filter(b -> "➕ Añadir".equals(b.getText()))
                .findFirst().orElseThrow();

        // ya hay UI current, así que Notification.show no rompe
        add.click();

        verify(pedidoCarritoService).agregarProducto(any(Pedido.class), eq(coca), eq(2));
        verify(pedidoCalculoService, atLeastOnce()).calcularTotalPedido(any(Pedido.class));

        Span total = findAll(view, Span.class).stream()
                .filter(s -> s.getText() != null && s.getText().startsWith("Total:"))
                .findFirst().orElseThrow();
        assertThat(total.getText()).isEqualTo("Total: 4.00 €");

        Button continuar = findAll(view, Button.class).stream()
                .filter(b -> "➡ Continuar".equals(b.getText()))
                .findFirst().orElseThrow();
        assertThat(continuar.isEnabled()).isTrue();
    }

    @Test
    void click_editar_carrito_toggle_texto() {

        when(featureService.tieneFeature(Feature.INGREDIENTES)).thenReturn(false);
        when(categoriaService.listarCategorias()).thenReturn(List.of());
        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of());

        TestView view = new TestView(pedidoService, pedidoCarritoService, pedidoCalculoService, productoService, categoriaService, featureService);
        ui.add(view);
        view.construirUI("Hacer pedido");

        Button editar = findAll(view, Button.class).stream()
                .filter(b -> "✏️ Editar carrito".equals(b.getText()))
                .findFirst().orElseThrow();

        editar.click();
        assertThat(editar.getText()).isEqualTo("✅ Listo");

        editar.click();
        assertThat(editar.getText()).isEqualTo("✏️ Editar carrito");
    }

    // Helpers

    private static Categoria categoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return c;
    }

    private static Producto producto(String codigo, String nombre, String categoriaNombre, BigDecimal precio) {
        Categoria cat = new Categoria();
        cat.setNombre(categoriaNombre);

        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setCategoria(cat);
        p.setPrecio(precio);
        p.setDescripcion("desc");
        return p;
    }

    private static void walk(Component root, java.util.function.Consumer<Component> consumer) {
        consumer.accept(root);
        root.getChildren().forEach(child -> walk(child, consumer));
    }

    private static <T extends Component> List<T> findAll(Component root, Class<T> type) {
        List<T> out = new ArrayList<>();
        walk(root, c -> {
            if (type.isInstance(c)) out.add(type.cast(c));
        });
        return out;
    }
}