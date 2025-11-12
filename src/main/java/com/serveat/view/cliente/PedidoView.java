package com.serveat.view.cliente;

import com.serveat.domain.menu.Plato;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.usuario.Cliente;
import com.serveat.service.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.serveat.service.impl.PedidoServiceImpl;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.flow.server.VaadinSession;



@PageTitle("Mi Pedido")
@Route(value = "pedido", layout = MainLayout.class)
public class PedidoView extends VerticalLayout {

    private final PedidoService pedidoService;
    private Pedido pedidoActual;

    private Grid<Plato> gridPlatos;
    private Button confirmarButton;
    private Button vaciarButton;

    @Autowired
    public PedidoView(PedidoService pedidoService) {
        this.pedidoService = pedidoService;

        // 🔹 Intentamos recuperar el pedido guardado en sesión
        Pedido pedidoSesion = VaadinSession.getCurrent().getAttribute(Pedido.class);

        if (pedidoSesion != null) {
            this.pedidoActual = pedidoSesion;
        } else {
            // Si no existe, creamos uno nuevo (cliente null = invitado)
            this.pedidoActual = pedidoService.crearPedido(null);
            VaadinSession.getCurrent().setAttribute(Pedido.class, pedidoActual);
        }

        configurarVista();
    }

    private void configurarVista() {
        add(new H2("Mi Pedido"));

        gridPlatos = new Grid<>(Plato.class, false);
        gridPlatos.addColumn(Plato::getNombre).setHeader("Plato");
        gridPlatos.addColumn(p -> p.getPrecio() + " €").setHeader("Precio");
        gridPlatos.addColumn(Plato::getDescripcion).setHeader("Descripción");

        confirmarButton = new Button("Confirmar Pedido", e -> confirmarPedido());
        vaciarButton = new Button("Vaciar Pedido", e -> vaciarPedido());

        HorizontalLayout botones = new HorizontalLayout(confirmarButton, vaciarButton);
        add(gridPlatos, botones);
    }

    private void confirmarPedido() {
        pedidoService.guardarPedido(pedidoActual);
        Notification.show("Pedido confirmado correctamente ✅");

        // 🔹 Limpiamos la sesión y creamos un nuevo pedido vacío
        VaadinSession.getCurrent().setAttribute(Pedido.class, null);
        pedidoActual = pedidoService.crearPedido(null);
        gridPlatos.setItems();
    }


    private void vaciarPedido() {
        pedidoActual.getDetalles().clear();
        Notification.show("Pedido vaciado");
    }


    public void agregarPlato(Plato plato) {
        pedidoActual = pedidoService.agregarPlato(pedidoActual, plato, 1);

        // 🔹 Guardamos el pedido actualizado en sesión
        VaadinSession.getCurrent().setAttribute(Pedido.class, pedidoActual);

        gridPlatos.setItems(
                pedidoActual.getDetalles().stream()
                        .map(detalle -> detalle.getPlato())
                        .toList()
        );
    }

}
