package com.serveat.view.empleado.repartidor;

import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.repartidor.RepartidorService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@PageTitle("Pedidos disponibles | Repartidor")
@Route(value = "empleado/repartidor/pedidos-disponibles", layout = MainLayout.class)
@Secured("ROLE_REPARTIDOR")
public class PedidosDisponiblesView extends RepartidorPedidosBaseView {

    private final transient RepartidorService repartidorService;

    public PedidosDisponiblesView(RepartidorService repartidorService) {
        this.repartidorService = repartidorService;
        initBase();
    }

    @Override
    protected String tituloPantalla() {
        return "Pedidos disponibles";
    }

    @Override
    protected String textoInfo() {
        return "Pedidos a domicilio pendientes de asignación.";
    }

    @Override
    protected void configurarGrid() {
        grid.removeAllColumns();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        grid.addColumn(Pedido::getCodigo).setHeader("Pedido").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(fmt) : "-")
                .setHeader("Recibido").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(p -> p.getCliente() != null ? p.getCliente().getNombre() : "-")
                .setHeader("Cliente").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(p -> p.getDireccionEntrega() != null ? p.getDireccionEntrega() : "-")
                .setHeader("Dirección").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(p -> p.getEstadoReparto() != null ? p.getEstadoReparto().name() : "-")
                .setHeader("Estado reparto").setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(p -> {
            Button asignarme = new Button("📌 Asignarme");
            asignarme.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            asignarme.setEnabled(p.getEstadoReparto() == EstadoReparto.PENDIENTE_ASIGNACION);
            asignarme.addClickListener(e -> confirmarAsignacion(p));
            return asignarme;
        }).setHeader("Acción").setAutoWidth(true).setFlexGrow(0);
    }

    @Override
    protected Page<Pedido> buscarPage(LocalDateTime d, LocalDateTime h, Pageable pageable) {
        return repartidorService.buscarPedidosDisponibles(d, h, pageable);
    }

    @Override
    protected void onEmptyResults() {
        Notification.show("No hay pedidos pendientes de asignación", 2500, Notification.Position.BOTTOM_START);
    }

    private void confirmarAsignacion(Pedido p) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar asignación");
        dialog.setText("¿Confirma la asignación del pedido " + p.getCodigo() + "?");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        dialog.setConfirmText("Asignar");
        dialog.setConfirmButtonTheme("primary");
        dialog.addConfirmListener(event -> procesarAsignacion(p));
        dialog.open();
    }

    private void procesarAsignacion(Pedido p) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            repartidorService.asignarmePedido(p.getCodigo(), username);

            Notification.show("Pedido asignado correctamente ✅", 2500, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            if (pageIndex > 0 && (totalItems - 1) <= (long) pageIndex * pageSize) pageIndex--;
            cargarPagina(pageIndex);

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}