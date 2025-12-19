package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@PageTitle("Detalle Comanda | Cocinero")
@Route(value = "empleado/cocinero/comanda", layout = MainLayout.class)
@Secured("ROLE_COCINERO")
public class DetalleComandaView extends VerticalLayout implements HasUrlParameter<String> {

    // SERVICIOS (transient para Sonar/Vaadin)
    private final transient PedidoService pedidoService;

    // ESTADO
    private transient Pedido pedidoActual;

    // COMPONENTES UI
    private final Grid<LineaPedido> grid = new Grid<>(LineaPedido.class, false);
    private final Select<EstadoCocina> estadoSelect = new Select<>();
    private final Span infoMesa = new Span();
    private final Span infoCodigo = new Span();
    private final Span infoTotal = new Span();

    public DetalleComandaView(PedidoService pedidoService) {

        this.pedidoService = pedidoService;

        setSpacing(false);
        setPadding(true);
        setWidthFull();

        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Detalle de Comanda");
        titulo.getStyle().set("margin", "0");
        add(titulo);

        // INFORMACIÓN DEL PEDIDO

        VerticalLayout cardInfo = crearCard();
        cardInfo.getStyle().set("gap", "10px");

        infoMesa.getStyle().set("font-weight", "600");
        infoCodigo.getStyle().set("font-weight", "600");
        infoTotal.getStyle().set("font-weight", "600");
        infoTotal.getStyle().set("color", "var(--lumo-success-color)");

        HorizontalLayout filaInfo = new HorizontalLayout(infoMesa, infoCodigo, infoTotal);
        filaInfo.setWidthFull();
        filaInfo.getStyle().set("gap", "24px");

        cardInfo.add(filaInfo);
        add(cardInfo);

        // PRODUCTOS

        H3 tituloProductos = new H3("Productos");
        tituloProductos.getStyle().set("margin", "6px 0 0 0");
        add(tituloProductos);

        VerticalLayout cardProductos = crearCard();
        cardProductos.getStyle().set("gap", "12px");

        configurarGrid();
        grid.setWidthFull();
        grid.setHeight("300px");

        cardProductos.add(grid);
        add(cardProductos);

        // ESTADO Y BOTONES

        H3 tituloEstado = new H3("Cambiar Estado");
        tituloEstado.getStyle().set("margin", "6px 0 0 0");
        add(tituloEstado);

        VerticalLayout cardEstado = crearCard();
        cardEstado.getStyle().set("gap", "12px");

        configurarEstado();
        estadoSelect.setWidthFull();

        Button confirmar = new Button("✅ Confirmar cambio de estado");
        confirmar.getStyle().set("font-weight", "600");
        confirmar.setWidthFull();
        confirmar.addClickListener(e -> cambiarEstado());

        Button volver = new Button("⬅️ Volver");
        volver.setWidth("260px");
        volver.addClickListener(e -> UI.getCurrent().navigate(ComandasView.class));

        HorizontalLayout filaEstado = new HorizontalLayout(estadoSelect, confirmar);
        filaEstado.setWidthFull();
        filaEstado.getStyle().set("gap", "12px");

        HorizontalLayout filaVolver = new HorizontalLayout(volver);
        filaVolver.setWidthFull();
        filaVolver.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        cardEstado.add(filaEstado, filaVolver);
        add(cardEstado);
    }

    @Override
    public void setParameter(BeforeEvent event, String idPedidoStr) {
        try {
            java.util.UUID idPedido = java.util.UUID.fromString(idPedidoStr);
            pedidoActual = pedidoService.obtenerPedidoPorId(idPedido);
            if (pedidoActual == null) {
                Notification.show("❌ Comanda no encontrada", 4000, Notification.Position.MIDDLE);
                UI.getCurrent().navigate(ComandasView.class);
                return;
            }
            refrescar();
        } catch (IllegalArgumentException e) {
            Notification.show("❌ ID de comanda inválido", 4000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate(ComandasView.class);
        } catch (Exception e) {
            Notification.show("❌ Error: " + e.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void configurarGrid() {
        grid.addColumn(lp -> lp.getProducto().getNombre())
                .setHeader("Producto")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(LineaPedido::getCantidad)
                .setHeader("Cantidad")
                .setAutoWidth(true);

        grid.addColumn(lp -> lp.getProducto().getPrecio() + " €")
                .setHeader("Precio Unit.")
                .setAutoWidth(true);

        grid.addColumn(lp -> lp.calcularPrecio() + " €")
                .setHeader("Subtotal")
                .setAutoWidth(true);
    }

    private void configurarEstado() {
        estadoSelect.setLabel("Estado");
        estadoSelect.setItems(EstadoCocina.values());
    }

    private void cambiarEstado() {
        EstadoCocina nuevoEstado = estadoSelect.getValue();

        if (nuevoEstado == null) {
            Notification.show("⚠️ Selecciona un estado", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (pedidoActual.getEstadoCocina() == nuevoEstado) {
            Notification.show("ℹ️ El estado ya es el mismo", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoActual = pedidoService.cambiarEstadoCocina(pedidoActual.getId(), nuevoEstado);
            Notification.show("✅ Estado actualizado a " + nuevoEstado, 3000, Notification.Position.MIDDLE);
            refrescar();
        } catch (Exception e) {
            Notification.show("❌ Error: " + e.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void refrescar() {
        if (pedidoActual != null) {
            String numMesa = pedidoActual.getReservaMesa() != null ?
                    String.valueOf(pedidoActual.getReservaMesa().getNumeroMesa()) : "N/A";
            infoMesa.setText("Mesa: " + numMesa);
            infoCodigo.setText("Código: " + pedidoActual.getCodigo());
            infoTotal.setText("Total: " + pedidoActual.calcularPrecioTotal() + " €");

            grid.setItems(pedidoActual.getLineaPedidos());
            estadoSelect.setValue(pedidoActual.getEstadoCocina());
        }
    }

    private VerticalLayout crearCard() {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();

        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");

        return card;
    }
}

