package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@PageTitle("Seguimiento | Cliente")
@Route(value = "cliente/pedidos/seguimiento", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class SeguimientoPedidoView extends VerticalLayout {

    private final transient PedidoService pedidoService;

    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    private final Span info = new Span("Selecciona un pedido para ver su estado.");
    private final Span chipTipo = chip("-");
    private final Span chipPedido = chip("-");
    private final Span chipCocina = chip("-");
    private final Span chipReparto = chip("-");
    private final Span direccion = new Span("Dirección: -");

    private final Button refrescar = new Button("🔄 Refrescar");
    private final Button volver = new Button("⬅ Volver");

    private transient Pedido seleccionado;

    private transient ScheduledExecutorService scheduler;

    public SeguimientoPedidoView(PedidoService pedidoService) {
        this.pedidoService = pedidoService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Seguimiento de pedidos");
        titulo.getStyle().set("margin", "0");

        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(PanelPedidoClienteView.class)));

        refrescar.addClickListener(e -> {
            cargarPedidos();
            refrescarSiHaySeleccion();
        });

        HorizontalLayout top = new HorizontalLayout(titulo, new HorizontalLayout(refrescar, volver));
        top.setWidthFull();
        top.setAlignItems(FlexComponent.Alignment.CENTER);
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        configurarGrid();

        VerticalLayout cardListado = crearCard();
        cardListado.add(info, grid);

        VerticalLayout cardEstado = crearCard();
        H3 hEstado = new H3("Estado actual");
        hEstado.getStyle().set("margin", "0");

        HorizontalLayout filaChips = new HorizontalLayout(
                bloqueEstado("Tipo", chipTipo),
                bloqueEstado("Pedido", chipPedido),
                bloqueEstado("Cocina", chipCocina),
                bloqueEstado("Reparto", chipReparto)
        );
        filaChips.setWidthFull();
        filaChips.setSpacing(false);
        filaChips.getStyle().set("gap", "10px");

        direccion.getStyle().set("color", "var(--lumo-secondary-text-color)");

        cardEstado.add(hEstado, filaChips, direccion);

        add(top, cardListado, cardEstado);

        cargarPedidos();
        refrescarDetalle();
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        UI ui = attachEvent.getUI();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (seleccionado == null) return;

                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                Pedido actualizado = pedidoService.cargarDetalleCliente(seleccionado.getCodigo(), username);

                ui.access(() -> {
                    seleccionado = actualizado;
                    refrescarDetalle();
                });

            } catch (Exception ignored) {
                ui.access(() -> { /* silencioso para no spamear notificaciones */ });
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        super.onDetach(detachEvent);
    }

    private void configurarGrid() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        grid.addColumn(Pedido::getCodigo)
                .setHeader("Pedido")
                .setAutoWidth(true);

        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(fmt) : "-")
                .setHeader("Fecha")
                .setAutoWidth(true);

        grid.addColumn(p -> safe(p.getTipoPedido()))
                .setHeader("Tipo")
                .setAutoWidth(true);

        grid.addColumn(p -> safe(p.getEstado()))
                .setHeader("Estado pedido")
                .setAutoWidth(true);

        grid.addColumn(p -> safe(p.getEstadoCocina()))
                .setHeader("Cocina")
                .setAutoWidth(true);

        grid.addColumn(p -> safe(p.getEstadoReparto()))
                .setHeader("Reparto")
                .setAutoWidth(true);

        grid.setWidthFull();
        grid.setHeight("420px");
        grid.getStyle().set("border-radius", "10px");
        grid.getStyle().set("overflow", "hidden");

        grid.addSelectionListener(e -> {
            seleccionado = e.getFirstSelectedItem().orElse(null);
            refrescarDetalle();
            refrescarSiHaySeleccion();
        });
    }

    private void cargarPedidos() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            List<Pedido> pedidos = pedidoService.listarPedidosCliente(username);

            grid.setItems(pedidos);

            info.setText(pedidos.isEmpty()
                    ? "No tienes pedidos todavía."
                    : "Tus pedidos: " + pedidos.size() + " (se actualiza automáticamente)");

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
        }
    }

    private void refrescarSiHaySeleccion() {
        if (seleccionado == null) return;
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            seleccionado = pedidoService.cargarDetalleCliente(seleccionado.getCodigo(), username);
            refrescarDetalle();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE);
        }
    }

    private void refrescarDetalle() {
        if (seleccionado == null) {
            setChip(chipTipo, "-");
            setChip(chipPedido, "-");
            setChip(chipCocina, "-");
            setChip(chipReparto, "-");
            direccion.setText("Dirección: -");
            return;
        }

        setChip(chipTipo, safe(seleccionado.getTipoPedido()));
        setChip(chipPedido, safe(seleccionado.getEstado()));
        setChip(chipCocina, safe(seleccionado.getEstadoCocina()));
        setChip(chipReparto, safe(seleccionado.getEstadoReparto()));

        String dir = seleccionado.getDireccionEntrega();
        direccion.setText("Dirección: " + (dir != null && !dir.isBlank() ? dir : "-"));

        colorearChips();
    }

    private void colorearChips() {
        pintar(chipPedido, safe(seleccionado.getEstado()));
        pintar(chipCocina, safe(seleccionado.getEstadoCocina()));
        pintar(chipReparto, safe(seleccionado.getEstadoReparto()));
    }

    private void pintar(Span chip, String estado) {
        chip.getStyle().remove("background");
        chip.getStyle().remove("color");
        chip.getStyle().remove("border");

        String e = estado != null ? estado.toUpperCase() : "-";

        String bg;
        String fg = "var(--lumo-base-color)";
        String border = "transparent";

        if (e.contains("ANUL") || e.contains("CANCEL") || e.contains("INCID")) {
            bg = "var(--lumo-error-color)";
        } else if (e.contains("ENTREG") || e.contains("COMPLET") || e.contains("CONFIRM")) {
            bg = "var(--lumo-success-color)";
        } else if (e.contains("REPARTO") || e.contains("COCINA") || e.contains("CURSO") || e.contains("ASIGN")) {
            bg = "var(--lumo-primary-color)";
        } else {
            bg = "var(--lumo-contrast-20pct)";
            fg = "var(--lumo-body-text-color)";
            border = "1px solid var(--lumo-contrast-20pct)";
        }

        chip.getStyle().set("background", bg);
        chip.getStyle().set("color", fg);
        chip.getStyle().set("border", border);
    }

    private VerticalLayout bloqueEstado(String titulo, Span chip) {
        Span t = new Span(titulo);
        t.getStyle().set("color", "var(--lumo-secondary-text-color)");
        t.getStyle().set("font-size", "0.85rem");

        VerticalLayout box = new VerticalLayout(t, chip);
        box.setPadding(true);
        box.setSpacing(false);
        box.getStyle().set("gap", "8px");
        box.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        box.getStyle().set("border-radius", "12px");
        box.setWidthFull();
        return box;
    }

    private Span chip(String text) {
        Span s = new Span(text);
        s.getStyle().set("display", "inline-block");
        s.getStyle().set("padding", "6px 10px");
        s.getStyle().set("border-radius", "999px");
        s.getStyle().set("font-weight", "600");
        s.getStyle().set("background", "var(--lumo-contrast-20pct)");
        return s;
    }

    private void setChip(Span chip, String text) {
        chip.setText(text != null ? text : "-");
    }

    private String safe(Object o) {
        return o != null ? o.toString() : "-";
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