package com.serveat.view.empleado.repartidor;

import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.TicketService;
import com.serveat.service.repartidor.RepartidorService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@PageTitle("Mis repartos | Repartidor")
@Route(value = "empleado/repartidor/mis-repartos", layout = MainLayout.class)
@Secured("ROLE_REPARTIDOR")
public class MisRepartosView extends RepartidorPedidosBaseView {

    private final transient RepartidorService repartidorService;
    private final transient PedidoCalculoService pedidoCalculoService;
    private final transient TicketService ticketService;
    private final transient FeatureService featureService;

    private final ComboBox<EstadoReparto> filtroEstado = new ComboBox<>("Estado reparto");

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public MisRepartosView(RepartidorService repartidorService,
                           PedidoCalculoService pedidoCalculoService,
                           TicketService ticketService,
                           FeatureService featureService) {
        this.repartidorService = repartidorService;
        this.pedidoCalculoService = pedidoCalculoService;
        this.ticketService = ticketService;
        this.featureService = featureService;

        filtroEstado.setItems(EstadoReparto.values());
        filtroEstado.setClearButtonVisible(true);

        initBase();
    }

    @Override
    protected String tituloPantalla() {
        return "Mis repartos";
    }

    @Override
    protected String textoInfo() {
        return "Aquí verás los pedidos asignados a ti.";
    }

    @Override
    protected Component filtroExtra() {
        return filtroEstado;
    }

    @Override
    protected void limpiarFiltroExtra() {
        filtroEstado.clear();
    }

    @Override
    protected void configurarGrid() {
        grid.removeAllColumns();

        grid.addColumn(Pedido::getCodigo)
                .setHeader("Pedido")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(FECHA_FMT) : "-")
                .setHeader("Creado")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getCliente() != null ? safe(p.getCliente().getNombre()) : "-")
                .setHeader("Cliente")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getDireccionEntrega() != null ? safe(p.getDireccionEntrega()) : "-")
                .setHeader("Dirección")
                .setAutoWidth(true)
                .setFlexGrow(1);

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
            Pago pago = p.getPago();
            boolean cobrar = pago != null && pago.getMetodo() == MetodoPago.EFECTIVO;
            Span badge = new Span(cobrar ? "COBRAR" : "PAGADO");
            badge.getElement().getThemeList().add("badge " + (cobrar ? "error" : "success"));
            return badge;
        }).setHeader("Pago").setAutoWidth(true).setFlexGrow(0);

        grid.addColumn(p -> p.getEstadoReparto() != null ? p.getEstadoReparto().name() : "-")
                .setHeader("Estado reparto")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(p -> {
            Button acciones = new Button("⚙ Acciones");
            acciones.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            acciones.getStyle().set("font-weight", "700");
            acciones.addClickListener(e -> abrirAccionesPedido(p));
            return acciones;
        }).setHeader("Acciones").setAutoWidth(true).setFlexGrow(0);
    }

    @Override
    protected Page<Pedido> buscarPage(LocalDateTime d, LocalDateTime h, Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        EstadoReparto estado = filtroEstado.getValue();
        return repartidorService.buscarMisRepartos(username, d, h, estado, pageable);
    }

    // Acciones

    private void abrirAccionesPedido(Pedido pedido) {
        Dialog dialog = new Dialog();
        dialog.setWidth("980px");
        dialog.setHeaderTitle("Pedido: " + safe(pedido.getCodigo()));

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "12px");

        content.add(crearResumenPedido(pedido));
        content.add(crearGridLineas(pedido));
        content.add(crearAccionesReparto(dialog, pedido));

        dialog.add(content);
        dialog.open();
    }

    private Component crearResumenPedido(Pedido pedido) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(false);
        row.getStyle().set("gap", "16px");
        row.getStyle().set("flex-wrap", "wrap");

        Span fecha = chip("📅 " + (pedido.getFechaCreacion() != null ? pedido.getFechaCreacion().format(FECHA_FMT) : "-"));
        Span cliente = chip("👤 " + (pedido.getCliente() != null ? safe(pedido.getCliente().getNombre()) : "-"));
        Span estado = chip("🚚 " + (pedido.getEstadoReparto() != null ? pedido.getEstadoReparto().name() : "-"));

        BigDecimal total;
        try {
            total = pedidoCalculoService.calcularTotalPedido(pedido);
        } catch (Exception ex) {
            total = BigDecimal.ZERO;
        }
        Span tot = chipTotal("💶 " + total + " €");

        row.add(fecha, cliente, estado, tot);
        return row;
    }

    private Component crearGridLineas(Pedido pedido) {
        Grid<LineaPedido> g = new Grid<>(LineaPedido.class, false);
        g.setWidthFull();
        g.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        g.getStyle().set("border-radius", "10px");
        g.getStyle().set("overflow", "hidden");

        List<LineaPedido> items =
                pedido.getLineaPedidos() != null ? new ArrayList<>(pedido.getLineaPedidos()) : List.of();
        items.sort(Comparator.comparing(LineaPedido::getCodigo, Comparator.nullsLast(String::compareToIgnoreCase)));
        g.setItems(items);

        g.addColumn(lp -> lp.getProducto() != null ? safe(lp.getProducto().getNombre()) : "-")
                .setHeader("Producto")
                .setFlexGrow(1);

        g.addColumn(LineaPedido::getCantidad)
                .setHeader("Cant.")
                .setAutoWidth(true)
                .setFlexGrow(0);

        g.addColumn(lp -> {
                    BigDecimal unit = (lp.getPrecioUnitario() != null) ? lp.getPrecioUnitario()
                            : (lp.getProducto() != null && lp.getProducto().getPrecio() != null
                            ? lp.getProducto().getPrecio()
                            : BigDecimal.ZERO);
                    return unit + " €";
                })
                .setHeader("Precio ud.")
                .setAutoWidth(true)
                .setFlexGrow(0);

        boolean showIngredientes = featureService.tieneFeature(Feature.INGREDIENTES);
        if (showIngredientes) {
            g.addComponentColumn(lp -> {
                        VerticalLayout box = new VerticalLayout();
                        box.setPadding(false);
                        box.setSpacing(false);
                        box.getStyle().set("gap", "4px");

                        List<String> det = construirDetalleIngredientes(lp.getIngredientes());
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
                    })
                    .setHeader("Ingredientes")
                    .setFlexGrow(1);
        }

        g.addColumn(lp -> pedidoCalculoService.calcularPrecioLinea(lp) + " €")
                .setHeader("Subtotal")
                .setAutoWidth(true)
                .setFlexGrow(0);

        BigDecimal total = BigDecimal.ZERO;
        try {
            total = pedidoCalculoService.calcularTotalPedido(pedido);
        } catch (Exception ignored) { }
        Span totalSpan = new Span("TOTAL: " + total + " €");
        totalSpan.getStyle().set("font-weight", "800");

        VerticalLayout wrap = new VerticalLayout(g, totalSpan);
        wrap.setPadding(false);
        wrap.setSpacing(false);
        wrap.getStyle().set("gap", "10px");
        return wrap;
    }

    private Component crearAccionesReparto(Dialog dialog, Pedido pedido) {
        VerticalLayout box = new VerticalLayout();
        box.setPadding(false);
        box.setSpacing(false);
        box.getStyle().set("gap", "10px");

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);

        if (pedido.getEstadoReparto() == EstadoReparto.ASIGNADO) {
            Button salir = new Button("🚚 Salir a reparto");
            salir.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            salir.addClickListener(e -> confirmarCambioEstado(pedido, "EN_REPARTO"));
            row.add(salir);
        } else if (pedido.getEstadoReparto() == EstadoReparto.EN_REPARTO) {
            Button entregar = new Button("✅ Marcar entregado");
            entregar.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            entregar.addClickListener(e -> confirmarCambioEstado(pedido, "ENTREGADO"));
            row.add(entregar);
        }

        if (pedido.getEstadoReparto() == EstadoReparto.ASIGNADO
                || pedido.getEstadoReparto() == EstadoReparto.EN_REPARTO) {
            Button incidencia = new Button("⚠ Incidencia");
            incidencia.addThemeVariants(ButtonVariant.LUMO_ERROR);
            incidencia.addClickListener(e -> confirmarIncidencia(pedido));
            row.add(incidencia);
        }

        boolean ticketActivo = featureService.tieneFeature(Feature.FACTURACION_TICKET);

        Anchor download = new Anchor();
        download.getStyle().set("display", "none");
        download.getElement().setAttribute("download", true);

        Button ticket = new Button("🧾 Ticket");
        ticket.setEnabled(ticketActivo);
        ticket.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ticket.getStyle().set("font-weight", "700");
        ticket.addClickListener(e -> {
            try {
                byte[] pdf = ticketService.generarTicketRepartidor(pedido.getCodigo());
                StreamResource res = new StreamResource(
                        "ticket-" + pedido.getCodigo() + ".pdf",
                        () -> new ByteArrayInputStream(pdf)
                );
                res.setContentType("application/pdf");
                download.setHref(res);
                download.getElement().callJsFunction("click");
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cerrar = new Button("Cerrar");
        cerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cerrar.addClickListener(e -> dialog.close());

        HorizontalLayout row2 = new HorizontalLayout(ticket, cerrar);
        row2.setWidthFull();
        row2.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        box.add(download, row, row2);
        return box;
    }

    private void confirmarCambioEstado(Pedido p, String accion) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar acción");
        dialog.setText("¿Estás seguro de marcar el pedido como " + accion + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Confirmar");
        dialog.setConfirmButtonTheme("primary");

        dialog.addConfirmListener(event -> {
            try {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                if ("EN_REPARTO".equals(accion)) {
                    repartidorService.marcarEnReparto(p.getCodigo(), username);
                } else if ("ENTREGADO".equals(accion)) {
                    repartidorService.marcarEntregado(p.getCodigo(), username);
                }
                Notification.show("Estado actualizado correctamente", 2500, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                cargarPagina(pageIndex);
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }

    private void confirmarIncidencia(Pedido p) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Reportar Incidencia");
        dialog.setText("¿Estás seguro de reportar una incidencia para este pedido?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Reportar");
        dialog.setConfirmButtonTheme("error");

        dialog.addConfirmListener(event -> {
            try {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                repartidorService.marcarIncidencia(p.getCodigo(), username, "Incidencia reportada por repartidor");
                Notification.show("Incidencia registrada", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                cargarPagina(pageIndex);
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }

    // Helpers

    private Span chip(String text) {
        Span s = new Span(text);
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

    private Span chipTotal(String text) {
        Span s = chip(text);
        s.getStyle().set("background", "var(--lumo-success-color-10pct)");
        s.getStyle().set("border", "1px solid var(--lumo-success-color-50pct)");
        s.getStyle().set("color", "var(--lumo-success-text-color)");
        return s;
    }

    private List<String> construirDetalleIngredientes(Collection<LineaPedidoIngrediente> ingsCol) {
        if (ingsCol == null || ingsCol.isEmpty()) return List.of();

        List<LineaPedidoIngrediente> ings = new ArrayList<>(ingsCol);
        ings.sort(Comparator.comparing(a ->
                a.getIngrediente() != null && a.getIngrediente().getNombre() != null
                        ? a.getIngrediente().getNombre().toLowerCase(Locale.ROOT)
                        : ""
        ));

        List<String> res = new ArrayList<>();

        for (LineaPedidoIngrediente li : ings) {
            if (li == null || li.getIngrediente() == null) continue;
            String n = li.getIngrediente().getNombre();
            if (n == null || n.isBlank()) continue;
            if (!li.isIncluido()) res.add("Sin " + n);
        }

        for (LineaPedidoIngrediente li : ings) {
            if (li == null || li.getIngrediente() == null) continue;

            int extraCant = Math.max(li.getExtraCantidad(), 0);
            if (extraCant <= 0) continue;

            String n = li.getIngrediente().getNombre();
            if (n == null || n.isBlank()) n = "Ingrediente";

            BigDecimal unit = li.getPrecioExtra() == null ? BigDecimal.ZERO : li.getPrecioExtra();
            BigDecimal plus = unit.multiply(BigDecimal.valueOf(extraCant));

            res.add("Extra " + n + " x" + extraCant + " (+" + plus + " €)");
        }

        return res;
    }

    private String safe(String s) {
        return (s == null) ? "-" : s;
    }
}