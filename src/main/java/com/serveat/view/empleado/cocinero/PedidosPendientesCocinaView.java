package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.cocina.CocineroService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

@Route(value = "empleado/cocinero/pendientes", layout = MainLayout.class)
@PageTitle("Pedidos pendientes | Cocina")
@Secured("ROLE_COCINERO")
public class PedidosPendientesCocinaView extends PedidosCocinaAbstractaView {

    public PedidosPendientesCocinaView(CocineroService cocineroService) {
        super(cocineroService, null);

        this.pageSize = 10;

        Button verHoy = navButton("Ver pedidos de hoy", "empleado/cocinero/hoy");
        initView("Pedidos pendientes de aceptación", verHoy, "520px");
    }

    @Override
    protected boolean usarFiltroEstado() {
        return false;
    }

    @Override
    protected void limpiarFiltros() {
        desde.clear();
        hasta.clear();
        filtroMesa.clear();
    }

    @Override
    protected void configurarGridColumnas() {
        grid.addColumn(Pedido::getCodigo)
                .setHeader("Código")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p ->
                        p.getReservaMesa() != null && p.getReservaMesa().getNumeroMesa() != null
                                ? "Mesa " + p.getReservaMesa().getNumeroMesa()
                                : "Recogida / Domicilio")
                .setHeader("Origen")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(p -> {
            Button aceptar = new Button("Aceptar");
            aceptar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            aceptar.addClickListener(e -> aceptar(p));

            Button descartar = new Button("Descartar");
            descartar.addThemeVariants(ButtonVariant.LUMO_ERROR);
            descartar.addClickListener(e -> descartar(p));

            HorizontalLayout acciones = new HorizontalLayout(aceptar, descartar);
            acciones.setSpacing(true);
            return acciones;
        }).setHeader("Acción").setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(p -> {
            VerticalLayout l = new VerticalLayout();
            l.setPadding(false);
            l.setSpacing(false);
            l.getStyle().set("gap", "4px");

            if (p.getLineaPedidos() != null) {
                for (LineaPedido lp : p.getLineaPedidos()) {
                    String nombre = lp.getProducto() != null && lp.getProducto().getNombre() != null
                            ? lp.getProducto().getNombre()
                            : "Producto";
                    l.add(new Span(nombre + " x" + lp.getCantidad()));
                }
            }

            return l;
        }).setHeader("Productos").setFlexGrow(1);
    }

    @Override
    protected Page<Pedido> buscar(Pageable pageable,
                                  LocalDateTime desde,
                                  LocalDateTime hasta,
                                  EstadoCocina estado,
                                  Integer mesa) {
        return cocineroService.buscarPendientesAceptacion(desde, hasta, mesa, pageable);
    }

    private void aceptar(Pedido p) {
        try {
            String user = SecurityContextHolder.getContext().getAuthentication().getName();
            cocineroService.aceptarPedido(p.getCodigo(), user);
            cargarPagina(pageIndex);
            notifySuccess("Pedido aceptado");
        } catch (Exception e) {
            notifyError(e.getMessage());
        }
    }

    private void descartar(Pedido p) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Descartar pedido");
        dialog.setText("¿Seguro que deseas descartar el pedido " + p.getCodigo() + "?");
        dialog.setConfirmText("Descartar");
        dialog.setCancelText("Cancelar");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                String user = SecurityContextHolder.getContext().getAuthentication().getName();
                cocineroService.cancelarDesdeCocina(p.getCodigo(), "Descartado por cocina", user);
                cargarPagina(pageIndex);
                Notification.show("Pedido descartado", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                notifyError(ex.getMessage());
            }
        });

        dialog.open();
    }
}