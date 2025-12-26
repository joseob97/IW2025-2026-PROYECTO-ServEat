package com.serveat.view.empleado.repartidor;

import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.repartidor.RepartidorService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
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

        // NUEVO: Columna Total
        grid.addColumn(p -> p.calcularPrecioTotal() + " €")
                .setHeader("Total")
                .setAutoWidth(true);

        // CORREGIDO: Columna Estado Pago basada en MetodoPago
        grid.addComponentColumn(p -> {
            Pago pago = p.getPago();
            boolean cobrar = pago != null && pago.getMetodo() == MetodoPago.EFECTIVO;
            
            Span badge = new Span(cobrar ? "COBRAR" : "PAGADO");
            badge.getElement().getThemeList().add("badge " + (cobrar ? "error" : "success"));
            return badge;
        }).setHeader("Pago").setAutoWidth(true);

        grid.addColumn(p -> p.getEstadoReparto() != null ? p.getEstadoReparto().name() : "-")
                .setHeader("Estado reparto")
                .setAutoWidth(true);

        // Columna de acciones unificada
        grid.addComponentColumn(p -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            if (p.getEstadoReparto() == EstadoReparto.ASIGNADO) {
                Button enReparto = new Button("🚚 Salir");
                enReparto.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                enReparto.addClickListener(e -> confirmarCambioEstado(p, "EN_REPARTO"));
                actions.add(enReparto);
            } else if (p.getEstadoReparto() == EstadoReparto.EN_REPARTO) {
                Button entregado = new Button("✅ Entregar");
                entregado.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                entregado.addClickListener(e -> confirmarCambioEstado(p, "ENTREGADO"));
                actions.add(entregado);
            }

            if (p.getEstadoReparto() == EstadoReparto.ASIGNADO || p.getEstadoReparto() == EstadoReparto.EN_REPARTO) {
                Button incidencia = new Button("⚠ Incidencia");
                incidencia.addThemeVariants(ButtonVariant.LUMO_ERROR);
                incidencia.addClickListener(e -> confirmarIncidencia(p));
                actions.add(incidencia);
            }

            return actions;
        }).setHeader("Acciones").setAutoWidth(true);
    }

    private void confirmarCambioEstado(Pedido p, String accion) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar acción");
        dialog.setText("¿Estás seguro de marcar el pedido como " + accion + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Confirmar");
        dialog.setConfirmButtonTheme("primary");

        dialog.addConfirmListener(event -> {
            try {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                if ("EN_REPARTO".equals(accion)) {
                    repartidorService.marcarEnReparto(p.getCodigo(), username);
                } else if ("ENTREGADO".equals(accion)) {
                    repartidorService.marcarEntregado(p.getCodigo(), username);
                }
                Notification.show("Estado actualizado correctamente", 2500, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                cargar();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private void confirmarIncidencia(Pedido p) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Reportar Incidencia");
        dialog.setText("¿Estás seguro de reportar una incidencia para este pedido?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Reportar");
        dialog.setConfirmButtonTheme("error");

        dialog.addConfirmListener(event -> {
            try {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                // Aquí podríamos abrir otro diálogo para pedir el motivo, pero por simplicidad usamos uno genérico
                repartidorService.marcarIncidencia(p.getCodigo(), username, "Incidencia reportada por repartidor");
                Notification.show("Incidencia registrada", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                cargar();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
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
