package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.math.BigDecimal;
import java.util.UUID;

@PageTitle("Detalle Comanda | Cocinero")
@Route(value = "empleado/cocinero/comanda", layout = MainLayout.class)
@Secured("ROLE_COCINERO")
public class DetalleComandaView extends VerticalLayout implements HasUrlParameter<String> {

    private final transient PedidoService pedidoService;
    private final transient PedidoCalculoService pedidoCalculoService;

    private transient Pedido pedidoActual;

    // UI
    private final Grid<LineaPedido> grid = new Grid<>(LineaPedido.class, false);
    private final Select<EstadoCocina> estadoSelect = new Select<>();

    private final Span chipMesa = chip();
    private final Span chipCodigo = chip();
    private final Span chipEstado = chip();
    private final Span chipTotal = chipTotal();

    public DetalleComandaView(PedidoService pedidoService,
                              PedidoCalculoService pedidoCalculoService) {

        this.pedidoService = pedidoService;
        this.pedidoCalculoService = pedidoCalculoService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        // Header superior (título + volver)
        Button volver = new Button("⬅ Volver");
        volver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        volver.getStyle().set("font-weight", "700");
        volver.addClickListener(e -> UI.getCurrent().navigate(PedidosCocinaHistoricoView.class));

        H3 titulo = new H3("Detalle de Comanda");
        titulo.getStyle().set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(volver, titulo);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(titulo);
        add(header);

        // Card resumen (chips)
        VerticalLayout cardResumen = crearCard();
        cardResumen.getStyle().set("gap", "12px");

        Span sub = new Span("Resumen del pedido");
        sub.getStyle().set("font-weight", "700");
        sub.getStyle().set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout filaChips = new HorizontalLayout(chipMesa, chipCodigo, chipEstado, chipTotal);
        filaChips.setWidthFull();
        filaChips.setSpacing(false);
        filaChips.getStyle().set("gap", "10px");
        filaChips.setWidthFull();
        filaChips.setAlignItems(FlexComponent.Alignment.CENTER);
        filaChips.getStyle().set("flex-wrap", "wrap");
        filaChips.getStyle().set("row-gap", "10px");
        filaChips.getStyle().set("column-gap", "10px");

        cardResumen.add(sub, filaChips);
        add(cardResumen);

        // Card productos (grid)
        VerticalLayout cardProductos = crearCard();
        cardProductos.getStyle().set("gap", "12px");

        Span tProd = new Span("Productos");
        tProd.getStyle().set("font-weight", "800");

        configurarGrid();
        grid.setWidthFull();
        grid.setHeight("320px");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.getStyle().set("border-radius", "10px");
        grid.getStyle().set("overflow", "hidden");

        cardProductos.add(tProd, grid);
        add(cardProductos);

        // Card cambiar estado
        VerticalLayout cardEstado = crearCard();
        cardEstado.getStyle().set("gap", "12px");

        Span tEstado = new Span("Cambiar estado");
        tEstado.getStyle().set("font-weight", "800");

        configurarEstado();
        estadoSelect.setWidthFull();

        Button confirmar = new Button("✅ Confirmar cambio");
        confirmar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        confirmar.getStyle().set("font-weight", "800");
        confirmar.setWidth("220px");
        confirmar.addClickListener(e -> cambiarEstado());

        HorizontalLayout filaAccion = new HorizontalLayout(estadoSelect, confirmar);
        filaAccion.setWidthFull();
        filaAccion.setSpacing(false);
        filaAccion.getStyle().set("gap", "12px");
        filaAccion.setAlignItems(Alignment.END);
        filaAccion.expand(estadoSelect);

        cardEstado.add(tEstado, filaAccion);
        add(cardEstado);
    }

    @Override
    public void setParameter(BeforeEvent event, String idPedidoStr) {
        try {
            UUID idPedido = UUID.fromString(idPedidoStr);
            pedidoActual = pedidoService.obtenerPedidoPorId(idPedido);

            if (pedidoActual == null) {
                Notification.show("❌ Comanda no encontrada", 4000, Notification.Position.MIDDLE);
                UI.getCurrent().navigate(PedidosCocinaHistoricoView.class);
                return;
            }

            refrescar();

        } catch (IllegalArgumentException e) {
            Notification.show("❌ ID de comanda inválido", 4000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate(PedidosCocinaHistoricoView.class);
        } catch (Exception e) {
            Notification.show("❌ Error: " + e.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void configurarGrid() {
        grid.removeAllColumns();

        grid.addColumn(lp -> lp.getProducto() != null ? safe(lp.getProducto().getNombre()) : "-")
                .setHeader("Producto")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(LineaPedido::getCantidad)
                .setHeader("Cant.")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(lp -> {
                    if (lp.getPrecioUnitario() != null) return lp.getPrecioUnitario() + " €";
                    if (lp.getProducto() != null && lp.getProducto().getPrecio() != null) return lp.getProducto().getPrecio() + " €";
                    return "-";
                })
                .setHeader("Precio ud.")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(lp -> {
            VerticalLayout box = new VerticalLayout();
            box.setPadding(false);
            box.setSpacing(false);
            box.getStyle().set("gap", "4px");

            var det = construirDetalleIngredientes(lp.getIngredientes());
            if (det.isEmpty()) {
                Span s = new Span("-");
                s.getStyle().set("color", "var(--lumo-secondary-text-color)");
                box.add(s);
            } else {
                for (String d : det) {
                    Span s = new Span(d);
                    s.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                    s.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    box.add(s);
                }
            }
            return box;
        }).setHeader("Ingredientes").setFlexGrow(1);

        grid.addColumn(lp -> pedidoCalculoService.calcularPrecioLinea(lp) + " €")
                .setHeader("Subtotal")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    private void configurarEstado() {
        estadoSelect.setLabel("Estado de cocina");
        estadoSelect.setItems(EstadoCocina.values());
        estadoSelect.setEmptySelectionAllowed(false);
    }

    private void cambiarEstado() {
        EstadoCocina nuevoEstado = estadoSelect.getValue();

        if (nuevoEstado == null) {
            Notification.show("⚠️ Selecciona un estado", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (pedidoActual != null && pedidoActual.getEstadoCocina() == nuevoEstado) {
            Notification.show("ℹ️ El estado ya es el mismo", 2500, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoActual = pedidoService.cambiarEstadoCocina(pedidoActual.getId(), nuevoEstado);
            Notification.show("✅ Estado actualizado a " + nuevoEstado, 2500, Notification.Position.BOTTOM_START);
            refrescar();
        } catch (Exception e) {
            Notification.show("❌ Error: " + e.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void refrescar() {
        if (pedidoActual == null) return;

        String numMesa = (pedidoActual.getReservaMesa() != null && pedidoActual.getReservaMesa().getNumeroMesa() != null)
                ? String.valueOf(pedidoActual.getReservaMesa().getNumeroMesa())
                : "N/A";

        chipMesa.setText("🪑 Mesa: " + numMesa);
        chipCodigo.setText("🏷️ Código: " + safe(pedidoActual.getCodigo()));
        chipEstado.setText("🍳 Estado: " + (pedidoActual.getEstadoCocina() != null ? pedidoActual.getEstadoCocina().name() : "-"));

        BigDecimal totalPedido = pedidoCalculoService.calcularTotalPedido(pedidoActual);
        chipTotal.setText("💶 Total: " + totalPedido + " €");

        grid.setItems(pedidoActual.getLineaPedidos() != null ? pedidoActual.getLineaPedidos() : java.util.List.of());
        estadoSelect.setValue(pedidoActual.getEstadoCocina());
    }

    // UI helpers

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

    private Span chip() {
        Span s = new Span("-");
        s.getStyle().set("display", "inline-flex");
        s.getStyle().set("align-items", "center");
        s.getStyle().set("padding", "8px 10px");
        s.getStyle().set("border-radius", "999px");
        s.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        s.getStyle().set("background", "var(--lumo-contrast-5pct)");
        s.getStyle().set("font-weight", "700");
        s.getStyle().set("font-size", "var(--lumo-font-size-s)");
        return s;
    }

    private Span chipTotal() {
        Span s = chip();
        s.getStyle().set("background", "var(--lumo-success-color-10pct)");
        s.getStyle().set("border", "1px solid var(--lumo-success-color-50pct)");
        s.getStyle().set("color", "var(--lumo-success-text-color)");
        return s;
    }

    private java.util.List<String> construirDetalleIngredientes(java.util.Collection<com.serveat.domain.pedido.LineaPedidoIngrediente> ingsCol) {
        if (ingsCol == null || ingsCol.isEmpty()) return java.util.List.of();

        java.util.List<com.serveat.domain.pedido.LineaPedidoIngrediente> ings = new java.util.ArrayList<>(ingsCol);
        ings.sort(java.util.Comparator.comparing(a ->
                a.getIngrediente() != null && a.getIngrediente().getNombre() != null
                        ? a.getIngrediente().getNombre().toLowerCase(java.util.Locale.ROOT)
                        : ""
        ));

        java.util.List<String> res = new java.util.ArrayList<>();

        // 1) “Sin X”
        for (var li : ings) {
            if (li == null || li.getIngrediente() == null) continue;
            String n = li.getIngrediente().getNombre();
            if (n == null || n.isBlank()) continue;
            if (!li.isIncluido()) res.add("Sin " + n);
        }

        // 2) “Extra X”
        for (var li : ings) {
            if (li == null || li.getIngrediente() == null) continue;

            int extraCant = Math.max(li.getExtraCantidad(), 0);
            if (extraCant <= 0) continue;

            String n = li.getIngrediente().getNombre();
            if (n == null || n.isBlank()) n = "Ingrediente";

            java.math.BigDecimal unit = li.getPrecioExtra() == null ? java.math.BigDecimal.ZERO : li.getPrecioExtra();
            java.math.BigDecimal plus = unit.multiply(java.math.BigDecimal.valueOf(extraCant));

            res.add("Extra " + n + " x" + extraCant + " (+" + plus + " €)");
        }

        return res;
    }

    private String safe(String s) {
        return s == null ? "-" : s;
    }
}