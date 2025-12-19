package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.util.Arrays;
import java.util.List;

@PageTitle("Gestión de Preparación | Cocinero")
@Route(value = "empleado/cocinero/gestion", layout = MainLayout.class)
@Secured("ROLE_COCINERO")
public class GestionPedidoCocineroView extends VerticalLayout {

    // SERVICIOS (transient para Sonar/Vaadin)
    private final transient PedidoService pedidoService;

    // COMPONENTES UI
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);
    private final ComboBox<EstadoCocina> filtroEstado = new ComboBox<>("Filtrar por estado");
    private final IntegerField filtroMesa = new IntegerField("Filtrar por mesa");

    public GestionPedidoCocineroView(PedidoService pedidoService) {

        this.pedidoService = pedidoService;

        setSpacing(false);
        setPadding(true);
        setWidthFull();

        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Gestión de Preparación");
        titulo.getStyle().set("margin", "0");
        add(titulo);

        // FILTROS

        H3 tituloFiltros = new H3("Filtros");
        tituloFiltros.getStyle().set("margin", "6px 0 0 0");
        add(tituloFiltros);

        VerticalLayout cardFiltros = crearCard();
        cardFiltros.getStyle().set("gap", "12px");

        configurarFiltros();

        HorizontalLayout filaFiltros = new HorizontalLayout(filtroEstado, filtroMesa);
        filaFiltros.setWidthFull();
        filaFiltros.getStyle().set("gap", "16px");
        filaFiltros.setAlignItems(FlexComponent.Alignment.END);

        Button limpiarFiltros = new Button("🔄 Limpiar");
        limpiarFiltros.addClickListener(e -> limpiarFiltrosAction());

        HorizontalLayout filaFiltrosConBotones = new HorizontalLayout(filaFiltros, limpiarFiltros);
        filaFiltrosConBotones.setWidthFull();
        filaFiltrosConBotones.getStyle().set("gap", "12px");
        filaFiltrosConBotones.setAlignItems(FlexComponent.Alignment.END);

        cardFiltros.add(filaFiltrosConBotones);
        add(cardFiltros);

        // GRID

        H3 tituloGrid = new H3("Comandas");
        tituloGrid.getStyle().set("margin", "6px 0 0 0");
        add(tituloGrid);

        VerticalLayout cardGrid = crearCard();
        cardGrid.getStyle().set("gap", "12px");

        configurarGrid();
        grid.setWidthFull();
        grid.setHeight("500px");

        cardGrid.add(grid);
        add(cardGrid);

        // CARGAR DATOS

        recargarPedidos();
    }

    private void configurarFiltros() {
        filtroEstado.setItems(EstadoCocina.values());
        filtroEstado.setPlaceholder("Todos los estados");
        filtroEstado.setClearButtonVisible(true);
        filtroEstado.addValueChangeListener(e -> recargarPedidos());

        filtroMesa.setMin(1);
        filtroMesa.setPlaceholder("Todas las mesas");
        filtroMesa.setValueChangeMode(ValueChangeMode.LAZY);
        filtroMesa.addValueChangeListener(e -> recargarPedidos());
    }

    private void configurarGrid() {
        grid.addColumn(p -> "Mesa " + (p.getReservaMesa() != null ? p.getReservaMesa().getNumeroMesa() : "N/A"))
                .setHeader("Mesa")
                .setAutoWidth(true);

        grid.addColumn(Pedido::getCodigo)
                .setHeader("Código")
                .setAutoWidth(true);

        grid.addColumn(Pedido::getEstadoCocina)
                .setHeader("Estado")
                .setAutoWidth(true);

        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().toString() : "N/A")
                .setHeader("Hora")
                .setAutoWidth(true);

        grid.addColumn(p -> p.calcularPrecioTotal() + " €")
                .setHeader("Total")
                .setAutoWidth(true);

        grid.addComponentColumn(pedido -> {
            Button verDetalle = new Button("✏️ Ver Detalle");
            verDetalle.addClickListener(e -> {
                UI.getCurrent().navigate(DetalleComandaView.class, pedido.getId().toString());
            });
            return verDetalle;
        }).setHeader("Acciones");
    }

    private void recargarPedidos() {
        try {
            List<Pedido> pedidos;

            EstadoCocina estado = filtroEstado.getValue();
            Integer mesa = filtroMesa.getValue();

            if (estado != null) {
                pedidos = pedidoService.obtenerPedidosPorEstado(estado);
            } else {
                // Obtener todos menos LISTO y CANCELADO
                pedidos = pedidoService.obtenerPedidosPorEstado(EstadoCocina.PENDIENTE_ACEPTACION);
                pedidos.addAll(pedidoService.obtenerPedidosPorEstado(EstadoCocina.EN_PREPARACION));
            }

            if (mesa != null && mesa > 0) {
                pedidos = pedidos.stream()
                        .filter(p -> p.getReservaMesa() != null && p.getReservaMesa().getNumeroMesa() == mesa)
                        .toList();
            }

            grid.setItems(pedidos);

            if (pedidos.isEmpty()) {
                Notification.show("ℹ️ No hay comandas con los filtros seleccionados", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            Notification.show("❌ Error: " + e.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void limpiarFiltrosAction() {
        filtroEstado.clear();
        filtroMesa.clear();
        recargarPedidos();
        Notification.show("✅ Filtros limpios", 2000, Notification.Position.MIDDLE);
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
