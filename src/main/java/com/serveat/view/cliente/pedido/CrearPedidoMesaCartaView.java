package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.Pedido;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@PageTitle("Pedido en mesa | Cliente")
@Route(value = "cliente/pedido/mesa", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class CrearPedidoMesaCartaView extends CrearPedidoCartaBaseView {

    private final IntegerField numeroMesa = new IntegerField("Número de mesa");

    public CrearPedidoMesaCartaView(PedidoService pedidoService,
                                    PedidoCarritoService pedidoCarritoService,
                                    PedidoCalculoService pedidoCalculoService,
                                    ProductoService productoService,
                                    CategoriaService categoriaService,
                                    FeatureService featureService) {
        super(pedidoService, pedidoCarritoService, pedidoCalculoService, productoService, categoriaService, featureService);
        construirUI("Pedido en mesa");
        continuar.setText("✅ Confirmar pedido (Mesa)");
    }

    @Override
    protected Component construirBloqueDetalles() {
        numeroMesa.setWidthFull();
        numeroMesa.setMin(1);
        numeroMesa.setStepButtonsVisible(true);
        numeroMesa.setPlaceholder("Ej: 12");

        /* Recalcula la habilitación del botón principal al cambiar el número de mesa */
        numeroMesa.addValueChangeListener(e -> refrescarCarrito());

        Span info = new Span("Pago en mesa (no pasarela).");
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout v = new VerticalLayout(numeroMesa, info);
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
                && numeroMesa.getValue() != null
                && numeroMesa.getValue() > 0;
    }

    @Override
    protected void onContinuar() {
        Integer mesa = numeroMesa.getValue();
        if (mesa == null || mesa <= 0) {
            Notification.show("Número de mesa inválido", 2500, Notification.Position.MIDDLE);
            return;
        }

        try {
            String username = usernameActual();
            Pedido creado = pedidoService.crearPedidoClienteMesa(carrito, username, mesa);

            Notification.show("Pedido creado: " + creado.getCodigo(), 3500, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate(ConsultaPedidosView.class));
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
        }
    }
}