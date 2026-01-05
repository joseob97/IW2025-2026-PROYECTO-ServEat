package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.seguimiento.PedidoSeguimientoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;

import java.time.LocalDateTime;

@PageTitle("Seguimiento | Cliente")
@Route(value = "cliente/pedidos/seguimiento/activos", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class SeguimientoPedidosActivosView extends SeguimientoPedidosAbstractaView {

    private final Button btnAnteriores = new Button("Pedidos anteriores");
    private final Button btnVolver = new Button("Volver a mis pedidos");

    public SeguimientoPedidosActivosView(PedidoSeguimientoService seguimientoService) {
        super(seguimientoService);

        btnAnteriores.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SeguimientoPedidosAnterioresView.class)));
        btnVolver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(ConsultaPedidosView.class)));

        HorizontalLayout accionesTop = new HorizontalLayout(btnRefrescar, btnAnteriores, btnVolver);
        accionesTop.setSpacing(false);
        accionesTop.getStyle().set("gap", "10px");
        accionesTop.setAlignItems(FlexComponent.Alignment.CENTER);

        construirUI("Seguimiento de pedidos activos", accionesTop);
    }

    @Override
    protected void configurarColumnasExtraGrid() {
        grid.addComponentColumn(this::crearBotonAcciones)
                .setHeader("Acciones")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    private Component crearBotonAcciones(Pedido p) {
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

    @Override
    protected Page<Pedido> buscarPagina(String username,
                                        LocalDateTime desde,
                                        LocalDateTime hasta,
                                        EstadoPedido estadoPedido,
                                        EstadoCocina estadoCocina,
                                        EstadoReparto estadoReparto,
                                        Pageable pageable) {
        return seguimientoService.buscarActivosCliente(
                username,
                desde, hasta,
                estadoPedido,
                estadoCocina,
                estadoReparto,
                pageable
        );
    }
}