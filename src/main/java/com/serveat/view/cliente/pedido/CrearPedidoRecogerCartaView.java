package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@PageTitle("Pedido para recoger | Cliente")
@Route(value = "cliente/pedido/recoger", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class CrearPedidoRecogerCartaView extends CrearPedidoCartaBaseView {

    public CrearPedidoRecogerCartaView(PedidoService pedidoService,
                                       PedidoCarritoService pedidoCarritoService,
                                       PedidoCalculoService pedidoCalculoService,
                                       ProductoService productoService,
                                       CategoriaService categoriaService,
                                       FeatureService featureService) {
        super(pedidoService, pedidoCarritoService, pedidoCalculoService, productoService, categoriaService, featureService);
        construirUI("Pedido para recoger");
    }

    @Override
    protected Component construirBloqueDetalles() {
        Span info = new Span("Métodos de pago: Tarjeta / PayPal / Efectivo.");
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return info;
    }

    @Override
    protected boolean puedeContinuar() {
        return carrito != null
                && carrito.getLineaPedidos() != null
                && !carrito.getLineaPedidos().isEmpty();
    }

    @Override
    protected void onContinuar() {
        String username = usernameActual();

        getUI().ifPresent(ui -> {
            ui.getSession().setAttribute("pedidoOnlineCarrito", carrito);
            ui.getSession().setAttribute("pedidoOnlineTipo", TipoPedidoCliente.RECOGER);
            ui.getSession().setAttribute("pedidoOnlineDireccion", null);
            ui.getSession().setAttribute("pedidoOnlineMetodoPago", null);
            ui.getSession().setAttribute("pedidoOnlineUsername", username);
            ui.navigate("cliente/pedido/online/pasarela");
        });
    }
}