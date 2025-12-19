package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.util.List;

@PageTitle("Comandas Pendientes | Cocinero")
@Route(value = "empleado/cocinero/comandas", layout = MainLayout.class)
@Secured("ROLE_COCINERO")
public class ComandasView extends VerticalLayout {

    // SERVICIOS (transient para Sonar/Vaadin)
    private final transient PedidoService pedidoService;

    // COMPONENTES UI
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    public ComandasView(PedidoService pedidoService) {

        this.pedidoService = pedidoService;

        setSpacing(false);
        setPadding(true);
        setWidthFull();

        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Comandas Pendientes");
        titulo.getStyle().set("margin", "0");
        add(titulo);

        // CARD CON GRID

        VerticalLayout card = crearCard();
        card.getStyle().set("gap", "12px");

        configurarGrid();
        grid.setWidthFull();
        grid.setHeight("500px");

        card.add(grid);
        add(card);

        // BOTÓN REFRESCAR

        Button refrescar = new Button("🔄 Refrescar");
        refrescar.getStyle().set("font-weight", "600");
        refrescar.setWidth("260px");
        refrescar.addClickListener(e -> recargarPedidos());

        HorizontalLayout filaAcciones = new HorizontalLayout(refrescar);
        filaAcciones.setWidthFull();
        filaAcciones.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        add(filaAcciones);

        // Cargar datos iniciales
        recargarPedidos();
    }

    private void configurarGrid() {

        // Número de mesa
        grid.addColumn(p -> "Mesa " + (p.getReservaMesa() != null ? p.getReservaMesa().getNumeroMesa() : "N/A"))
                .setHeader("Mesa")
                .setAutoWidth(true);

        // Código pedido
        grid.addColumn(Pedido::getCodigo)
                .setHeader("Código")
                .setAutoWidth(true);

        // Estado
        grid.addColumn(Pedido::getEstadoCocina)
                .setHeader("Estado")
                .setAutoWidth(true);

        // Hora del pedido
        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().toString() : "N/A")
                .setHeader("Hora")
                .setAutoWidth(true);

        // Botón ver detalle
        grid.addComponentColumn(pedido -> {
            Button verDetalle = new Button("Ver Detalle");
            verDetalle.addClickListener(e ->
                UI.getCurrent().navigate(DetalleComandaView.class, pedido.getId().toString())
            );
            return verDetalle;
        }).setHeader("Acciones");
    }

    private void recargarPedidos() {
        try {
            // Obtener pedidos en estado PENDIENTE_ACEPTACION o EN_PREPARACION
            List<Pedido> pedidos = pedidoService.obtenerPedidosPorEstado(EstadoCocina.PENDIENTE_ACEPTACION);
            List<Pedido> enPreparacion = pedidoService.obtenerPedidosPorEstado(EstadoCocina.EN_PREPARACION);

            pedidos.addAll(enPreparacion);
            grid.setItems(pedidos);

            if (pedidos.isEmpty()) {
                Notification.show("ℹ️ No hay comandas pendientes", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            Notification.show("❌ Error al cargar comandas: " + e.getMessage(), 4000, Notification.Position.MIDDLE);
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

        return card;
    }
}

