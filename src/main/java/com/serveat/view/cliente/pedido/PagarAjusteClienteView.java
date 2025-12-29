package com.serveat.view.cliente.pedido;

import com.serveat.domain.pago.ajuste.EstadoAjustePago;
import com.serveat.domain.pago.ajuste.TipoAjustePago;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pago.AjustePagoService;
import com.serveat.service.pago.AjustePagoDTO;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

@PageTitle("Ajuste de pago | Cliente")
@Route(value = "cliente/ajustes", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class PagarAjusteClienteView extends VerticalLayout implements HasUrlParameter<String> {

    private static final String SESSION_KEY_PREFIX = "AJUSTE_BORRADOR_"; // + codigoAjuste

    private final transient AjustePagoService ajustePagoService;
    private final transient PedidoService pedidoService;

    private String codigoAjuste;

    private final Span info = new Span();
    private final Span estado = new Span();
    private final Span importe = new Span();

    private final TextField referencia = new TextField("Referencia (opcional)");
    private final Button btnCompletar = new Button();
    private final Button btnCancelar = new Button("Cancelar ajuste");
    private final Button volver = new Button("⬅ Volver");

    private AjustePagoDTO dto;

    public PagarAjusteClienteView(AjustePagoService ajustePagoService,
                                  PedidoService pedidoService) {
        this.ajustePagoService = ajustePagoService;
        this.pedidoService = pedidoService;

        setWidthFull();
        setPadding(true);
        getStyle().set("max-width", "900px");
        getStyle().set("margin", "0 auto");
        getStyle().set("gap", "12px");

        H3 t = new H3("Procesar ajuste");
        t.getStyle().set("margin", "0");

        info.getStyle().set("color", "var(--lumo-secondary-text-color)");
        estado.getStyle().set("font-weight", "700");
        importe.getStyle().set("font-weight", "700");

        referencia.setWidth("420px");

        btnCompletar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnCompletar.getStyle().set("font-weight", "700");
        btnCompletar.addClickListener(e -> completar());

        btnCancelar.addThemeVariants(ButtonVariant.LUMO_ERROR);
        btnCancelar.addClickListener(e -> cancelar());

        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("cliente/pedidos")));

        add(t, info, estado, importe, referencia,
                new HorizontalLayout(btnCompletar, btnCancelar),
                volver);
    }

    @Override
    public void setParameter(BeforeEvent event, String codigoAjuste) {
        this.codigoAjuste = codigoAjuste;

        if (codigoAjuste == null || codigoAjuste.isBlank()) {
            Notification.show("Código de ajuste inválido", 3500, Notification.Position.MIDDLE);
            event.forwardTo("cliente/pedidos");
            return;
        }

        cargar();
    }

    private void cargar() {
        try {
            dto = ajustePagoService.obtenerDetallePorCodigo(codigoAjuste);

            info.setText("Ajuste: " + safe(dto.getCodigoAjuste()) + " | Pedido: " + safe(dto.getCodigoPedido()));
            estado.setText("Estado: " + safe(dto.getEstadoAjuste()));
            importe.setText("Importe: " + safe(dto.getDiferenciaAbs()) + " €");

            boolean pendiente = dto.getEstadoAjuste() == null || dto.getEstadoAjuste() == EstadoAjustePago.PENDIENTE;
            btnCompletar.setEnabled(pendiente);
            btnCancelar.setEnabled(pendiente);

            if (dto.getTipoAjuste() == TipoAjustePago.COBRO) {
                btnCompletar.setText("Pagar ajuste");
            } else {
                btnCompletar.setText("Procesar devolución");
            }

            // Si no existe borrador en sesión, no puedes aplicar cambios.
            Pedido borrador = (Pedido) VaadinSession.getCurrent().getAttribute(SESSION_KEY_PREFIX + codigoAjuste);
            if (pendiente && borrador == null) {
                Notification.show("No se encontró el borrador de cambios para este ajuste. Vuelve a editar el pedido.",
                        5000, Notification.Position.MIDDLE);
                btnCompletar.setEnabled(false);
            }

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("cliente/pedidos"));
        }
    }

    /**
     * Aquí SÍ se persiste el pedido:
     * - aplica cambios (confirmarCambiosPedidoCliente)
     * - completa ajuste
     * - borra el borrador de sesión
     */
    private void completar() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            Pedido borrador = (Pedido) VaadinSession.getCurrent().getAttribute(SESSION_KEY_PREFIX + codigoAjuste);
            if (borrador == null) {
                Notification.show("No se encontró el borrador. Vuelve a editar el pedido.", 4500, Notification.Position.MIDDLE);
                return;
            }

            // 1) Persistir pedido AHORA
            pedidoService.confirmarCambiosPedidoCliente(borrador, username);

            // 2) Completar ajuste
            AjustePagoDTO res = ajustePagoService.completarAjuste(codigoAjuste, referencia.getValue());

            // 3) Limpiar borrador
            VaadinSession.getCurrent().setAttribute(SESSION_KEY_PREFIX + codigoAjuste, null);

            Notification.show(res != null ? safe(res.getMensaje()) : "Ajuste completado", 3500, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("cliente/pedidos"));

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
        }
    }

    /**
     *  Cancelar ajuste:
     * - cancela ajuste
     * - borra borrador de sesión
     * - NO se persiste pedido
     */
    private void cancelar() {
        try {
            AjustePagoDTO res = ajustePagoService.cancelarAjuste(codigoAjuste, "Cancelado por cliente");

            VaadinSession.getCurrent().setAttribute(SESSION_KEY_PREFIX + codigoAjuste, null);

            Notification.show(res != null ? safe(res.getMensaje()) : "Ajuste cancelado", 3500, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("cliente/pedidos"));

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
        }
    }

    private String safe(Object o) {
        return o == null ? "-" : o.toString();
    }
}