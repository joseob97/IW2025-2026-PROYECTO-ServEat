package com.serveat.view.empleado.camarero;

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

@PageTitle("Gestión de Pedidos | Camarero")
@Route(value = "empleado/camarero/pedidos", layout = MainLayout.class)
public class GestionPedidoCamareroView extends VerticalLayout {

    public GestionPedidoCamareroView(PedidoService pedidoService) {

        TextField codigoPedido = new TextField("Código del pedido");
        Button buscar = new Button("Buscar");

        ComboBox<String> nuevoEstado = new ComboBox<>("Nuevo estado");
        nuevoEstado.setItems("CREADO", "EN_PREPARACION", "LISTO", "SERVIDO");

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