package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.pedido.TicketService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Mis pedidos | Cliente")
@Route(value = "cliente/pedidos", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class ConsultaPedidosView extends VerticalLayout {

    private final transient PedidoService pedidoService;
    private final transient PedidoCalculoService pedidoCalculoService;
    private final transient TicketService ticketService;

    // Filtros
    private final DatePicker desde = new DatePicker("Desde");
    private final DatePicker hasta = new DatePicker("Hasta");
    private final ComboBox<EstadoPedido> filtroEstadoPedido = new ComboBox<>("Estado pedido");
    private final ComboBox<EstadoCocina> filtroEstadoCocina = new ComboBox<>("Estado cocina");
    private final IntegerField filtroMesa = new IntegerField("Mesa");

    private final Button btnBuscar = new Button("Buscar");
    private final Button btnLimpiar = new Button("Limpiar");

    // Grid + paginación
    private final Grid<Pedido> gridPedidos = new Grid<>(Pedido.class, false);
    private final Button prev = new Button("◀ Anterior");
    private final Button next = new Button("Siguiente ▶");
    private final Span infoPagina = new Span("");

    private int pageIndex = 0;
    private final int pageSize = 10;

    private List<Pedido> pedidosFiltrados = new ArrayList<>();
    private long totalItems = 0;

    // Detalle
    private final Grid<LineaPedido> gridLineas = new Grid<>(LineaPedido.class, false);
    private final Span infoSeleccion = new Span("Selecciona un pedido para ver el detalle.");
    private transient Pedido pedidoSeleccionado;

    // Descarga ticket (Anchor oculto)
    private final Anchor download = new Anchor();

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ConsultaPedidosView(PedidoService pedidoService,
                               PedidoCalculoService pedidoCalculoService,
                               TicketService ticketService) {
        this.pedidoService = pedidoService;
        this.pedidoCalculoService = pedidoCalculoService;
        this.ticketService = ticketService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Mis pedidos");
        titulo.getStyle().set("margin", "0");

        download.getStyle().set("display", "none");
        download.getElement().setAttribute("download", true);

        add(titulo, download);

        add(crearBloqueFiltros());
        add(crearBloqueListado());
        add(crearBloquePaginacion());
        add(crearBloqueDetalle());

        configurarGridPedidos();
        configurarGridLineas();

        cargarYFiltrar();
        cargarPagina(0);
        refrescarLineas();
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
        btnBuscar.getStyle().set("font-weight", "800");
        btnBuscar.addClickListener(e -> {
            pageIndex = 0;
            cargarYFiltrar();
            cargarPagina(pageIndex);
        });

        btnLimpiar.addClickListener(e -> {
            desde.clear();
            hasta.clear();
            filtroEstadoPedido.clear();
            filtroEstadoCocina.clear();
            filtroMesa.clear();
            pageIndex = 0;
            cargarYFiltrar();
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
        fila2.setAlignItems(FlexComponent.Alignment.END);

        card.add(new H3("Filtros"), fila1, fila2);
        return card;
    }

    private Component crearBloqueListado() {
        VerticalLayout card = crearCard();

        gridPedidos.setWidthFull();
        gridPedidos.setHeight("360px");
        gridPedidos.getStyle().set("border-radius", "10px");
        gridPedidos.getStyle().set("overflow", "hidden");
        gridPedidos.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        card.add(gridPedidos);
        return card;
    }

    private Component crearBloquePaginacion() {
        HorizontalLayout barra = new HorizontalLayout(prev, infoPagina, next);
        barra.setWidthFull();
        barra.setAlignItems(FlexComponent.Alignment.CENTER);
        barra.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
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

    private Component crearBloqueDetalle() {
        VerticalLayout card = crearCard();
        infoSeleccion.getStyle().set("color", "var(--lumo-secondary-text-color)");

        gridLineas.setWidthFull();
        gridLineas.setHeight("260px");
        gridLineas.getStyle().set("border-radius", "10px");
        gridLineas.getStyle().set("overflow", "hidden");
        gridLineas.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        card.add(infoSeleccion, gridLineas);
        return card;
    }

    private void configurarGridPedidos() {
        gridPedidos.removeAllColumns();

        gridPedidos.addColumn(Pedido::getCodigo)
                .setHeader("Nº Pedido")
                .setAutoWidth(true)
                .setFlexGrow(0);

        gridPedidos.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(FECHA_FMT) : "-")
                .setHeader("Fecha")
                .setAutoWidth(true)
                .setFlexGrow(0);

        gridPedidos.addColumn(p -> p.getEstado() != null ? p.getEstado().name() : "-")
                .setHeader("Estado pedido")
                .setAutoWidth(true)
                .setFlexGrow(0);

        gridPedidos.addColumn(p -> p.getEstadoCocina() != null ? p.getEstadoCocina().name() : "-")
                .setHeader("Estado cocina")
                .setAutoWidth(true)
                .setFlexGrow(0);

        gridPedidos.addColumn(p -> {
                    try {
                        return pedidoCalculoService.calcularTotalPedido(p) + " €";
                    } catch (Exception e) {
                        return "-";
                    }
                })
                .setHeader("Total")
                .setAutoWidth(true)
                .setFlexGrow(0);

        //  UNA sola columna de acciones (menú)
        gridPedidos.addComponentColumn(this::crearBotonAcciones)
                .setHeader("Acciones")
                .setAutoWidth(true)
                .setFlexGrow(0);

        gridPedidos.addSelectionListener(e -> {
            pedidoSeleccionado = e.getFirstSelectedItem().orElse(null);

            if (pedidoSeleccionado == null) {
                infoSeleccion.setText("Selecciona un pedido para ver el detalle.");
                refrescarLineas();
                return;
            }

            try {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                pedidoSeleccionado = pedidoService.cargarDetalleCliente(pedidoSeleccionado.getCodigo(), username);

                infoSeleccion.setText("Detalle del pedido " + pedidoSeleccionado.getCodigo());
                refrescarLineas();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });
    }

    private Component crearBotonAcciones(Pedido p) {
        Button acciones = new Button("⋯ Acciones");
        acciones.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        acciones.getStyle().set("font-weight", "800");

        ContextMenu menu = new ContextMenu(acciones);
        menu.setOpenOnClick(true);

        // Seguimiento (pantalla individual)
        menu.addItem("📍 Seguimiento", e ->
                getUI().ifPresent(ui -> ui.navigate(SeguimientoPedidoIndividualView.class, p.getCodigo()))
        );

        // Ticket
        menu.addItem("🧾 Ticket", e -> descargarTicket(p));

        // Modificar (solo si se puede)
        if (pedidoService.puedeModificarCliente(p)) {
            menu.addItem("✏️ Modificar", e ->
                    getUI().ifPresent(ui -> ui.navigate(ModificarPedidoClienteView.class, p.getCodigo()))
            );

            // Cancelar (solo si se puede)
            menu.addItem("⛔ Cancelar", e -> confirmarCancelar(p));
        }

        return acciones;
    }

    private void confirmarCancelar(Pedido p) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Cancelar pedido");
        dialog.setText("¿Seguro que quieres cancelar el pedido " + p.getCodigo() + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Sí, cancelar");
        dialog.setCancelText("No");

        dialog.addConfirmListener(ev -> cancelarPedido(p));
        dialog.open();
    }

    private void cancelarPedido(Pedido p) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            pedidoService.cancelarPedidoCliente(p.getCodigo(), "Cancelado por cliente", username);

            Notification.show("Pedido cancelado", 2500, Notification.Position.BOTTOM_START);

            pageIndex = 0;
            cargarYFiltrar();
            cargarPagina(pageIndex);

            gridPedidos.deselectAll();
            pedidoSeleccionado = null;
            infoSeleccion.setText("Selecciona un pedido para ver el detalle.");
            refrescarLineas();

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void configurarGridLineas() {
        gridLineas.removeAllColumns();

        gridLineas.addColumn(lp -> lp.getProducto() != null ? safe(lp.getProducto().getNombre()) : "-")
                .setHeader("Producto")
                .setAutoWidth(true)
                .setFlexGrow(1);

        gridLineas.addColumn(LineaPedido::getCantidad)
                .setHeader("Cantidad")
                .setAutoWidth(true)
                .setFlexGrow(0);

        gridLineas.addColumn(lp -> lp.getPrecioUnitario() != null ? lp.getPrecioUnitario() + " €" : "-")
                .setHeader("Precio ud.")
                .setAutoWidth(true)
                .setFlexGrow(0);

        gridLineas.addComponentColumn(lp -> {
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
        }).setHeader("Ingredientes").setFlexGrow(1);

        gridLineas.addColumn(lp -> pedidoCalculoService.calcularPrecioLinea(lp) + " €")
                .setHeader("Subtotal")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    private void cargarYFiltrar() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            List<Pedido> todos = pedidoService.listarPedidosCliente(username);

            LocalDateTime d = toStartOfDay(desde.getValue());
            LocalDateTime h = toEndOfDay(hasta.getValue());
            EstadoPedido ep = filtroEstadoPedido.getValue();
            EstadoCocina ec = filtroEstadoCocina.getValue();
            Integer mesa = filtroMesa.getValue();

            List<Pedido> filtrados = todos.stream()
                    .filter(p -> d == null || (p.getFechaCreacion() != null && !p.getFechaCreacion().isBefore(d)))
                    .filter(p -> h == null || (p.getFechaCreacion() != null && !p.getFechaCreacion().isAfter(h)))
                    .filter(p -> ep == null || (p.getEstado() != null && p.getEstado().equals(ep)))
                    .filter(p -> ec == null || (p.getEstadoCocina() != null && p.getEstadoCocina().equals(ec)))
                    .filter(p -> mesa == null || (p.getReservaMesa() != null && Objects.equals(p.getReservaMesa().getNumeroMesa(), mesa)))
                    .collect(Collectors.toList());

            // Orden: primero cancelables/modificables, luego el resto; dentro por fecha desc
            Comparator<Pedido> byEditableFirst = Comparator.comparing(
                    (Pedido p) -> pedidoService.puedeModificarCliente(p)
            ).reversed();

            Comparator<Pedido> byFechaDesc = (a, b) -> {
                if (a.getFechaCreacion() == null && b.getFechaCreacion() == null) return 0;
                if (a.getFechaCreacion() == null) return 1;
                if (b.getFechaCreacion() == null) return -1;
                return b.getFechaCreacion().compareTo(a.getFechaCreacion());
            };

            pedidosFiltrados = filtrados.stream()
                    .sorted(byEditableFirst.thenComparing(byFechaDesc))
                    .toList();

            totalItems = pedidosFiltrados.size();

        } catch (Exception ex) {
            pedidosFiltrados = new ArrayList<>();
            totalItems = 0;
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void cargarPagina(int index) {
        gridPedidos.deselectAll();
        pedidoSeleccionado = null;
        infoSeleccion.setText("Selecciona un pedido para ver el detalle.");
        refrescarLineas();

        int from = Math.max(0, index * pageSize);
        int to = Math.min(pedidosFiltrados.size(), from + pageSize);

        List<Pedido> page = (from >= to) ? List.of() : pedidosFiltrados.subList(from, to);
        gridPedidos.setItems(page);

        int maxPage = (int) Math.max(0, (totalItems - 1) / pageSize);
        prev.setEnabled(index > 0);
        next.setEnabled(index < maxPage);

        long shownFrom = totalItems == 0 ? 0 : (index * pageSize + 1L);
        long shownTo = Math.min(totalItems, (long) (index + 1) * pageSize);
        infoPagina.setText("Mostrando " + shownFrom + "-" + shownTo + " de " + totalItems);
    }

    private void refrescarLineas() {
        if (pedidoSeleccionado == null || pedidoSeleccionado.getLineaPedidos() == null) {
            gridLineas.setItems(List.of());
            return;
        }
        gridLineas.setItems(pedidoSeleccionado.getLineaPedidos());
    }

    private void descargarTicket(Pedido p) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            byte[] pdf = ticketService.generarTicketCliente(p.getCodigo(), username);

            StreamResource res = new StreamResource(
                    "ticket-" + p.getCodigo() + ".pdf",
                    () -> new ByteArrayInputStream(pdf)
            );
            res.setContentType("application/pdf");

            download.setHref(res);
            download.getElement().callJsFunction("click");

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
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
        return s == null ? "-" : s;
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

    private LocalDateTime toStartOfDay(LocalDate d) {
        return d == null ? null : d.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate d) {
        return d == null ? null : d.atTime(LocalTime.MAX);
    }
}