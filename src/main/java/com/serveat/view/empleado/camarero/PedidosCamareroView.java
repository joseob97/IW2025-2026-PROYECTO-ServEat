package com.serveat.view.empleado.camarero;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.pedido.TicketService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@PageTitle("Pedidos | Camarero")
@Route(value = "empleado/camarero/pedidos", layout = MainLayout.class)
@Secured("ROLE_CAMARERO")
public class PedidosCamareroView extends VerticalLayout {

    private final transient PedidoService pedidoService;
    private final transient PedidoCalculoService calculoService;
    private final transient TicketService ticketService;
    private final transient FeatureService featureService;

    private final boolean ingredientesHabilitados;

    private final DatePicker desde = new DatePicker("Desde");
    private final DatePicker hasta = new DatePicker("Hasta");
    private final ComboBox<EstadoPedido> filtroEstadoPedido = new ComboBox<>("Estado pedido");
    private final ComboBox<EstadoCocina> filtroEstadoCocina = new ComboBox<>("Estado cocina");
    private final IntegerField filtroMesa = new IntegerField("Mesa");

    private final Button btnBuscar = new Button("Buscar");
    private final Button btnLimpiar = new Button("Limpiar");

    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);
    private final Button prev = new Button("◀ Anterior");
    private final Button next = new Button("Siguiente ▶");
    private final Span infoPagina = new Span("");

    private int pageIndex = 0;
    private final int pageSize = 10;
    private long totalItems = 0;

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PedidosCamareroView(PedidoService pedidoService,
                               PedidoCalculoService calculoService,
                               TicketService ticketService,
                               FeatureService featureService) {
        this.pedidoService = pedidoService;
        this.calculoService = calculoService;
        this.ticketService = ticketService;
        this.featureService = featureService;

        this.ingredientesHabilitados = featureService.tieneFeature(Feature.INGREDIENTES);

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1280px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Pedidos (Camarero)");
        titulo.getStyle().set("margin", "0");
        add(titulo);

        add(crearBloqueFiltros());
        add(crearBloqueGrid());
        add(crearBloquePaginacion());

        cargarPagina(0);
    }

    private Component crearBloqueFiltros() {
        VerticalLayout card = crearCard();

        filtroEstadoPedido.setItems(EstadoPedido.values());
        filtroEstadoPedido.setClearButtonVisible(true);

        filtroEstadoCocina.setItems(EstadoCocina.values());
        filtroEstadoCocina.setClearButtonVisible(true);

        filtroMesa.setMin(1);
        filtroMesa.setStepButtonsVisible(true);
        filtroMesa.setClearButtonVisible(true);

        btnBuscar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnBuscar.getStyle().set("font-weight", "700");
        btnBuscar.addClickListener(e -> {
            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        btnLimpiar.addClickListener(e -> {
            desde.clear();
            hasta.clear();
            filtroEstadoPedido.clear();
            filtroEstadoCocina.clear();
            filtroMesa.clear();
            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        HorizontalLayout fila1 = new HorizontalLayout(desde, hasta, filtroMesa);
        fila1.setWidthFull();
        fila1.setSpacing(false);
        fila1.getStyle().set("gap", "12px");

        HorizontalLayout fila2 = new HorizontalLayout(filtroEstadoPedido, filtroEstadoCocina, btnBuscar, btnLimpiar);
        fila2.setWidthFull();
        fila2.setSpacing(false);
        fila2.getStyle().set("gap", "12px");
        fila2.setAlignItems(Alignment.END);

        card.add(new H3("Filtros"), fila1, fila2);
        return card;
    }

    private Component crearBloqueGrid() {
        VerticalLayout card = crearCard();

        grid.setWidthFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.addColumn(Pedido::getCodigo)
                .setHeader("Código")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(FECHA_FMT) : "-")
                .setHeader("Fecha")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getReservaMesa() != null ? p.getReservaMesa().getNumeroMesa() : "-")
                .setHeader("Mesa")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getEstado() != null ? p.getEstado().name() : "-")
                .setHeader("Estado pedido")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getEstadoCocina() != null ? p.getEstadoCocina().name() : "-")
                .setHeader("Estado cocina")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> {
                    try {
                        return calculoService.calcularTotalPedido(p) + " €";
                    } catch (Exception ex) {
                        return "-";
                    }
                })
                .setHeader("Total")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(p -> {
                    Button detalles = new Button("Más detalles");
                    detalles.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    detalles.getStyle().set("font-weight", "700");
                    detalles.addClickListener(e -> abrirDetalles(p));
                    return detalles;
                })
                .setHeader("Acciones")
                .setAutoWidth(true)
                .setFlexGrow(0);

        card.add(grid);
        return card;
    }

    private Component crearBloquePaginacion() {
        HorizontalLayout barra = new HorizontalLayout(prev, infoPagina, next);
        barra.setWidthFull();
        barra.setAlignItems(Alignment.CENTER);
        barra.setJustifyContentMode(JustifyContentMode.CENTER);
        barra.setSpacing(false);
        barra.getStyle().set("gap", "12px");

        prev.addClickListener(e -> {
            if (pageIndex > 0) {
                pageIndex--;
                cargarPagina(pageIndex);
            }
        });

        next.addClickListener(e -> {
            int maxPage = (int) Math.max(0, (totalItems - 1) / pageSize);
            if (pageIndex < maxPage) {
                pageIndex++;
                cargarPagina(pageIndex);
            }
        });

        return barra;
    }

    private void cargarPagina(int index) {
        try {
            Pageable pageable = PageRequest.of(index, pageSize);

            LocalDateTime d = toStartOfDay(desde.getValue());
            LocalDateTime h = toEndOfDay(hasta.getValue());

            EstadoPedido ep = filtroEstadoPedido.getValue();
            EstadoCocina ec = filtroEstadoCocina.getValue();
            Integer mesa = filtroMesa.getValue();

            Page<Pedido> page = pedidoService.buscarPedidosFiltrados(d, h, ep, ec, mesa, pageable);

            totalItems = page.getTotalElements();
            grid.setItems(page.getContent());

            int maxPage = (int) Math.max(0, (totalItems - 1) / pageSize);
            prev.setEnabled(index > 0);
            next.setEnabled(index < maxPage);

            long from = totalItems == 0 ? 0 : (index * pageSize + 1L);
            long to = Math.min(totalItems, (long) (index + 1) * pageSize);
            infoPagina.setText("Mostrando " + from + "-" + to + " de " + totalItems);

        } catch (Exception ex) {
            Notification.show("Error cargando pedidos: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void abrirDetalles(Pedido pedidoGrid) {
        try {
            Pedido pedido = pedidoService.obtenerPorCodigo(pedidoGrid.getCodigo());

            Dialog dialog = new Dialog();
            dialog.setWidth("980px");
            dialog.setHeaderTitle("Pedido: " + safe(pedido.getCodigo()));

            VerticalLayout content = new VerticalLayout();
            content.setPadding(false);
            content.setSpacing(false);
            content.getStyle().set("gap", "12px");

            content.add(crearResumenPedido(pedido));
            content.add(crearGridLineas(pedido));
            content.add(crearAccionesPedido(dialog, pedido));

            dialog.add(content);
            dialog.open();

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private Component crearResumenPedido(Pedido pedido) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(false);
        row.getStyle().set("gap", "16px");

        Span fecha = new Span("Fecha: " + (pedido.getFechaCreacion() != null ? pedido.getFechaCreacion().format(FECHA_FMT) : "-"));
        Span mesa = new Span("Mesa: " + (pedido.getReservaMesa() != null ? pedido.getReservaMesa().getNumeroMesa() : "-"));
        Span estado = new Span("Estado: " + (pedido.getEstado() != null ? pedido.getEstado().name() : "-"));
        Span cocina = new Span("Cocina: " + (pedido.getEstadoCocina() != null ? pedido.getEstadoCocina().name() : "-"));

        fecha.getStyle().set("font-weight", "700");
        mesa.getStyle().set("font-weight", "700");
        estado.getStyle().set("font-weight", "700");
        cocina.getStyle().set("font-weight", "700");

        row.add(fecha, mesa, estado, cocina);
        return row;
    }

    private Component crearGridLineas(Pedido pedido) {
        Grid<LineaPedido> g = new Grid<>(LineaPedido.class, false);
        g.setWidthFull();
        g.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        List<LineaPedido> items =
                pedido.getLineaPedidos() != null ? new ArrayList<>(pedido.getLineaPedidos()) : List.of();
        items.sort(Comparator.comparing(LineaPedido::getCodigo, Comparator.nullsLast(String::compareToIgnoreCase)));
        g.setItems(items);

        g.addColumn(lp -> lp.getProducto() != null ? safe(lp.getProducto().getNombre()) : "-")
                .setHeader("Producto").setFlexGrow(1);

        g.addColumn(LineaPedido::getCantidad)
                .setHeader("Cant.").setAutoWidth(true).setFlexGrow(0);

        g.addColumn(lp -> {
                    BigDecimal unit = (lp.getPrecioUnitario() != null) ? lp.getPrecioUnitario()
                            : (lp.getProducto() != null && lp.getProducto().getPrecio() != null ? lp.getProducto().getPrecio() : BigDecimal.ZERO);
                    return unit + " €";
                })
                .setHeader("Precio ud.").setAutoWidth(true).setFlexGrow(0);

        // Los ingredientes solo deben mostrarse si el feature INGREDIENTES está activo.
        // Si no está activo, no se añade la columna y no se filtra información sensible al usuario.
        if (ingredientesHabilitados) {
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
                    .setHeader("Ingredientes").setFlexGrow(1);
        }

        g.addColumn(lp -> calculoService.calcularPrecioLinea(lp) + " €")
                .setHeader("Subtotal").setAutoWidth(true).setFlexGrow(0);

        Span total = new Span("TOTAL: " + calculoService.calcularTotalPedido(pedido) + " €");
        total.getStyle().set("font-weight", "800");

        VerticalLayout wrap = new VerticalLayout(g, total);
        wrap.setPadding(false);
        wrap.setSpacing(false);
        wrap.getStyle().set("gap", "10px");
        return wrap;
    }

    private Component crearAccionesPedido(Dialog dialog, Pedido pedido) {
        VerticalLayout box = new VerticalLayout();
        box.setPadding(false);
        box.setSpacing(false);
        box.getStyle().set("gap", "10px");

        Button generarTicket = new Button("🧾 Generar ticket");
        generarTicket.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generarTicket.getStyle().set("font-weight", "800");

        Anchor download = new Anchor();
        download.getStyle().set("display", "none");
        download.getElement().setAttribute("download", true);

        generarTicket.addClickListener(e -> {
            try {
                byte[] pdf = ticketService.generarTicketCamarero(pedido.getCodigo());

                StreamResource res = new StreamResource(
                        "ticket-" + pedido.getCodigo() + ".pdf",
                        () -> new ByteArrayInputStream(pdf)
                );
                res.setContentType("application/pdf");

                download.setHref(res);
                download.getElement().callJsFunction("click");
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        boolean puede = pedidoService.puedeEditarOCancelarCamarero(pedido);

        Button editar = new Button("✏️ Editar pedido");
        editar.setEnabled(puede);
        editar.getStyle().set("font-weight", "800");
        editar.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("empleado/camarero/pedidos/editar/" + pedido.getCodigo()));
            dialog.close();
        });

        Button cancelar = new Button("⛔ Cancelar pedido");
        cancelar.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancelar.setEnabled(puede);
        cancelar.getStyle().set("font-weight", "800");
        cancelar.addClickListener(e -> {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Cancelar pedido");
            confirm.setText("¿Seguro que deseas cancelar este pedido?\nEsta acción no se puede deshacer.");

            confirm.setConfirmText("Sí, cancelar pedido");
            confirm.setCancelText("Volver");

            confirm.setConfirmButtonTheme("error primary");
            confirm.setCancelable(true);

            confirm.addConfirmListener(ev -> {
                try {
                    pedidoService.cancelarPedidoCamarero(pedido.getCodigo(), "Cancelado por camarero");
                    Notification.show("Pedido cancelado", 2500, Notification.Position.BOTTOM_START);
                    dialog.close();
                    cargarPagina(pageIndex);
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });

            confirm.open();
        });

        box.add(download, generarTicket, editar, cancelar);
        return box;
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

    private String safe(String s) {
        return (s == null) ? "-" : s;
    }

    private LocalDateTime toStartOfDay(LocalDate d) {
        return d == null ? null : d.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate d) {
        return d == null ? null : d.atTime(LocalTime.MAX);
    }
}