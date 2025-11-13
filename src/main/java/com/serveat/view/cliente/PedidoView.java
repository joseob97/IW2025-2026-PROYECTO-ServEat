package com.serveat.view.cliente;

import com.serveat.domain.menu.Plato;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.PedidoService;
import com.serveat.util.MapperUtils;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@PageTitle("Mi Pedido")
@Route(value = "pedido", layout = MainLayout.class)
public class PedidoView extends VerticalLayout {

    private final PedidoService pedidoService;
    private Pedido pedidoActual;

    private Grid<Plato> gridPlatos;
    private Button confirmarButton;
    private Button vaciarButton;

    private H3 totalLabel;

    @Autowired
    public PedidoView(PedidoService pedidoService) {
        this.pedidoService = pedidoService;

        Pedido pedidoSesion = VaadinSession.getCurrent().getAttribute(Pedido.class);

        if (pedidoSesion != null) {
            this.pedidoActual = pedidoSesion;
        } else {
            this.pedidoActual = pedidoService.crearPedido(null);
            VaadinSession.getCurrent().setAttribute(Pedido.class, pedidoActual);
        }

        configurarVista();
    }

    private void configurarVista() {
        add(new H2("Mi Pedido"));

        totalLabel = new H3();
        add(totalLabel);

        gridPlatos = new Grid<>(Plato.class, false);
        gridPlatos.addColumn(Plato::getNombre).setHeader("Plato");
        gridPlatos.addColumn(p -> MapperUtils.formatEuro(p.getPrecio()))
                .setHeader("Precio");
        gridPlatos.addColumn(Plato::getDescripcion).setHeader("Descripción");

        gridPlatos.setItems(
                pedidoActual.getDetalles().stream()
                        .map(detalle -> detalle.getPlato())
                        .toList()
        );

        // ⭐ Actualizamos el total al cargar la vista
        actualizarTotal();

        confirmarButton = new Button("Confirmar Pedido", e -> confirmarPedido());
        vaciarButton = new Button("Vaciar Pedido", e -> vaciarPedido());

        HorizontalLayout botones = new HorizontalLayout(confirmarButton, vaciarButton);
        add(gridPlatos, botones);
    }

    // ⭐ MÉTODO NUEVO: calcula y muestra el total
    private void actualizarTotal() {
        BigDecimal total = pedidoActual.getDetalles().stream()
                .map(d -> d.getPlato().getPrecio())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalLabel.setText("Total del pedido: " + MapperUtils.formatEuro(total));
    }

    private void confirmarPedido() {
        pedidoService.guardarPedido(pedidoActual);
        Notification.show("Pedido confirmado correctamente ✅");

        VaadinSession.getCurrent().setAttribute(Pedido.class, null);
        pedidoActual = pedidoService.crearPedido(null);

        gridPlatos.setItems();
        actualizarTotal();  //
    }

    private void vaciarPedido() {
        pedidoActual.getDetalles().clear();
        gridPlatos.setItems();
        actualizarTotal();
        Notification.show("Pedido vaciado");
    }

    public void agregarPlato(Plato plato) {
        pedidoActual = pedidoService.agregarPlato(pedidoActual, plato, 1);
        VaadinSession.getCurrent().setAttribute(Pedido.class, pedidoActual);

        gridPlatos.setItems(
                pedidoActual.getDetalles().stream()
                        .map(detalle -> detalle.getPlato())
                        .toList()
        );

        actualizarTotal();
    }
}
