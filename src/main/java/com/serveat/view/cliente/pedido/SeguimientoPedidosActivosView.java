package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.seguimiento.PedidoSeguimientoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;

import java.time.LocalDateTime;

@PageTitle("Seguimiento | Cliente")
@Route(value = "cliente/pedidos/seguimiento/activos", layout = com.serveat.view.layout.MainLayout.class)
@Secured("ROLE_CLIENTE")
public class SeguimientoPedidosActivosView extends AbstractSeguimientoPedidosView {

    private final transient PedidoSeguimientoService seguimientoService;

    public SeguimientoPedidosActivosView(PedidoSeguimientoService seguimientoService) {
        super("Seguimiento de pedidos activos", accionesTop());
        this.seguimientoService = seguimientoService;
    }

    private static Component accionesTop() {
        Button btnRefrescar = new Button("Refrescar");
        btnRefrescar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnAnteriores = new Button("Pedidos anteriores");
        Button btnVolver = new Button("Volver a mis pedidos");

        btnRefrescar.addClickListener(e -> btnRefrescar.getUI().ifPresent(ui -> ui.getPage().reload()));
        btnAnteriores.addClickListener(e -> btnAnteriores.getUI().ifPresent(ui -> ui.navigate(SeguimientoPedidosAnterioresView.class)));
        btnVolver.addClickListener(e -> btnVolver.getUI().ifPresent(ui -> ui.navigate(ConsultaPedidosView.class)));

        HorizontalLayout hl = new HorizontalLayout(btnRefrescar, btnAnteriores, btnVolver);
        hl.setSpacing(false);
        hl.getStyle().set("gap", "10px");
        return hl;
    }

    @Override
    protected Page<Pedido> buscar(Pageable pageable, String username, LocalDateTime desde, LocalDateTime hasta,
                                  EstadoPedido estadoPedido, EstadoCocina estadoCocina, EstadoReparto estadoReparto) {
        return seguimientoService.buscarActivosCliente(username, desde, hasta, estadoPedido, estadoCocina, estadoReparto, pageable);
    }

    @Override
    protected boolean mostrarColumnaAcciones() {
        return true;
    }

    @Override
    protected Component crearAcciones(Pedido p) {
        Button acciones = new Button("Acciones");
        acciones.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        acciones.getStyle().set("font-weight", "700");

        ContextMenu menu = new ContextMenu(acciones);
        menu.setOpenOnClick(true);

        menu.addItem("Ver seguimiento", e ->
                getUI().ifPresent(ui -> ui.navigate(SeguimientoPedidoIndividualView.class, p.getCodigo()))
        );

        return acciones;
    }
}