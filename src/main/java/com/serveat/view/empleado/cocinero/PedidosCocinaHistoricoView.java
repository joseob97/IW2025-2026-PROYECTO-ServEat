package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.cocina.CocineroService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;

import java.time.format.DateTimeFormatter;

@PageTitle("Cocina | Histórico de pedidos")
@Route(value = "empleado/cocinero/historico", layout = MainLayout.class)
@Secured("ROLE_COCINERO")
public class PedidosCocinaHistoricoView extends PedidosCocinaAbstractaView {

    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PedidosCocinaHistoricoView(CocineroService cocineroService,
                                      PedidoCalculoService pedidoCalculoService) {
        super(cocineroService, pedidoCalculoService);

        this.pageSize = 15;

        Button irHoy = navButton("Ver pedidos de hoy", "empleado/cocinero/hoy");

        initView("Histórico de pedidos (Cocina)", irHoy, "560px");
    }

    @Override
    protected boolean usarFiltroEstado() {
        return true;
    }

    @Override
    protected void limpiarFiltros() {
        desde.clear();
        hasta.clear();
        filtroEstado.clear();
        filtroMesa.clear();
    }

    @Override
    protected void configurarGridColumnas() {
        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(FECHA_HORA_FMT) : "-")
                .setHeader("Fecha/Hora")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(Pedido::getCodigo)
                .setHeader("Código")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> {
                    if (p.getTipoPedido() == null) return "Cliente";
                    return switch (p.getTipoPedido()) {
                        case DOMICILIO -> "Domicilio";
                        case RECOGER -> "Recogida";
                        default -> (p.getReservaMesa() != null && p.getReservaMesa().getNumeroMesa() != null)
                                ? "Mesa " + p.getReservaMesa().getNumeroMesa()
                                : "Mesa";
                    };
                })
                .setHeader("Origen")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getEstadoCocina() != null ? p.getEstadoCocina().name() : "-")
                .setHeader("Estado cocina")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> {
                    try {
                        return pedidoCalculoService.calcularTotalPedido(p) + " €";
                    } catch (Exception ex) {
                        return "-";
                    }
                })
                .setHeader("Total")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(p -> {
            Button ver = new Button("Ver/Actualizar");
            ver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            ver.getStyle().set("font-weight", "700");
            ver.addClickListener(e -> {
                if (p.getId() == null) {
                    notifyError("Pedido sin ID");
                    return;
                }
                UI.getCurrent().navigate(DetalleComandaView.class, p.getId().toString());
            });
            return ver;
        }).setHeader("Acciones").setAutoWidth(true).setFlexGrow(0);
    }

    @Override
    protected Page<Pedido> buscar(Pageable pageable,
                                  java.time.LocalDateTime desde,
                                  java.time.LocalDateTime hasta,
                                  EstadoCocina estado,
                                  Integer mesa) {
        return cocineroService.buscarPedidosCocinaHistorico(desde, hasta, estado, mesa, pageable);
    }
}