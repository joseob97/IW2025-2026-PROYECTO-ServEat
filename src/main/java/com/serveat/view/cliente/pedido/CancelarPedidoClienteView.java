package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.List;

@PageTitle("Cancelar pedido | Cliente")
@Route(value = "cliente/pedidos/cancelar", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class CancelarPedidoClienteView extends VerticalLayout {

    private final transient PedidoService pedidoService;
    private final transient PedidoCalculoService pedidoCalculoService;

    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);
    private final TextArea motivo = new TextArea("Motivo (opcional)");
    private final Button cancelar = new Button("❌ Cancelar pedido seleccionado");

    private transient Pedido seleccionado;

    public CancelarPedidoClienteView(PedidoService pedidoService,
                                     PedidoCalculoService pedidoCalculoService) {
        this.pedidoService = pedidoService;
        this.pedidoCalculoService = pedidoCalculoService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Cancelar pedido");
        titulo.getStyle().set("margin", "0");
        add(titulo, new Span("Solo puedes cancelar si cocina aún no lo ha aceptado (PENDIENTE_ACEPTACION)."));

        configurarGrid();

        VerticalLayout card = crearCard();
        card.add(grid);
        add(card);

        motivo.setWidthFull();
        motivo.setMaxLength(200);
        motivo.setPlaceholder("Ej: Me he equivocado con el pedido...");
        motivo.setClearButtonVisible(true);

        cancelar.setEnabled(false);
        cancelar.getStyle().set("font-weight", "600");
        cancelar.addClickListener(e -> confirmarCancelacion());

        HorizontalLayout acciones = new HorizontalLayout(motivo, cancelar);
        acciones.setWidthFull();
        acciones.setAlignItems(FlexComponent.Alignment.END);
        acciones.setFlexGrow(1, motivo);

        VerticalLayout cardAcciones = crearCard();
        cardAcciones.add(acciones);
        add(cardAcciones);

        cargarCancelables();
    }

    private void configurarGrid() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        grid.addColumn(Pedido::getCodigo).setHeader("Nº Pedido").setAutoWidth(true);

        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(fmt) : "-")
                .setHeader("Fecha").setAutoWidth(true);

        grid.addColumn(p -> p.getEstado() != null ? p.getEstado().name() : "-")
                .setHeader("Estado pedido").setAutoWidth(true);

        grid.addColumn(p -> p.getEstadoCocina() != null ? p.getEstadoCocina().name() : "-")
                .setHeader("Estado cocina").setAutoWidth(true);

        grid.addColumn(p -> {
                    try {
                        return pedidoCalculoService.calcularTotalPedido(p) + " €";
                    } catch (Exception ex) {
                        return "-";
                    }
                })
                .setHeader("Total").setAutoWidth(true);

        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setHeight("420px");
        grid.setWidthFull();

        grid.addSelectionListener(e -> {
            seleccionado = e.getFirstSelectedItem().orElse(null);
            cancelar.setEnabled(seleccionado != null);
        });
    }

    private void cargarCancelables() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            List<Pedido> pedidos = pedidoService.listarPedidosCliente(username).stream()
                    .filter(pedidoService::puedeModificarCliente)
                    .toList();

            grid.setItems(pedidos);
            seleccionado = null;
            cancelar.setEnabled(false);

            if (pedidos.isEmpty()) {
                Notification.show("No tienes pedidos cancelables ahora mismo.", 3000, Notification.Position.BOTTOM_START);
            }
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void confirmarCancelacion() {
        if (seleccionado == null) return;

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar cancelación");
        dialog.setText("¿Seguro que quieres cancelar el pedido " + seleccionado.getCodigo() + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Sí, cancelar");
        dialog.addConfirmListener(e -> ejecutarCancelacion());
        dialog.open();
    }

    private void ejecutarCancelacion() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            pedidoService.cancelarPedido(
                    seleccionado.getCodigo(),
                    motivo.getValue() != null ? motivo.getValue().trim() : null,
                    username
            );

            Notification.show("Pedido cancelado ✅", 3000, Notification.Position.MIDDLE);
            motivo.clear();
            cargarCancelables();

        } catch (Exception ex) {
            Notification.show("No se pudo cancelar: " + ex.getMessage(), 4500, Notification.Position.MIDDLE);
        }
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