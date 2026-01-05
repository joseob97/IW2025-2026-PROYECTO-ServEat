package com.serveat.view.cliente.pedido;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.pedido.TicketService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaPedidosViewTest {

    @Mock
    private PedidoService pedidoService;

    @Mock
    private PedidoCalculoService pedidoCalculoService;

    @Mock
    private TicketService ticketService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void constructor_carga_pedidos_del_cliente_muestra_info_paginacion_y_limita_a_10() {
        setAuth("cliente1");

        List<Pedido> pedidos = new ArrayList<>();

        Pedido e1 = pedido("E1", LocalDateTime.now().minusMinutes(1), EstadoPedido.EN_COCINA, EstadoCocina.ACEPTADO, TipoPedidoCliente.MESA, 3);
        Pedido e2 = pedido("E2", LocalDateTime.now().minusMinutes(5), EstadoPedido.EN_COCINA, EstadoCocina.PENDIENTE_ACEPTACION, TipoPedidoCliente.RECOGER, null);

        Pedido n1 = pedido("N1", LocalDateTime.now().minusMinutes(2), EstadoPedido.EN_CURSO, EstadoCocina.PENDIENTE_ACEPTACION, TipoPedidoCliente.DOMICILIO, null);
        Pedido n2 = pedido("N2", LocalDateTime.now().minusMinutes(3), EstadoPedido.ANULADO, EstadoCocina.CANCELADO, TipoPedidoCliente.MESA, 7);

        pedidos.add(e1);
        pedidos.add(e2);
        pedidos.add(n1);
        pedidos.add(n2);

        for (int i = 3; i <= 12; i++) {
            pedidos.add(pedido("N" + i, LocalDateTime.now().minusMinutes(10 + i), EstadoPedido.EN_COCINA, EstadoCocina.EN_PREPARACION, TipoPedidoCliente.RECOGER, null));
        }

        when(pedidoService.listarPedidosCliente("cliente1")).thenReturn(pedidos);

        when(pedidoService.puedeModificarCliente(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            return p != null && p.getCodigo() != null && p.getCodigo().startsWith("E");
        });

        ConsultaPedidosView view = new ConsultaPedidosView(pedidoService, pedidoCalculoService, ticketService);

        verify(pedidoService).listarPedidosCliente("cliente1");
        verify(pedidoService, atLeastOnce()).puedeModificarCliente(any(Pedido.class));
        verifyNoInteractions(ticketService);

        H3 titulo = findFirst(view, H3.class).orElseThrow();
        assertThat(titulo.getText()).isEqualTo("Mis pedidos");

        Span infoPagina = findAll(view, Span.class).stream()
                .filter(s -> s.getText() != null && s.getText().startsWith("Mostrando "))
                .findFirst()
                .orElseThrow();

        assertThat(infoPagina.getText()).isEqualTo("Mostrando 1-10 de 14");

        Grid<Pedido> gridPedidos = findFirst(view, Grid.class).orElseThrow();
        List<Pedido> pagina = fetchGridItems(gridPedidos);

        assertThat(pagina).hasSize(10);

        // Orden esperado: primero editables, y dentro por fecha desc
        assertThat(pagina.get(0).getCodigo()).isEqualTo("E1");
        assertThat(pagina.get(1).getCodigo()).isEqualTo("E2");

        List<String> resto = pagina.subList(2, pagina.size())
                .stream().map(Pedido::getCodigo).collect(Collectors.toList());

        assertThat(resto).contains("N1", "N2");
    }


    @Test
    void seleccionar_pedido_carga_detalle_cliente_y_actualiza_texto_detalle() {
        setAuth("cliente1");

        Pedido pListado = pedido(
                "P1",
                LocalDateTime.now().minusMinutes(1),
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION,
                TipoPedidoCliente.MESA,
                5
        );

        Pedido pDet = pedido(
                "P1",
                pListado.getFechaCreacion(),
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION,
                TipoPedidoCliente.MESA,
                5
        );

        Producto prod = new Producto();
        prod.setNombre("Hamburguesa");
        prod.setPrecio(new BigDecimal("6.50"));
        prod.setCodigo("PR-1");

        LineaPedido lp = new LineaPedido(pDet, prod, 2);
        pDet.getLineaPedidos().add(lp);

        when(pedidoService.listarPedidosCliente("cliente1")).thenReturn(List.of(pListado));
        when(pedidoService.cargarDetalleCliente("P1", "cliente1")).thenReturn(pDet);

        ConsultaPedidosView view =
                new ConsultaPedidosView(pedidoService, pedidoCalculoService, ticketService);

        @SuppressWarnings("unchecked")
        Grid<Pedido> gridPedidos = (Grid<Pedido>) findFirst(view, Grid.class).orElseThrow();

        // Dispara el selection listener
        gridPedidos.select(pListado);

        verify(pedidoService).cargarDetalleCliente("P1", "cliente1");

        Span infoSeleccion = findAll(view, Span.class).stream()
                .filter(s -> "Detalle del pedido P1".equals(s.getText()))
                .findFirst()
                .orElseThrow();

        assertThat(infoSeleccion.getText()).isEqualTo("Detalle del pedido P1");
    }



    private static void setAuth(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "x", List.of())
        );
    }

    private static Pedido pedido(String codigo,
                                 LocalDateTime fecha,
                                 EstadoPedido ep,
                                 EstadoCocina ec,
                                 TipoPedidoCliente tipo,
                                 Integer mesa) {
        Pedido p = new Pedido();
        p.setCodigo(codigo);
        p.setFechaCreacion(fecha);
        p.setEstado(ep);
        p.setEstadoCocina(ec);
        p.setTipoPedido(tipo);

        if (mesa != null) {
            ReservaMesa rm = new ReservaMesa(mesa);
            p.setReservaMesa(rm);
        }

        return p;
    }

    private static <T> List<T> fetchGridItems(Grid<T> grid) {
        return grid.getDataProvider().fetch(new Query<>()).toList();
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