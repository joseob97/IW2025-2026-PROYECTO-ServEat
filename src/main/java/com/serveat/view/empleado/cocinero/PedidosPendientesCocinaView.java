package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.cocina.CocineroService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.context.SecurityContextHolder;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog; // Importar ConfirmDialog

import java.util.List;

@Route("cocinero/pendientes")
@PageTitle("Pedidos pendientes | Cocina")
public class PedidosPendientesCocinaView extends VerticalLayout {

    private final CocineroService cocineroService;
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    public PedidosPendientesCocinaView(CocineroService cocineroService) {
        this.cocineroService = cocineroService;

        setSizeFull();
        add(new H2("Pedidos pendientes de aceptación"));

        configurarGrid();
        cargar();

        add(grid);
    }

    private void configurarGrid() {
        grid.addColumn(Pedido::getCodigo).setHeader("Código");
        grid.addColumn(p -> p.getReservaMesa() != null
                ? "Mesa " + p.getReservaMesa().getNumeroMesa()
                : "Cliente").setHeader("Origen");

        // Columna de acciones con botones Aceptar y Descartar
        grid.addComponentColumn(p -> {
            Button aceptar = new Button("Aceptar");
            aceptar.addClickListener(e -> aceptar(p));
            aceptar.addThemeVariants(ButtonVariant.LUMO_PRIMARY); // Estilo primario para aceptar

            Button descartar = new Button("Descartar");
            descartar.addClickListener(e -> descartar(p));
            descartar.addThemeVariants(ButtonVariant.LUMO_ERROR); // Estilo de error (rojo) para descartar

            return new HorizontalLayout(aceptar, descartar); // Agrupar botones en un HorizontalLayout
        }).setHeader("Acción");

        grid.addComponentColumn(p -> {
            VerticalLayout l = new VerticalLayout();
            for (LineaPedido lp : p.getLineaPedidos()) {
                l.add(lp.getProducto().getNombre() + " x" + lp.getCantidad());
            }
            return l;
        }).setHeader("Productos");
    }

    private void aceptar(Pedido p) {
        try {
            String user = SecurityContextHolder.getContext().getAuthentication().getName();
            cocineroService.aceptarPedido(p.getCodigo(), user);
            cargar();
            Notification.show("Pedido " + p.getCodigo() + " aceptado correctamente.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Error al aceptar el pedido: " + e.getMessage(), 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void descartar(Pedido p) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Descartar Pedido");
        dialog.setText("¿Estás seguro de que quieres descartar el pedido " + p.getCodigo() + "? Esta acción no se puede deshacer.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Descartar");
        dialog.setConfirmButtonTheme("error primary"); // Botón de confirmación rojo
        dialog.setCancelText("Cancelar");

        dialog.addConfirmListener(event -> {
            try {
                String user = SecurityContextHolder.getContext().getAuthentication().getName();
                cocineroService.cancelarDesdeCocina(p.getCodigo(), "Descartado por cocina", user);
                cargar();
                Notification.show("Pedido " + p.getCodigo() + " descartado.", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception e) {
                Notification.show("Error al descartar el pedido: " + e.getMessage(), 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private void cargar() {
        List<Pedido> pedidos = cocineroService.listarPendientesAceptacion();
        grid.setItems(pedidos);
    }
}