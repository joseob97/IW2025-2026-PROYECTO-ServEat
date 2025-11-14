package com.serveat.view.empleado.repartidor;

import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Gestión de Pedidos | Repartidor")
@Route(value = "empleado/repartidor/pedidos", layout = MainLayout.class)
public class GestionPedidoRepartidorView extends VerticalLayout {

    public GestionPedidoRepartidorView(PedidoService pedidoService) {

        TextField codigoPedido = new TextField("Código del pedido");
        Button buscar = new Button("Buscar");

        ComboBox<String> nuevoEstado = new ComboBox<>("Nuevo estado");
        nuevoEstado.setItems("EN_REPARTO", "ENTREGADO");

        Button cambiar = new Button("Cambiar estado");

        buscar.addClickListener(e -> {
            try {
                Pedido p = pedidoService.obtenerPorCodigo(codigoPedido.getValue());
                Notification.show("Estado actual: " + p.getEstado());
            } catch (Exception ex) {
                Notification.show("Pedido no encontrado");
            }
        });

        cambiar.addClickListener(e -> {
            try {
                pedidoService.cambiarEstado(codigoPedido.getValue(), nuevoEstado.getValue());
                Notification.show("Estado actualizado");
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage());
            }
        });

        add(codigoPedido, buscar, nuevoEstado, cambiar);
    }
}