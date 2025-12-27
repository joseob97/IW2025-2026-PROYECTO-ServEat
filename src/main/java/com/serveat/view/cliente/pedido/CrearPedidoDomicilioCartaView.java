package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@PageTitle("Pedido a domicilio | Cliente")
@Route(value = "cliente/pedido/domicilio", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class CrearPedidoDomicilioCartaView extends CrearPedidoCartaBaseView {

    private final TextField direccion = new TextField("Dirección de entrega");

    public CrearPedidoDomicilioCartaView(PedidoService pedidoService,
                                         PedidoCarritoService pedidoCarritoService,
                                         PedidoCalculoService pedidoCalculoService,
                                         ProductoService productoService,
                                         CategoriaService categoriaService) {
        super(pedidoService, pedidoCarritoService, pedidoCalculoService, productoService, categoriaService);
        construirUI("Pedido a domicilio");
    }

    @Override
    protected Component construirBloqueDetalles() {
        direccion.setWidthFull();
        direccion.setRequired(true);
        direccion.setPlaceholder("Calle, número, piso...");
        direccion.setClearButtonVisible(true);

        /* Recalcula la habilitación del botón principal mientras se escribe */
        direccion.setValueChangeMode(ValueChangeMode.EAGER);
        direccion.addValueChangeListener(e -> refrescarCarrito());

        Span info = new Span("Métodos de pago: Tarjeta / PayPal / Efectivo.");
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout v = new VerticalLayout(direccion, info);
        v.setPadding(false);
        v.setSpacing(false);
        v.getStyle().set("gap", "10px");
        return v;
    }

    @Override
    protected boolean puedeContinuar() {
        return carrito != null
                && carrito.getLineaPedidos() != null
                && !carrito.getLineaPedidos().isEmpty()
                && direccion.getValue() != null
                && !direccion.getValue().trim().isBlank();
    }

    @Override
    protected void onContinuar() {
        if (direccion.getValue() == null || direccion.getValue().trim().isBlank()) {
            Notification.show("La dirección es obligatoria", 2500, Notification.Position.MIDDLE);
            return;
        }

        String username = usernameActual();

        getUI().ifPresent(ui -> {
            ui.getSession().setAttribute("pedidoOnlineCarrito", carrito);
            ui.getSession().setAttribute("pedidoOnlineTipo", TipoPedidoCliente.DOMICILIO);
            ui.getSession().setAttribute("pedidoOnlineDireccion", direccion.getValue().trim());
            ui.getSession().setAttribute("pedidoOnlineUsername", username);
            ui.navigate("cliente/pedido/online/pasarela");
        });
    }
}