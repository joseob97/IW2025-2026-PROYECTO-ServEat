package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;

@PageTitle("Mis pedidos | Cliente")
@Route(value = "cliente/pedidos", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class ConsultaPedidosView extends VerticalLayout {

    private final transient PedidoService pedidoService;
    private final transient PedidoCalculoService pedidoCalculoService;

    private final Grid<Pedido> gridPedidos = new Grid<>(Pedido.class, false);
    private final Grid<LineaPedido> gridLineas = new Grid<>(LineaPedido.class, false);

    private final Span infoSeleccion = new Span("Selecciona un pedido para ver el detalle.");

    private transient Pedido pedidoSeleccionado;

    public ConsultaPedidosView(PedidoService pedidoService,
                               PedidoCalculoService pedidoCalculoService) {
        this.pedidoService = pedidoService;
        this.pedidoCalculoService = pedidoCalculoService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();

        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Mis pedidos");
        titulo.getStyle().set("margin", "0");
        add(titulo);

        VerticalLayout cardListado = crearCard();
        configurarGridPedidos();
        gridPedidos.setWidthFull();
        gridPedidos.setHeight("360px");
        gridPedidos.getStyle().set("border-radius", "10px");
        gridPedidos.getStyle().set("overflow", "hidden");
        cardListado.add(gridPedidos);
        add(cardListado);

        VerticalLayout cardDetalle = crearCard();
        infoSeleccion.getStyle().set("color", "var(--lumo-secondary-text-color)");

        configurarGridLineas();
        gridLineas.setWidthFull();
        gridLineas.setHeight("260px");
        gridLineas.getStyle().set("border-radius", "10px");
        gridLineas.getStyle().set("overflow", "hidden");

        cardDetalle.add(infoSeleccion, gridLineas);
        add(cardDetalle);

        cargarPedidos();
        refrescarLineas();
    }

    private void configurarGridPedidos() {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        gridPedidos.addColumn(Pedido::getCodigo)
                .setHeader("Nº Pedido")
                .setAutoWidth(true);

        gridPedidos.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(fmt) : "-")
                .setHeader("Fecha")
                .setAutoWidth(true);

        gridPedidos.addColumn(p -> p.getEstado() != null ? p.getEstado().name() : "-")
                .setHeader("Estado pedido")
                .setAutoWidth(true);

        gridPedidos.addColumn(p -> p.getEstadoCocina() != null ? p.getEstadoCocina().name() : "-")
                .setHeader("Estado cocina")
                .setAutoWidth(true);

        gridPedidos.addColumn(p -> {
                    try {
                        return pedidoCalculoService.calcularTotalPedido(p) + " €";
                    } catch (Exception e) {
                        return "-";
                    }
                })
                .setHeader("Total")
                .setAutoWidth(true);

        gridPedidos.addComponentColumn(p -> {
            if (!pedidoService.puedeModificarCliente(p)) return new Span("");
            Button editar = new Button("✏️ Editar");
            editar.addClickListener(e ->
                    getUI().ifPresent(ui -> ui.navigate(ModificarPedidoClienteView.class, p.getCodigo()))
            );
            return editar;
        }).setHeader("Modificar").setAutoWidth(true);

        gridPedidos.addComponentColumn(p -> {
            if (!pedidoService.puedeModificarCliente(p)) return new Span("");
            Button cancelar = new Button("❌ Cancelar");
            cancelar.addClickListener(e ->
                    getUI().ifPresent(ui -> ui.navigate(CancelarPedidoClienteView.class))
            );
            return cancelar;
        }).setHeader("Cancelar").setAutoWidth(true);

        gridPedidos.addSelectionListener(e -> {
            pedidoSeleccionado = e.getFirstSelectedItem().orElse(null);

            if (pedidoSeleccionado == null) {
                infoSeleccion.setText("Selecciona un pedido para ver el detalle.");
                refrescarLineas();
                return;
            }

            try {
                pedidoSeleccionado = pedidoService.obtenerPorCodigo(pedidoSeleccionado.getCodigo());
                infoSeleccion.setText("Detalle del pedido " + pedidoSeleccionado.getCodigo());
                refrescarLineas();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });
    }

    private void cargarPedidos() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            gridPedidos.setItems(pedidoService.listarPedidosCliente(username));
            pedidoSeleccionado = null;
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void configurarGridLineas() {

        gridLineas.addColumn(lp -> lp.getProducto() != null ? lp.getProducto().getNombre() : "-")
                .setHeader("Producto")
                .setAutoWidth(true)
                .setFlexGrow(1);

        gridLineas.addColumn(LineaPedido::getCantidad)
                .setHeader("Cantidad")
                .setAutoWidth(true);

        gridLineas.addColumn(lp -> lp.getPrecioUnitario() != null ? lp.getPrecioUnitario() + " €" : "-")
                .setHeader("Precio ud.")
                .setAutoWidth(true);

        gridLineas.addColumn(lp -> pedidoCalculoService.calcularPrecioLinea(lp) + " €")
                .setHeader("Subtotal")
                .setAutoWidth(true);
    }

    private void refrescarLineas() {
        if (pedidoSeleccionado == null) {
            gridLineas.setItems();
            return;
        }
        gridLineas.setItems(pedidoSeleccionado.getLineaPedidos());
    }

    private VerticalLayout crearCard() {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();

        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");
        card.getStyle().set("gap", "12px");

        return card;
    }
}