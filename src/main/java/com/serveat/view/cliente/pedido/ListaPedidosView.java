package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@PageTitle("Mis pedidos | ServEat")
@Route(value = "cliente/pedidos", layout = MainLayout.class)
public class ListaPedidosView extends VerticalLayout {

    private final PedidoService pedidoService;
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    public ListaPedidosView(PedidoService pedidoService) {
        this.pedidoService = pedidoService;

        setSizeFull();
        H2 title = new H2("Lista de pedidos");

        configurarGrid();

        add(title, grid);

        actualizarGrid();
    }

    private void configurarGrid() {
        grid.addColumn(Pedido::getCodigo).setHeader("Código");
        grid.addColumn(Pedido::getEstado).setHeader("Estado");
        grid.addColumn(p -> p.getFechaCreacion().toString()).setHeader("Fecha");
        grid.addColumn(p -> p.calcularPrecioTotal().toString()).setHeader("Total (€)");

        grid.addItemClickListener(e ->
                getUI().ifPresent(ui ->
                        ui.navigate(DetallePedidoView.class, e.getItem().getCodigo())
                )
        );
    }

    private void actualizarGrid() {
        List<Pedido> pedidos = pedidoService.listarPedidos();
        grid.setItems(pedidos);
    }
}