package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.service.cocina.CocineroService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("cocinero/preparacion")
@PageTitle("Pedidos en cocina | Cocina")
public class PedidosEnPreparacionView extends VerticalLayout {

    private final CocineroService cocineroService;
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    public PedidosEnPreparacionView(CocineroService cocineroService) {
        this.cocineroService = cocineroService;

        setSizeFull();
        add(new H2("Pedidos en preparación"));

        configurarGrid();
        cargar();

        add(grid);
    }

    private void configurarGrid() {
        grid.addColumn(Pedido::getCodigo).setHeader("Código");
        grid.addColumn(p -> p.getEstadoCocina().name()).setHeader("Estado cocina");

        grid.addComponentColumn(p -> {
            Button preparar = new Button("En preparación");
            preparar.setEnabled(p.getEstadoCocina() == EstadoCocina.ACEPTADO);

            preparar.addClickListener(e -> marcarPreparacion(p));

            Button listo = new Button("Listo");
            listo.setEnabled(p.getEstadoCocina() == EstadoCocina.EN_PREPARACION);

            listo.addClickListener(e -> marcarListo(p));

            return new HorizontalLayout(preparar, listo);
        }).setHeader("Acciones");

        grid.addComponentColumn(p -> {
            VerticalLayout l = new VerticalLayout();
            for (LineaPedido lp : p.getLineaPedidos()) {
                l.add(lp.getProducto().getNombre() + " x" + lp.getCantidad());
            }
            return l;
        }).setHeader("Productos");
    }

    private void marcarPreparacion(Pedido p) {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        cocineroService.marcarEnPreparacion(p.getCodigo(), user);
        cargar();
    }

    private void marcarListo(Pedido p) {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        cocineroService.marcarListo(p.getCodigo(), user);
        cargar();
    }

    private void cargar() {
        grid.setItems(cocineroService.listarPedidosEnCurso());
    }
}