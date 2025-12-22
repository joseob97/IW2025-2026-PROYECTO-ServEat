package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "cliente/pedido/domicilio", layout = MainLayout.class)
@PageTitle("Pedido a domicilio")
@Secured("ROLE_CLIENTE")
public class CrearPedidoDomicilioView extends CrearPedidoRecogerView {

    private final TextField direccion = new TextField("Dirección de entrega");

    public CrearPedidoDomicilioView(PedidoService pedidoService,
                                    ProductoService productoService,
                                    CategoriaService categoriaService) {
        super(pedidoService, productoService, categoriaService);

        direccion.setRequired(true);
        direccion.setWidth("420px");

        addComponentAtIndex(1, direccion);
    }

    @Override
    protected void continuarPago() {

        if (direccion.isEmpty()) {
            Notification.show("La dirección es obligatoria");
            return;
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        getUI().ifPresent(ui -> {
            ui.getSession().setAttribute("pedidoOnlineCarrito", carrito);
            ui.getSession().setAttribute("pedidoOnlineTipo", TipoPedidoCliente.DOMICILIO);
            ui.getSession().setAttribute("pedidoOnlineDireccion", direccion.getValue());
            ui.getSession().setAttribute("pedidoOnlineUsername", username);
            ui.navigate("cliente/pedido/online/pasarela");
        });
    }
}