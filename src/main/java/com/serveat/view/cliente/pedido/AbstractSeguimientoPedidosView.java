package com.serveat.view.cliente.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public abstract class AbstractSeguimientoPedidosView extends VerticalLayout {

    protected final DatePicker desde = new DatePicker("Desde");
    protected final DatePicker hasta = new DatePicker("Hasta");
    protected final ComboBox<EstadoPedido> filtroEstadoPedido = new ComboBox<>("Estado pedido");
    protected final ComboBox<EstadoCocina> filtroEstadoCocina = new ComboBox<>("Estado cocina");
    protected final ComboBox<EstadoReparto> filtroEstadoReparto = new ComboBox<>("Estado reparto");

    protected final Button btnBuscar = new Button("Buscar");
    protected final Button btnLimpiar = new Button("Limpiar");

    protected final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    protected final Button prev = new Button("Anterior");
    protected final Button next = new Button("Siguiente");
    protected final Span infoPagina = new Span("");

    protected int pageIndex = 0;
    protected final int pageSize = 10;
    protected long totalItems = 0;

    protected static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    protected AbstractSeguimientoPedidosView(String titulo, Component accionesTop) {
        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 h3 = new H3(titulo);
        h3.getStyle().set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(h3, accionesTop);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        add(header, crearBloqueFiltros(), crearBloqueListado(), crearBloquePaginacion());

        configurarFiltros();
        configurarGrid();
        cargarPagina(0);
    }

    protected abstract Page<Pedido> buscar(Pageable pageable,
                                           String username,
                                           LocalDateTime desde,
                                           LocalDateTime hasta,
                                           EstadoPedido estadoPedido,
                                           EstadoCocina estadoCocina,
                                           EstadoReparto estadoReparto);

    protected abstract boolean mostrarColumnaAcciones();

    protected Component crearAcciones(Pedido p) {
        return new Span("-");
    }

    private Component crearBloqueFiltros() {
        VerticalLayout card = crearCard();

        HorizontalLayout fila1 = new HorizontalLayout(desde, hasta);
        fila1.setWidthFull();
        fila1.setSpacing(false);
        fila1.getStyle().set("gap", "12px");

        HorizontalLayout fila2 = new HorizontalLayout(
                filtroEstadoPedido, filtroEstadoCocina, filtroEstadoReparto, btnBuscar, btnLimpiar
        );
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

        if (mostrarColumnaAcciones()) {
            grid.addComponentColumn(this::crearAcciones)
                    .setHeader("Acciones")
                    .setAutoWidth(true)
                    .setFlexGrow(0);
        }
    }

    protected void cargarPagina(int newPageIndex) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            LocalDateTime d = toStartOfDay(desde.getValue());
            LocalDateTime h = toEndOfDay(hasta.getValue());

            Pageable pageable = PageRequest.of(Math.max(newPageIndex, 0), pageSize);

            Page<Pedido> page = buscar(
                    pageable,
                    username,
                    d, h,
                    filtroEstadoPedido.getValue(),
                    filtroEstadoCocina.getValue(),
                    filtroEstadoReparto.getValue()
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

    protected String safe(Object o) {
        return o != null ? o.toString() : "-";
    }

    protected VerticalLayout crearCard() {
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

    protected LocalDateTime toStartOfDay(LocalDate d) {
        return d == null ? null : d.atStartOfDay();
    }

    protected LocalDateTime toEndOfDay(LocalDate d) {
        return d == null ? null : d.atTime(LocalTime.MAX);
    }
}