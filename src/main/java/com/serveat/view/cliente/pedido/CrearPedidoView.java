package com.serveat.view.cliente.pedido;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.menu.ProductoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Crear Pedido")
@Route(value = "cliente/pedidos/nuevo", layout = MainLayout.class)
public class CrearPedidoView extends VerticalLayout {

    private final PedidoService pedidoService;
    private final ProductoService productoService;

    private final ComboBox<Producto> productoCombo = new ComboBox<>("Producto");
    private final IntegerField cantidadInput = new IntegerField("Cantidad");

    public CrearPedidoView(PedidoService pedidoService, ProductoService productoService) {
        this.pedidoService = pedidoService;
        this.productoService = productoService;

        setPadding(true);

        H2 titulo = new H2("Nuevo Pedido");

        productoCombo.setItems(productoService.buscarPorNombreParcial(""));
        productoCombo.setItemLabelGenerator(Producto::getNombre);

        cantidadInput.setValue(1);
        cantidadInput.setMin(1);

        Button crearPedidoBtn = new Button("Crear Pedido", e -> crearPedido());

        add(titulo, productoCombo, cantidadInput, crearPedidoBtn);
    }

    private void crearPedido() {

        if (productoCombo.isEmpty()) {
            Notification.show("Selecciona un producto");
            return;
        }

        Pedido pedido = pedidoService.crearPedido();

        pedidoService.agregarProducto(
                pedido.getCodigo(),
                productoCombo.getValue().getCodigo(),
                cantidadInput.getValue()
        );

        Notification.show("Pedido creado: " + pedido.getCodigo());
    }
}