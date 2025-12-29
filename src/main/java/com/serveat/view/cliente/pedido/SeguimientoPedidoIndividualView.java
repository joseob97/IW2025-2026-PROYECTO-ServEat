package com.serveat.view.cliente.pedido;

import com.serveat.service.pedido.seguimiento.PedidoSeguimientoDTO;
import com.serveat.service.pedido.seguimiento.PedidoSeguimientoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.*;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@PageTitle("Seguimiento pedido | Cliente")
@Route(value = "cliente/pedidos/seguimiento", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class SeguimientoPedidoIndividualView extends VerticalLayout implements HasUrlParameter<String> {

    private final transient PedidoSeguimientoService seguimientoService;

    private String codigoPedido;

    private final Span info = new Span("Cargando seguimiento...");
    private final Span chipPedido = chip("-");
    private final Span chipCocina = chip("-");
    private final Span chipReparto = chip("-");
    private final Span eta = new Span("Tiempo estimado restante: -");
    private final Span mensaje = new Span("");

    private final ProgressBar barra = new ProgressBar();

    private final Button refrescar = new Button("Refrescar");
    private final Button volver = new Button("Volver a activos");

    private transient ScheduledExecutorService scheduler;

    public SeguimientoPedidoIndividualView(PedidoSeguimientoService seguimientoService) {
        this.seguimientoService = seguimientoService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "18px");
        getStyle().set("max-width", "980px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Seguimiento del pedido");
        titulo.getStyle().set("margin", "0");

        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        refrescar.addClickListener(e -> cargar(false));
        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SeguimientoPedidosActivosView.class)));

        HorizontalLayout top = new HorizontalLayout(titulo, new HorizontalLayout(refrescar, volver));
        top.setWidthFull();
        top.setAlignItems(FlexComponent.Alignment.CENTER);
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout card = crearCard();

        H3 estadoTitle = new H3("Estado actual");
        estadoTitle.getStyle().set("margin", "0");

        HorizontalLayout chips = new HorizontalLayout(
                bloqueEstado("Pedido", chipPedido),
                bloqueEstado("Cocina", chipCocina),
                bloqueEstado("Reparto", chipReparto)
        );
        chips.setWidthFull();
        chips.setSpacing(false);
        chips.getStyle().set("gap", "10px");

        eta.getStyle().set("font-weight", "600");
        mensaje.getStyle().set("color", "var(--lumo-secondary-text-color)");

        barra.setWidthFull();
        barra.setIndeterminate(true);

        card.add(info, estadoTitle, chips, barra, eta, mensaje);
        add(top, card);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.codigoPedido = parameter;

        if (codigoPedido == null || codigoPedido.isBlank()) {
            Notification.show("Falta el código del pedido", 3500, Notification.Position.MIDDLE);
            event.forwardTo(SeguimientoPedidosActivosView.class);
            return;
        }

        info.setText("Pedido: " + codigoPedido);
        cargar(false);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        UI ui = attachEvent.getUI();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (codigoPedido == null || codigoPedido.isBlank()) return;
            ui.access(() -> cargar(true));
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        super.onDetach(detachEvent);
    }

    private void cargar(boolean silencioso) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            PedidoSeguimientoDTO dto = seguimientoService.obtenerSeguimientoPedidoCliente(codigoPedido, username);
            pintar(dto);
        } catch (Exception ex) {
            if (!silencioso) {
                Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
            }
        }
    }

    private void pintar(PedidoSeguimientoDTO dto) {
        if (dto == null) return;

        chipPedido.setText(dto.getEstadoPedido());
        chipCocina.setText(dto.getEstadoCocina());
        chipReparto.setText(dto.getEstadoReparto());

        aplicarEstiloChip(chipPedido, dto.getEstiloPedido());
        aplicarEstiloChip(chipCocina, dto.getEstiloCocina());
        aplicarEstiloChip(chipReparto, dto.getEstiloReparto());

        eta.setText("Tiempo estimado restante: " + (dto.getEtiquetaTiempoRestante() != null ? dto.getEtiquetaTiempoRestante() : "-"));

        Double prog = dto.getProgreso();
        if (prog == null) {
            barra.setIndeterminate(true);
        } else {
            barra.setIndeterminate(false);
            barra.setValue(Math.min(1.0, Math.max(0.0, prog)));
        }

        String msg = dto.getMensaje();
        if (msg != null && !msg.isBlank()) {
            mensaje.setText("Info: " + msg);
            mensaje.setVisible(true);
        } else {
            mensaje.setText("");
            mensaje.setVisible(false);
        }
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
        s.getStyle().set("font-weight", "700");
        s.getStyle().set("background", "var(--lumo-contrast-20pct)");
        s.getStyle().set("color", "var(--lumo-body-text-color)");
        return s;
    }

    private void aplicarEstiloChip(Span chip, String estilo) {
        chip.getStyle().set("border", "transparent");
        chip.getStyle().set("color", "var(--lumo-base-color)");

        String st = (estilo == null) ? "NEUTRO" : estilo.toUpperCase();

        switch (st) {
            case "ERROR" -> chip.getStyle().set("background", "var(--lumo-error-color)");
            case "OK" -> chip.getStyle().set("background", "var(--lumo-success-color)");
            case "INFO" -> chip.getStyle().set("background", "var(--lumo-primary-color)");
            default -> {
                chip.getStyle().set("background", "var(--lumo-contrast-20pct)");
                chip.getStyle().set("color", "var(--lumo-body-text-color)");
                chip.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            }
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