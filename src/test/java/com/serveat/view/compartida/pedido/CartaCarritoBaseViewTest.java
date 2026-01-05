package com.serveat.view.compartida.pedido;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CartaCarritoBaseViewTest {

    private PedidoCarritoService carritoService;
    private PedidoCalculoService calculoService;
    private ProductoService productoService;
    private CategoriaService categoriaService;

    @BeforeEach
    void setUp() {
        carritoService = mock(PedidoCarritoService.class);
        calculoService = mock(PedidoCalculoService.class);
        productoService = mock(ProductoService.class);
        categoriaService = mock(CategoriaService.class);

        Categoria c1 = new Categoria();
        c1.setNombre("Bebidas");
        Categoria c2 = new Categoria();
        c2.setNombre("Comida");

        when(categoriaService.listarCategorias()).thenReturn(List.of(c1, c2));
        when(productoService.buscarPorNombreParcial(anyString())).thenReturn(List.of());
        when(productoService.buscarPorCategoria(anyString())).thenReturn(List.of());
        when(productoService.productoTieneIngredientes(anyString())).thenReturn(false);
        when(calculoService.calcularTotalPedido(any(Pedido.class))).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void construirCartaYCarrito_crea_layout_y_configura_grid() {
        TestView view = new TestView(carritoService, calculoService, productoService, categoriaService);
        Component main = view.construirCartaYCarrito(null);

        assertNotNull(main);
        assertTrue(main instanceof HorizontalLayout);

        assertTrue(view.filtroCategoria.isClearButtonVisible());
        assertTrue(view.buscador.isClearButtonVisible());

        Grid<LineaPedido> grid = view.gridCarrito;
        assertNotNull(grid);
        assertEquals(4, grid.getColumns().size());
    }

    @Test
    void cargarProductos_sin_personalizacion_no_consulta_productoTieneIngredientes() {
        Producto p1 = producto("P1", "Agua", "Bebidas");
        Producto p2 = producto("P2", "Pizza", "Comida");

        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of(p1, p2));

        TestView view = new TestView(carritoService, calculoService, productoService, categoriaService);
        view.setPersonalizacion(false);

        view.cargarProductos();

        verify(productoService, times(1)).buscarPorNombreParcial("");
        verify(productoService, never()).productoTieneIngredientes(anyString());

        assertTrue(view.contenido.getChildren().anyMatch(c -> c instanceof FlexLayout));
    }

    @Test
    void cargarProductos_con_personalizacion_consulta_productoTieneIngredientes_y_habilita_personalizar() {
        Producto p1 = producto("P1", "Agua", "Bebidas");

        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of(p1));
        when(productoService.productoTieneIngredientes("P1")).thenReturn(true);

        TestView view = new TestView(carritoService, calculoService, productoService, categoriaService);
        view.setPuedeInteractuar(true);
        view.setPersonalizacion(true);

        view.cargarProductos();

        verify(productoService, times(1)).productoTieneIngredientes("P1");

        Button personalizar = findButtonByText(view.contenido, "Personalizar");
        assertNotNull(personalizar);
        assertTrue(personalizar.isEnabled());
    }

    @Test
    void cargarProductos_con_personalizacion_y_sin_ingredientes_desactiva_personalizar() {
        Producto p1 = producto("P1", "Agua", "Bebidas");

        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of(p1));
        when(productoService.productoTieneIngredientes("P1")).thenReturn(false);

        TestView view = new TestView(carritoService, calculoService, productoService, categoriaService);
        view.setPuedeInteractuar(true);
        view.setPersonalizacion(true);

        view.cargarProductos();

        Button personalizar = findButtonByText(view.contenido, "Personalizar");
        assertNotNull(personalizar);
        assertFalse(personalizar.isEnabled());
    }

    @Test
    void refrescarCarrito_actualiza_total_y_llama_hook() {
        when(calculoService.calcularTotalPedido(any(Pedido.class))).thenReturn(new BigDecimal("12.34"));

        TestView view = new TestView(carritoService, calculoService, productoService, categoriaService);

        Producto prod = producto("P1", "Agua", "Bebidas");
        LineaPedido lp = mock(LineaPedido.class);
        when(lp.getCodigo()).thenReturn("L1");
        when(lp.getCantidad()).thenReturn(2);
        when(lp.getProducto()).thenReturn(prod);

        Pedido carrito = new Pedido();
        carrito.setLineaPedidos(new LinkedHashSet<>(List.of(lp)));
        view.carrito = carrito;

        view.refrescarCarrito();

        assertEquals("Total: 12.34 €", view.total.getText());
        assertEquals(1, view.onCarritoActualizadoCount);
    }

    @Test
    void setCartaEnabled_desactiva_controles_y_recarga_productos() {
        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of());

        TestView view = new TestView(carritoService, calculoService, productoService, categoriaService);

        view.setCartaEnabled(false);

        assertFalse(view.filtroCategoria.isEnabled());
        assertFalse(view.buscador.isEnabled());
        assertFalse(view.btnEditarCarrito.isEnabled());
        assertFalse(view.gridCarrito.isEnabled());

        verify(productoService, atLeastOnce()).buscarPorNombreParcial("");
    }

    private static Producto producto(String codigo, String nombre, String categoriaNombre) {
        Categoria cat = new Categoria();
        cat.setNombre(categoriaNombre);

        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setCategoria(cat);
        p.setPrecio(new BigDecimal("1.00"));
        return p;
    }

    private static Button findButtonByText(Component root, String text) {
        if (root instanceof Button b) {
            if (text.equals(b.getText())) return b;
        }
        for (Component child : root.getChildren().toList()) {
            Button found = findButtonByText(child, text);
            if (found != null) return found;
        }
        return null;
    }

    static final class TestView extends CartaCarritoBaseView {

        private boolean puede = true;
        private boolean personalizacion = false;

        int onCarritoActualizadoCount = 0;

        TestView(PedidoCarritoService carritoService,
                 PedidoCalculoService calculoService,
                 ProductoService productoService,
                 CategoriaService categoriaService) {
            super(carritoService, calculoService, productoService, categoriaService);

            if (carrito.getLineaPedidos() == null) {
                carrito.setLineaPedidos(new LinkedHashSet<>());
            }
        }

        void setPuedeInteractuar(boolean v) {
            this.puede = v;
        }

        void setPersonalizacion(boolean v) {
            this.personalizacion = v;
        }

        @Override
        protected boolean puedeInteractuarConCarta() {
            return puede;
        }

        @Override
        protected boolean personalizacionHabilitada() {
            return personalizacion;
        }

        @Override
        protected void onCarritoActualizado() {
            onCarritoActualizadoCount++;
        }
    }
}