package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.cocina.CocineroService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("cocinero/pendientes")
@PageTitle("Pedidos pendientes | Cocina")
public class PedidosPendientesCocinaView extends VerticalLayout {

    private final CocineroService cocineroService;
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    public PedidosPendientesCocinaView(CocineroService cocineroService) {
        this.cocineroService = cocineroService;

        setSizeFull();
        add(new H2("Pedidos pendientes de aceptación"));

        configurarGrid();
        cargar();

        add(grid);
    }

    private void configurarGrid() {
        grid.addColumn(Pedido::getCodigo).setHeader("Código");
        grid.addColumn(p -> p.getReservaMesa() != null
                ? "Mesa " + p.getReservaMesa().getNumeroMesa()
                : "Cliente").setHeader("Origen");

        grid.addComponentColumn(p -> {
            Button aceptar = new Button("Aceptar");
            aceptar.addClickListener(e -> aceptar(p));
            return aceptar;
        }).setHeader("Acción");

        grid.addComponentColumn(p -> {
            VerticalLayout l = new VerticalLayout();
            for (LineaPedido lp : p.getLineaPedidos()) {
                l.add(lp.getProducto().getNombre() + " x" + lp.getCantidad());
            }
            return l;
        }).setHeader("Productos");
    }

    private void aceptar(Pedido p) {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        cocineroService.aceptarPedido(p.getCodigo(), user);
        cargar();
    }

    private void cargar() {
        grid.setItems(cocineroService.listarPendientesAceptacion());
    }
}