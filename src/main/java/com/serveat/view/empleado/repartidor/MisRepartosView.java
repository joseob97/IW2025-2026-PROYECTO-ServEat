package com.serveat.view.empleado.repartidor;

import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.repartidor.RepartidorService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.List;

@PageTitle("Mis repartos | Repartidor")
@Route(value = "empleado/repartidor/mis-repartos", layout = MainLayout.class)
@Secured("ROLE_REPARTIDOR")
public class MisRepartosView extends VerticalLayout {

    private final transient RepartidorService repartidorService;

    private final Span info = new Span("Aquí verás los pedidos asignados a ti.");
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    private final Button refrescar = new Button("🔄 Refrescar");

    public MisRepartosView(RepartidorService repartidorService) {
        this.repartidorService = repartidorService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Mis repartos");
        titulo.getStyle().set("margin", "0");

        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        configurarGrid();

        grid.setWidthFull();
        grid.setHeight("520px");
        grid.getStyle().set("border-radius", "10px");
        grid.getStyle().set("overflow", "hidden");

        refrescar.addClickListener(e -> cargar());
        HorizontalLayout barra = new HorizontalLayout(refrescar);
        barra.setWidthFull();
        barra.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout card = crearCard();
        card.add(info, barra, grid);

        add(titulo, card);

        cargar();
    }

    private void configurarGrid() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        grid.addColumn(Pedido::getCodigo)
                .setHeader("Pedido")
                .setAutoWidth(true);

        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(fmt) : "-")
                .setHeader("Creado")
                .setAutoWidth(true);

        grid.addColumn(p -> p.getCliente() != null ? p.getCliente().getNombre() : "-")
                .setHeader("Cliente")
                .setAutoWidth(true);

        grid.addColumn(p -> p.getDireccionEntrega() != null ? p.getDireccionEntrega() : "-")
                .setHeader("Dirección")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(p -> p.getEstadoReparto() != null ? p.getEstadoReparto().name() : "-")
                .setHeader("Estado reparto")
                .setAutoWidth(true);

        grid.addComponentColumn(p -> {
            Button enReparto = new Button("🚚 En reparto");
            enReparto.setEnabled(p.getEstadoReparto() == EstadoReparto.ASIGNADO);

            enReparto.addClickListener(e -> {
                try {
                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    repartidorService.marcarEnReparto(p.getCodigo(), username);
                    Notification.show("Pedido marcado EN_REPARTO", 2500, Notification.Position.BOTTOM_START);
                    cargar();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });

            return enReparto;
        }).setHeader("Acción 1").setAutoWidth(true);

        grid.addComponentColumn(p -> {
            Button entregado = new Button("✅ Entregado");
            entregado.setEnabled(p.getEstadoReparto() == EstadoReparto.EN_REPARTO);

            entregado.addClickListener(e -> {
                try {
                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    repartidorService.marcarEntregado(p.getCodigo(), username);
                    Notification.show("Pedido marcado ENTREGADO", 2500, Notification.Position.BOTTOM_START);
                    cargar();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });

            return entregado;
        }).setHeader("Acción 2").setAutoWidth(true);

        grid.addComponentColumn(p -> {
            Button incidencia = new Button("⚠ Incidencia");
            incidencia.setEnabled(p.getEstadoReparto() == EstadoReparto.ASIGNADO || p.getEstadoReparto() == EstadoReparto.EN_REPARTO);

            incidencia.addClickListener(e -> {
                try {
                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    repartidorService.marcarIncidencia(p.getCodigo(), username, "Incidencia indicada por el repartidor");
                    Notification.show("Incidencia registrada", 3000, Notification.Position.MIDDLE);
                    cargar();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });

            return incidencia;
        }).setHeader("Incidencia").setAutoWidth(true);

        grid.addComponentColumn(pedido -> {
                    Button detalles = new Button("📄 Ver detalles");

                    detalles.addClickListener(e -> {
                        detalles.getUI().ifPresent(ui ->
                                ui.navigate("empleado/repartidor/detalle/" + pedido.getCodigo())
                        );
                    });

                    return detalles;
                })
                .setHeader("Detalles")
                .setAutoWidth(true);
    }

    private void cargar() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            List<Pedido> pedidos = repartidorService.listarMisPedidos(username);
            grid.setItems(pedidos);
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
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