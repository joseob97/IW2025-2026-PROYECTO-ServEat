package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.cocina.CocineroService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
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
        configurarColumnasCocinaBase(FECHA_HORA_FMT, "Fecha/Hora", "Ver/Actualizar");
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