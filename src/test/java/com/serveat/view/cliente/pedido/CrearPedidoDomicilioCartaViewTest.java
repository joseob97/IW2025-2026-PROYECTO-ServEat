package com.serveat.view.cliente.pedido;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrearPedidoDomicilioCartaViewTest {

    @Mock PedidoService pedidoService;
    @Mock PedidoCarritoService pedidoCarritoService;
    @Mock PedidoCalculoService pedidoCalculoService;
    @Mock ProductoService productoService;
    @Mock CategoriaService categoriaService;
    @Mock FeatureService featureService;

    private TestUI ui;
    private VaadinSession session;

    @BeforeEach
    void setUp() {
        // Autenticacion necesaria para usernameActual
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cliente1", "pw", List.of())
        );

        // Sesion simulada para verificar setAttribute y evitar dependencias reales de Vaadin
        session = mock(VaadinSession.class);

        // UI actual para que getUI de la vista devuelva un valor presente
        ui = new TestUI(session);
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        UI.setCurrent(null);
        ui = null;
        session = null;
    }

    @Test
    void constructor_renderiza_titulo_y_bloque_detalles_con_direccion_e_info() {
        when(featureService.tieneFeature(Feature.INGREDIENTES)).thenReturn(false);
        when(categoriaService.listarCategorias()).thenReturn(List.of(categoria("Bebidas")));
        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of());

        CrearPedidoDomicilioCartaView view = new CrearPedidoDomicilioCartaView(
                pedidoService,
                pedidoCarritoService,
                pedidoCalculoService,
                productoService,
                categoriaService,
                featureService
        );

        // Adjuntar a UI para flujo normal de Vaadin
        ui.add(view);

        H3 titulo = findAll(view, H3.class).stream()
                .filter(h -> "Pedido a domicilio".equals(h.getText()))
                .findFirst()
                .orElseThrow();

        assertThat(titulo.getText()).isEqualTo("Pedido a domicilio");

        TextField direccion = findAll(view, TextField.class).stream()
                .filter(tf -> "Dirección de entrega".equals(tf.getLabel()))
                .findFirst()
                .orElseThrow();

        assertThat(direccion.isRequired()).isTrue();
        assertThat(direccion.getPlaceholder()).isEqualTo("Calle, número, piso...");

        Span info = findAll(view, Span.class).stream()
                .filter(s -> "Métodos de pago: Tarjeta / PayPal / Efectivo.".equals(s.getText()))
                .findFirst()
                .orElseThrow();

        assertThat(info.getText()).isEqualTo("Métodos de pago: Tarjeta / PayPal / Efectivo.");

        Button continuar = findAll(view, Button.class).stream()
                .filter(b -> "➡ Continuar".equals(b.getText()))
                .findFirst()
                .orElseThrow();

        assertThat(continuar.isEnabled()).isFalse();
    }

    @Test
    void continuar_se_habilita_solo_si_hay_lineas_y_direccion_no_esta_vacia() {
        when(featureService.tieneFeature(Feature.INGREDIENTES)).thenReturn(false);
        when(categoriaService.listarCategorias()).thenReturn(List.of());
        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of());
        when(pedidoCalculoService.calcularTotalPedido(any(Pedido.class))).thenReturn(new BigDecimal("5.00"));

        CrearPedidoDomicilioCartaView view = new CrearPedidoDomicilioCartaView(
                pedidoService,
                pedidoCarritoService,
                pedidoCalculoService,
                productoService,
                categoriaService,
                featureService
        );

        ui.add(view);

        Button continuar = findAll(view, Button.class).stream()
                .filter(b -> "➡ Continuar".equals(b.getText()))
                .findFirst()
                .orElseThrow();

        TextField direccion = findAll(view, TextField.class).stream()
                .filter(tf -> "Dirección de entrega".equals(tf.getLabel()))
                .findFirst()
                .orElseThrow();

        assertThat(continuar.isEnabled()).isFalse();

        Pedido carrito = pedidoConUnaLinea();
        ReflectionTestUtils.setField(view, "carrito", carrito);

        direccion.setValue(" ");
        view.refrescarCarrito();
        assertThat(continuar.isEnabled()).isFalse();

        direccion.setValue("Calle Real 1");
        view.refrescarCarrito();
        assertThat(continuar.isEnabled()).isTrue();
    }

    @Test
    void onContinuar_con_direccion_vacia_no_navega_y_no_escribe_atributos_de_pedido_en_sesion() {
        when(featureService.tieneFeature(Feature.INGREDIENTES)).thenReturn(false);
        when(categoriaService.listarCategorias()).thenReturn(List.of());
        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of());

        CrearPedidoDomicilioCartaView view = new CrearPedidoDomicilioCartaView(
                pedidoService,
                pedidoCarritoService,
                pedidoCalculoService,
                productoService,
                categoriaService,
                featureService
        );

        ui.add(view);

        TextField direccion = findAll(view, TextField.class).stream()
                .filter(tf -> "Dirección de entrega".equals(tf.getLabel()))
                .findFirst()
                .orElseThrow();

        direccion.setValue(" ");

        ReflectionTestUtils.invokeMethod(view, "onContinuar");

        assertThat(ui.getLastNavigation()).isNull();

        // No se deben escribir atributos funcionales del pedido cuando la direccion es invalida
        verify(session, never()).setAttribute(eq("pedidoOnlineCarrito"), any());
        verify(session, never()).setAttribute(eq("pedidoOnlineTipo"), any());
        verify(session, never()).setAttribute(eq("pedidoOnlineDireccion"), any());
        verify(session, never()).setAttribute(eq("pedidoOnlineUsername"), any());
    }

    @Test
    void onContinuar_con_direccion_valida_escribe_en_sesion_y_navega() {
        when(featureService.tieneFeature(Feature.INGREDIENTES)).thenReturn(false);
        when(categoriaService.listarCategorias()).thenReturn(List.of());
        when(productoService.buscarPorNombreParcial("")).thenReturn(List.of());

        CrearPedidoDomicilioCartaView view = new CrearPedidoDomicilioCartaView(
                pedidoService,
                pedidoCarritoService,
                pedidoCalculoService,
                productoService,
                categoriaService,
                featureService
        );

        ui.add(view);

        Pedido carrito = pedidoConUnaLinea();
        ReflectionTestUtils.setField(view, "carrito", carrito);

        TextField direccion = findAll(view, TextField.class).stream()
                .filter(tf -> "Dirección de entrega".equals(tf.getLabel()))
                .findFirst()
                .orElseThrow();

        direccion.setValue("  Calle Mayor 10  ");

        ReflectionTestUtils.invokeMethod(view, "onContinuar");

        assertThat(ui.getLastNavigation()).isEqualTo("cliente/pedido/online/pasarela");

        verify(session).setAttribute("pedidoOnlineCarrito", carrito);
        verify(session).setAttribute("pedidoOnlineTipo", TipoPedidoCliente.DOMICILIO);

        ArgumentCaptor<Object> captorDireccion = ArgumentCaptor.forClass(Object.class);
        verify(session).setAttribute(eq("pedidoOnlineDireccion"), captorDireccion.capture());
        assertThat(String.valueOf(captorDireccion.getValue())).isEqualTo("Calle Mayor 10");

        verify(session).setAttribute("pedidoOnlineUsername", "cliente1");
    }

    // UI de prueba que captura navegacion y devuelve una sesion simulada
    static final class TestUI extends UI {
        private final VaadinSession session;
        private String lastNavigation;

        TestUI(VaadinSession session) {
            this.session = session;
        }

        @Override
        public VaadinSession getSession() {
            return session;
        }

        @Override
        public void navigate(String location) {
            lastNavigation = location;
        }

        String getLastNavigation() {
            return lastNavigation;
        }
    }

    private static Categoria categoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return c;
    }

    private static Pedido pedidoConUnaLinea() {
        Categoria cat = new Categoria();
        cat.setNombre("Bebidas");

        Producto p = new Producto();
        p.setCodigo("P1");
        p.setNombre("Agua");
        p.setCategoria(cat);
        p.setPrecio(new BigDecimal("1.00"));

        Pedido pedido = new Pedido();
        pedido.setLineaPedidos(new LinkedHashSet<>());

        LineaPedido lp = new LineaPedido(pedido, p, 1);
        lp.setCodigo("LP1");
        pedido.getLineaPedidos().add(lp);

        return pedido;
    }

    private static void walk(Component root, java.util.function.Consumer<Component> consumer) {
        consumer.accept(root);
        root.getChildren().forEach(child -> walk(child, consumer));
    }

    private static <T extends Component> List<T> findAll(Component root, Class<T> type) {
        List<T> out = new ArrayList<>();
        walk(root, c -> {
            if (type.isInstance(c)) {
                out.add(type.cast(c));
            }
        });
        return out;
    }
}