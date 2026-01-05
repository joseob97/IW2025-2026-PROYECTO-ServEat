package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.seguimiento.PedidoSeguimientoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;

import java.time.LocalDateTime;

@PageTitle("Pedidos anteriores | Cliente")
@Route(value = "cliente/pedidos/seguimiento/anteriores", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class SeguimientoPedidosAnterioresView extends SeguimientoPedidosAbstractaView {

    private final Button btnActivos = new Button("Volver a activos");

    public SeguimientoPedidosAnterioresView(PedidoSeguimientoService seguimientoService) {
        super(seguimientoService);

        btnActivos.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SeguimientoPedidosActivosView.class)));

        HorizontalLayout accionesTop = new HorizontalLayout(btnRefrescar, btnActivos);
        accionesTop.setSpacing(false);
        accionesTop.getStyle().set("gap", "10px");
        accionesTop.setAlignItems(FlexComponent.Alignment.CENTER);

        construirUI("Pedidos anteriores", accionesTop);
    }

    @Override
    protected void configurarColumnasExtraGrid() {
        // No añade columnas extra
    }

    @Override
    protected Page<Pedido> buscarPagina(String username,
                                        LocalDateTime desde,
                                        LocalDateTime hasta,
                                        EstadoPedido estadoPedido,
                                        EstadoCocina estadoCocina,
                                        EstadoReparto estadoReparto,
                                        Pageable pageable) {
        return seguimientoService.buscarAnterioresCliente(
                username,
                desde, hasta,
                estadoPedido,
                estadoCocina,
                estadoReparto,
                pageable
        );
    }
}