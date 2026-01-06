package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.cocina.CocineroService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@PageTitle("Cocina | Pedidos de hoy")
@Route(value = "empleado/cocinero/hoy", layout = MainLayout.class)
@Secured("ROLE_COCINERO")
public class PedidosCocinaHoyView extends PedidosCocinaAbstractaView {

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public PedidosCocinaHoyView(CocineroService cocineroService,
                                PedidoCalculoService pedidoCalculoService) {
        super(cocineroService, pedidoCalculoService);

        this.pageSize = 10;

        Button irHistorico = navButton("Ver histórico", "empleado/cocinero/historico");
        initView("Pedidos de hoy (Cocina)", irHistorico, "520px");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        aplicarHoyEnFechas();
        pageIndex = 0;
        cargarPagina(pageIndex);
    }

    @Override
    protected boolean usarFiltroEstado() {
        return true;
    }

    @Override
    protected void limpiarFiltros() {
        aplicarHoyEnFechas();
        filtroEstado.clear();
        filtroMesa.clear();
    }

    @Override
    protected void configurarGridColumnas() {
        configurarColumnasCocinaBase(HORA_FMT, "Hora", "Actualizar");
    }

    @Override
    protected Page<Pedido> buscar(Pageable pageable,
                                  java.time.LocalDateTime desde,
                                  java.time.LocalDateTime hasta,
                                  EstadoCocina estado,
                                  Integer mesa) {
        return cocineroService.buscarPedidosCocinaHoy(desde, hasta, estado, mesa, pageable);
    }

    private void aplicarHoyEnFechas() {
        LocalDate hoy = LocalDate.now();
        desde.setValue(hoy);
        hasta.setValue(hoy);
    }
}