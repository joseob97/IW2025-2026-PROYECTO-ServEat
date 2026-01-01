package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.seguimiento.PedidoSeguimientoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;

import java.time.LocalDateTime;

@PageTitle("Pedidos anteriores | Cliente")
@Route(value = "cliente/pedidos/seguimiento/anteriores", layout = com.serveat.view.layout.MainLayout.class)
@Secured("ROLE_CLIENTE")
public class SeguimientoPedidosAnterioresView extends AbstractSeguimientoPedidosView {

    private final transient PedidoSeguimientoService seguimientoService;

    public SeguimientoPedidosAnterioresView(PedidoSeguimientoService seguimientoService) {
        super("Pedidos anteriores", accionesTop());
        this.seguimientoService = seguimientoService;
    }

    private static Component accionesTop() {
        Button btnRefrescar = new Button("Refrescar");
        btnRefrescar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnActivos = new Button("Volver a activos");

        btnRefrescar.addClickListener(e -> btnRefrescar.getUI().ifPresent(ui -> ui.getPage().reload()));
        btnActivos.addClickListener(e -> btnActivos.getUI().ifPresent(ui -> ui.navigate(SeguimientoPedidosActivosView.class)));

        HorizontalLayout hl = new HorizontalLayout(btnRefrescar, btnActivos);
        hl.setSpacing(false);
        hl.getStyle().set("gap", "10px");
        return hl;
    }

    @Override
    protected Page<Pedido> buscar(Pageable pageable, String username, LocalDateTime desde, LocalDateTime hasta,
                                  EstadoPedido estadoPedido, EstadoCocina estadoCocina, EstadoReparto estadoReparto) {
        return seguimientoService.buscarAnterioresCliente(username, desde, hasta, estadoPedido, estadoCocina, estadoReparto, pageable);
    }

    @Override
    protected boolean mostrarColumnaAcciones() {
        return false;
    }
}