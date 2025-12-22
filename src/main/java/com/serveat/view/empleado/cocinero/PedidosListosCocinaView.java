package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.service.pedido.PedidoService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("cocinero/listos")
@PageTitle("Pedidos listos | Cocina")
public class PedidosListosCocinaView extends VerticalLayout {

    private final PedidoService pedidoService;
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    public PedidosListosCocinaView(PedidoService pedidoService) {
        this.pedidoService = pedidoService;

        setSizeFull();
        add(new H2("Pedidos listos para servir / reparto"));

        grid.addColumn(Pedido::getCodigo).setHeader("Código");
        grid.addColumn(p -> p.getTipoPedido().name()).setHeader("Tipo");
        grid.addColumn(p -> p.getEstadoReparto() != null
                ? p.getEstadoReparto().name()
                : "-").setHeader("Reparto");

        grid.setItems(
                pedidoService.listarPedidos().stream()
                        .filter(p -> p.getEstadoCocina() == EstadoCocina.LISTO)
                        .toList()
        );

        add(grid);
    }
}