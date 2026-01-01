package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.seguimiento.PedidoSeguimientoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@PageTitle("Seguimiento | Cliente")
@Route(value = "cliente/pedidos/seguimiento/activos", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class SeguimientoPedidosActivosView extends VerticalLayout {

    private final transient PedidoSeguimientoService seguimientoService;

    private final DatePicker desde = new DatePicker("Desde");
    private final DatePicker hasta = new DatePicker("Hasta");

    private final ComboBox<EstadoPedido> filtroEstadoPedido = new ComboBox<>("Estado pedido");
    private final ComboBox<EstadoCocina> filtroEstadoCocina = new ComboBox<>("Estado cocina");
    private final ComboBox<EstadoReparto> filtroEstadoReparto = new ComboBox<>("Estado reparto");

    private final Button btnBuscar = new Button("Buscar");
    private final Button btnLimpiar = new Button("Limpiar");

    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    private final Button btnRefrescar = new Button("Refrescar");
    private final Button btnAnteriores = new Button("Pedidos anteriores");
    private final Button btnVolver = new Button("Volver a mis pedidos");

    private final Button prev = new Button("Anterior");
    private final Button next = new Button("Siguiente");
    private final Span infoPagina = new Span("");

    private int pageIndex = 0;
    private final int pageSize = 10;

    private long totalItems = 0;

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public SeguimientoPedidosActivosView(PedidoSeguimientoService seguimientoService) {
        this.seguimientoService = seguimientoService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Seguimiento de pedidos activos");
        titulo.getStyle().set("margin", "0");

        btnRefrescar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnRefrescar.addClickListener(e -> cargarPagina(0));

        btnAnteriores.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SeguimientoPedidosAnterioresView.class)));
        btnVolver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(ConsultaPedidosView.class)));

        HorizontalLayout accionesTop = new HorizontalLayout(btnRefrescar, btnAnteriores, btnVolver);
        accionesTop.setSpacing(false);
        accionesTop.getStyle().set("gap", "10px");

        HorizontalLayout header = new HorizontalLayout(titulo, accionesTop);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        add(header, crearBloqueFiltros(), crearBloqueListado(), crearBloquePaginacion());

        configurarFiltros();
        configurarGrid();

        cargarPagina(0);
    }

    private Component crearBloqueFiltros() {
        VerticalLayout card = crearCard();

        HorizontalLayout fila1 = new HorizontalLayout(desde, hasta);
        fila1.setWidthFull();
        fila1.setSpacing(false);
        fila1.getStyle().set("gap", "12px");

        HorizontalLayout fila2 = new HorizontalLayout(filtroEstadoPedido, filtroEstadoCocina, filtroEstadoReparto, btnBuscar, btnLimpiar);
        fila2.setWidthFull();
        fila2.setSpacing(false);
        fila2.getStyle().set("gap", "12px");
        fila2.setAlignItems(FlexComponent.Alignment.END);

        card.add(new H3("Filtros"), fila1, fila2);
        return card;
    }

    private void configurarFiltros() {
        filtroEstadoPedido.setItems(EstadoPedido.values());
        filtroEstadoPedido.setClearButtonVisible(true);

        filtroEstadoCocina.setItems(EstadoCocina.values());
        filtroEstadoCocina.setClearButtonVisible(true);

        filtroEstadoReparto.setItems(EstadoReparto.values());
        filtroEstadoReparto.setClearButtonVisible(true);

        btnBuscar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnBuscar.getStyle().set("font-weight", "800");
        btnBuscar.addClickListener(e -> cargarPagina(0));

        btnLimpiar.addClickListener(e -> {
            desde.clear();
            hasta.clear();
            filtroEstadoPedido.clear();
            filtroEstadoCocina.clear();
            filtroEstadoReparto.clear();
            cargarPagina(0);
        });
    }

    private Component crearBloqueListado() {
        VerticalLayout card = crearCard();

        grid.setWidthFull();
        grid.setHeight("460px");
        grid.getStyle().set("border-radius", "10px");
        grid.getStyle().set("overflow", "hidden");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        card.add(grid);
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
            if (pageIndex > 0) cargarPagina(pageIndex - 1);
        });

        next.addClickListener(e -> {
            int maxPage = (int) Math.max(0, (totalItems - 1) / pageSize);
            if (pageIndex < maxPage) cargarPagina(pageIndex + 1);
        });

        return barra;
    }

    private void configurarGrid() {
        grid.removeAllColumns();

        grid.addColumn(Pedido::getCodigo)
                .setHeader("Pedido")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(FECHA_FMT) : "-")
                .setHeader("Fecha")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> safe(p.getTipoPedido()))
                .setHeader("Tipo")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> safe(p.getEstado()))
                .setHeader("Pedido")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> safe(p.getEstadoCocina()))
                .setHeader("Cocina")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> safe(p.getEstadoReparto()))
                .setHeader("Reparto")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(this::crearBotonAcciones)
                .setHeader("Acciones")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    private Component crearBotonAcciones(Pedido p) {
        Button acciones = new Button("Acciones");
        acciones.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        acciones.getStyle().set("font-weight", "700");

        ContextMenu menu = new ContextMenu(acciones);
        menu.setOpenOnClick(true);

        menu.addItem("Ver seguimiento", e ->
                getUI().ifPresent(ui -> ui.navigate(SeguimientoPedidoIndividualView.class, p.getCodigo()))
        );

        return acciones;
    }

    private void cargarPagina(int newPageIndex) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            LocalDateTime d = toStartOfDay(desde.getValue());
            LocalDateTime h = toEndOfDay(hasta.getValue());

            Pageable pageable = PageRequest.of(Math.max(newPageIndex, 0), pageSize);

            Page<Pedido> page = seguimientoService.buscarActivosCliente(
                    username,
                    d, h,
                    filtroEstadoPedido.getValue(),
                    filtroEstadoCocina.getValue(),
                    filtroEstadoReparto.getValue(),
                    pageable
            );

            pageIndex = page.getNumber();
            totalItems = page.getTotalElements();

            grid.setItems(page.getContent());

            int maxPage = (int) Math.max(0, (totalItems - 1) / pageSize);
            prev.setEnabled(pageIndex > 0);
            next.setEnabled(pageIndex < maxPage);

            long shownFrom = totalItems == 0 ? 0 : (pageIndex * pageSize + 1L);
            long shownTo = Math.min(totalItems, (long) (pageIndex + 1) * pageSize);
            infoPagina.setText("Mostrando " + shownFrom + "-" + shownTo + " de " + totalItems);

        } catch (Exception ex) {
            grid.setItems();
            totalItems = 0;
            pageIndex = 0;
            prev.setEnabled(false);
            next.setEnabled(false);
            infoPagina.setText("Mostrando 0-0 de 0");
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
        }
    }

    private String safe(Object o) {
        return o != null ? o.toString() : "-";
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