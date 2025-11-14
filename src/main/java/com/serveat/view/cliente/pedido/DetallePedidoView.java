package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Detalle pedido | ServEat")
@Route(value = "cliente/pedido", layout = MainLayout.class)
public class DetallePedidoView extends VerticalLayout implements HasUrlParameter<String> {

    private final PedidoService pedidoService;

    private Pedido pedido;
    private final Grid<LineaPedido> grid = new Grid<>(LineaPedido.class, false);
    private final Select<String> estados = new Select<>();

    public DetallePedidoView(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
        setSizeFull();
        setSpacing(true);
        setPadding(true);
    }

    @Override
    public void setParameter(BeforeEvent event, String codigoPedido) {
        this.pedido = pedidoService.obtenerPorCodigo(codigoPedido);

        removeAll();

        H2 title = new H2("Pedido " + pedido.getCodigo());

        configurarGrid();
        configurarEstados();

        FormLayout form = new FormLayout();
        form.add(estados);

        add(title, form, grid);

        grid.setItems(pedido.getLineaPedidos());
    }

    private void configurarGrid() {
        grid.addColumn(lp -> lp.getProducto().getNombre()).setHeader("Producto");
        grid.addColumn(LineaPedido::getCantidad).setHeader("Cantidad");
        grid.addColumn(lp -> lp.getPrecioUnitario().toString()).setHeader("Precio unit.");
        grid.addColumn(lp -> lp.calcularPrecio().toString()).setHeader("Subtotal");
    }

    private void configurarEstados() {
        estados.setLabel("Estado del pedido");
        estados.setItems("CREADO", "EN_PREPARACION", "PREPARADO", "EN_REPARTO", "ENTREGADO");
        estados.setValue(pedido.getEstado());

        Button guardar = new Button("Actualizar estado", e -> {
            pedidoService.cambiarEstado(pedido.getCodigo(), estados.getValue());
            getUI().ifPresent(ui -> ui.navigate(ListaPedidosView.class));
        });

        add(guardar);
    }
}