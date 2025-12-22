package com.serveat.view.cliente.pedido;

import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pago.PagoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.math.BigDecimal;

@PageTitle("Pasarela de pago | Cliente")
@Route(value = "cliente/pedido/online/pasarela", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class PasarelaPagoSimuladaView extends VerticalLayout implements BeforeEnterObserver {

    private final transient PedidoService pedidoService;
    private final transient PagoService pagoService;

    private transient Pedido carrito;
    private transient MetodoPago metodo;
    private transient String username;

    private final Span info = new Span("Cargando...");
    private final Span total = new Span("Total: -");

    private final ComboBox<MetodoPago> metodoPago = new ComboBox<>("Método de pago");

    private final TextField cardNumber = new TextField("Número de tarjeta");
    private final TextField cardHolder = new TextField("Titular");
    private final TextField cardExpiry = new TextField("Caducidad (MM/YY)");
    private final PasswordField cardCvv = new PasswordField("CVV");

    private final EmailField paypalEmail = new EmailField("Email PayPal");
    private final PasswordField paypalPassword = new PasswordField("Contraseña PayPal");

    private final IntegerField efectivoPagaCon = new IntegerField("Paga con (opcional)");

    private final Button confirmar = new Button("✅ Confirmar pago");
    private final Button volver = new Button("⬅ Volver al carrito");

    private final VerticalLayout bloqueTarjeta = new VerticalLayout();
    private final VerticalLayout bloquePaypal = new VerticalLayout();
    private final VerticalLayout bloqueEfectivo = new VerticalLayout();

    public PasarelaPagoSimuladaView(PedidoService pedidoService, PagoService pagoService) {
        this.pedidoService = pedidoService;
        this.pagoService = pagoService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "18px");
        getStyle().set("max-width", "900px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Pasarela de pago (simulada)");
        titulo.getStyle().set("margin", "0");

        info.getStyle().set("color", "var(--lumo-secondary-text-color)");
        total.getStyle().set("font-weight", "600");

        metodoPago.setItems(MetodoPago.values());
        metodoPago.setWidth("360px");
        metodoPago.setPlaceholder("Selecciona método");
        metodoPago.addValueChangeListener(e -> {
            metodo = e.getValue();
            actualizarBloques();
        });

        prepararBloques();

        confirmar.setWidth("260px");
        confirmar.getStyle().set("font-weight", "600");
        confirmar.addClickListener(e -> confirmarPago());

        volver.setWidth("260px");
        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(CrearPedidoDomicilioView.class)));

        HorizontalLayout acciones = new HorizontalLayout(confirmar, volver);
        acciones.setWidthFull();
        acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        VerticalLayout card = crearCard();
        card.add(info, total, metodoPago, bloqueTarjeta, bloquePaypal, bloqueEfectivo, acciones);

        add(titulo, card);

        setAccionesEnabled(false);
        actualizarBloques();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        carrito = (Pedido) event.getUI().getSession().getAttribute("pedidoOnlineCarrito");
        metodo = (MetodoPago) event.getUI().getSession().getAttribute("pedidoOnlineMetodoPago");
        username = (String) event.getUI().getSession().getAttribute("pedidoOnlineUsername");

        if (carrito == null || carrito.getLineaPedidos() == null || carrito.getLineaPedidos().isEmpty()
                || username == null || username.isBlank()) {
            Notification.show("No hay carrito para pagar. Vuelve a crear el pedido.", 3500, Notification.Position.MIDDLE);
            event.forwardTo(CrearPedidoDomicilioView.class);
            return;
        }

        metodoPago.setValue(metodo);

        info.setText("Usuario: " + username);
        total.setText("Total: " + carrito.calcularPrecioTotal() + " €");

        setAccionesEnabled(true);
        actualizarBloques();
    }

    private void confirmarPago() {
        try {
            setAccionesEnabled(false);

            Pedido pedidoCreado = pedidoService.crearPedidoDesdeCliente(carrito, username);

            BigDecimal pagaCon = efectivoPagaCon.getValue() != null
                    ? BigDecimal.valueOf(efectivoPagaCon.getValue())
                    : null;

            Pago pago = pagoService.procesarPagoOnline(
                    pedidoCreado,
                    metodoPago.getValue(),
                    cardNumber.getValue(),
                    cardHolder.getValue(),
                    cardExpiry.getValue(),
                    cardCvv.getValue(),
                    paypalEmail.getValue(),
                    paypalPassword.getValue(),
                    pagaCon
            );

            limpiarSesion();

            Notification.show("Pago confirmado ✅ (" + pago.getMetodo() + ")", 4500, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate(ConsultaPedidosView.class));

        } catch (Exception ex) {
            setAccionesEnabled(true);
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
        }
    }

    private void limpiarSesion() {
        getUI().ifPresent(ui -> {
            ui.getSession().setAttribute("pedidoOnlineCarrito", null);
            ui.getSession().setAttribute("pedidoOnlineMetodoPago", null);
            ui.getSession().setAttribute("pedidoOnlineUsername", null);
        });
    }

    private void prepararBloques() {
        cardNumber.setWidth("360px");
        cardNumber.setPlaceholder("4111 1111 1111 1111");
        cardHolder.setWidth("360px");
        cardHolder.setPlaceholder("NOMBRE APELLIDOS");
        cardExpiry.setWidth("200px");
        cardExpiry.setPlaceholder("MM/YY");
        cardCvv.setWidth("160px");
        cardCvv.setPlaceholder("123");

        HorizontalLayout fila3 = new HorizontalLayout(cardExpiry, cardCvv);
        fila3.setAlignItems(FlexComponent.Alignment.END);

        bloqueTarjeta.setPadding(false);
        bloqueTarjeta.setSpacing(false);
        bloqueTarjeta.getStyle().set("gap", "10px");
        bloqueTarjeta.add(new H3("Tarjeta"), cardNumber, cardHolder, fila3);

        paypalEmail.setWidth("360px");
        paypalEmail.setPlaceholder("email@paypal.com");
        paypalPassword.setWidth("360px");
        paypalPassword.setPlaceholder("••••••••");

        bloquePaypal.setPadding(false);
        bloquePaypal.setSpacing(false);
        bloquePaypal.getStyle().set("gap", "10px");
        bloquePaypal.add(new H3("PayPal"), paypalEmail, paypalPassword);

        efectivoPagaCon.setWidth("240px");
        efectivoPagaCon.setMin(0);
        efectivoPagaCon.setStepButtonsVisible(true);
        efectivoPagaCon.setHelperText("Opcional: para preparar el cambio.");

        bloqueEfectivo.setPadding(false);
        bloqueEfectivo.setSpacing(false);
        bloqueEfectivo.getStyle().set("gap", "10px");
        bloqueEfectivo.add(new H3("Efectivo"), efectivoPagaCon);
    }

    private void actualizarBloques() {
        MetodoPago m = metodoPago.getValue();
        bloqueTarjeta.setVisible(m == MetodoPago.TARJETA);
        bloquePaypal.setVisible(m == MetodoPago.PAYPAL);
        bloqueEfectivo.setVisible(m == MetodoPago.EFECTIVO);
    }

    private void setAccionesEnabled(boolean enabled) {
        confirmar.setEnabled(enabled);
        volver.setEnabled(enabled);
        metodoPago.setEnabled(enabled);
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