package com.serveat.view.empleado.camarero;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

@PageTitle("Cancelar Pedido | Camarero")
@Route(value = "empleado/camarero/pedidos/cancelar", layout = MainLayout.class)
@Secured("ROLE_CAMARERO")
public class CancelarPedidoView extends VerticalLayout {

    // SERVICIO
    private final transient PedidoService pedidoService;

    // UI
    private final IntegerField filtroMesa = new IntegerField("Filtrar por mesa");
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);
    private final TextArea motivo = new TextArea("Motivo de la cancelación");
    private final Button cancelar = new Button("❌ Cancelar pedido");

    // ESTADO
    private Pedido pedidoSeleccionado;

    public CancelarPedidoView(PedidoService pedidoService) {
        this.pedidoService = pedidoService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();

        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        // TÍTULO
        H3 titulo = new H3("Cancelar pedido en curso");
        add(titulo);

        // CARD FILTRO
        VerticalLayout cardFiltro = crearCard();
        filtroMesa.setMin(1);
        filtroMesa.setClearButtonVisible(true);
        filtroMesa.addValueChangeListener(e -> cargarPedidos());

        cardFiltro.add(filtroMesa);
        add(cardFiltro);

        // CARD GRID
        VerticalLayout cardGrid = crearCard();
        configurarGrid();
        grid.setWidthFull();
        grid.setHeight("360px");

        cardGrid.add(grid);
        add(cardGrid);

        // CARD CANCELACIÓN
        VerticalLayout cardCancelacion = crearCard();

        motivo.setWidthFull();
        motivo.setMinHeight("120px");
        motivo.setPlaceholder("Indica el motivo de la cancelación (obligatorio)");

        cancelar.setEnabled(false);
        cancelar.addClickListener(e -> confirmarCancelacion());

        HorizontalLayout filaBoton = new HorizontalLayout(cancelar);
        filaBoton.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        filaBoton.setWidthFull();

        cardCancelacion.add(motivo, filaBoton);
        add(cardCancelacion);

        cargarPedidos();
    }

    // GRID

    private void configurarGrid() {

        grid.addColumn(Pedido::getCodigo)
                .setHeader("Código")
                .setAutoWidth(true);

        grid.addColumn(p ->
                        p.getReservaMesa() != null
                                ? p.getReservaMesa().getNumeroMesa()
                                : "-")
                .setHeader("Mesa")
                .setAutoWidth(true);

        grid.addColumn(p -> p.getEstado().name())
                .setHeader("Estado pedido")
                .setAutoWidth(true);

        grid.addColumn(p ->
                        p.getEstadoCocina() != null
                                ? p.getEstadoCocina().name()
                                : EstadoCocina.PENDIENTE_ACEPTACION.name())
                .setHeader("Estado cocina")
                .setAutoWidth(true);

        grid.addSelectionListener(e -> {
            pedidoSeleccionado = e.getFirstSelectedItem().orElse(null);
            cancelar.setEnabled(pedidoSeleccionado != null);
        });
    }

    // ACCIONES

    private void cargarPedidos() {
        Integer mesa = filtroMesa.getValue();

        if (mesa == null) {
            grid.setItems(pedidoService.listarPedidosCancelables());
        } else {
            grid.setItems(pedidoService.listarPedidosCancelablesPorMesa(mesa));
        }

        cancelar.setEnabled(false);
        pedidoSeleccionado = null;
    }

    private void confirmarCancelacion() {

        if (pedidoSeleccionado == null) return;

        if (motivo.getValue() == null || motivo.getValue().isBlank()) {
            Notification.show("Debes indicar un motivo", 3000, Notification.Position.MIDDLE);
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar cancelación");
        dialog.setText("¿Seguro que deseas cancelar el pedido " + pedidoSeleccionado.getCodigo() + "?");

        dialog.setCancelable(true);
        dialog.setConfirmText("Sí, cancelar");

        dialog.addConfirmListener(e -> ejecutarCancelacion());
        dialog.open();
    }

    private void ejecutarCancelacion() {

        try {
            String camarero = SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getName();

            pedidoService.cancelarPedido(
                    pedidoSeleccionado.getCodigo(),
                    motivo.getValue(),
                    camarero
            );

            Notification.show("Pedido cancelado correctamente", 3000, Notification.Position.MIDDLE);
            motivo.clear();
            cargarPedidos();

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    // UI HELPERS

    private VerticalLayout crearCard() {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();

        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");
        card.getStyle().set("gap", "12px");

        return card;
    }
}