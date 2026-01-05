package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.cocina.CocineroService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Base reutilizable para vistas de cocina con:
 * filtros, grid, paginación y helpers comunes.
 *
 * Subclases deben:
 * - definir si usan filtro de estado
 * - configurar columnas del grid
 * - implementar la consulta paginada al servicio
 * - definir la acción de limpiar filtros
 */
public abstract class PedidosCocinaAbstractaView extends VerticalLayout {

    protected final transient CocineroService cocineroService;
    protected final transient PedidoCalculoService pedidoCalculoService;

    // Filtros comunes
    protected final DatePicker desde = new DatePicker("Desde");
    protected final DatePicker hasta = new DatePicker("Hasta");
    protected final IntegerField filtroMesa = new IntegerField("Mesa");

    // Filtro opcional
    protected final ComboBox<EstadoCocina> filtroEstado = new ComboBox<>("Estado cocina");

    protected final Button btnBuscar = new Button("Buscar");
    protected final Button btnLimpiar = new Button("Limpiar");

    // Grid y paginación
    protected final Grid<Pedido> grid = new Grid<>(Pedido.class, false);
    protected final Button prev = new Button("Anterior");
    protected final Button next = new Button("Siguiente");
    protected final Span infoPagina = new Span("");

    protected int pageIndex = 0;
    protected int pageSize = 10;
    protected long totalItems = 0;

    protected PedidosCocinaAbstractaView(CocineroService cocineroService,
                                         PedidoCalculoService pedidoCalculoService) {
        this.cocineroService = cocineroService;
        this.pedidoCalculoService = pedidoCalculoService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1280px");
        getStyle().set("margin", "0 auto");
    }

    // Inicializa la vista completa en las subclases tras configurar pageSize, alturas, etc.
    protected final void initView(String titulo, Component accionHeaderDerecha, String gridHeightPx) {
        add(crearHeader(titulo, accionHeaderDerecha));
        add(crearBloqueFiltros());
        add(crearBloqueGrid(gridHeightPx));
        add(crearBloquePaginacion());

        configurarFiltros();
        configurarGridBase();
        configurarGridColumnas();

        cargarPagina(0);
    }

    protected abstract boolean usarFiltroEstado();

    // Acción de limpiar filtros definida por cada pantalla
    protected abstract void limpiarFiltros();

    // Configura columnas del grid
    protected abstract void configurarGridColumnas();

    // Consulta paginada específica por pantalla
    protected abstract Page<Pedido> buscar(Pageable pageable,
                                           LocalDateTime desde,
                                           LocalDateTime hasta,
                                           EstadoCocina estado,
                                           Integer mesa);

    protected void onRowAction(Pedido pedido) {
        // Hook opcional para acciones comunes
    }

    protected void notifyError(String msg) {
        Notification.show(msg, 4500, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    protected void notifySuccess(String msg) {
        Notification.show(msg, 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    protected LocalDateTime toStartOfDay(LocalDate d) {
        return d == null ? null : d.atStartOfDay();
    }

    protected LocalDateTime toEndOfDay(LocalDate d) {
        return d == null ? null : d.atTime(LocalTime.MAX);
    }

    protected VerticalLayout crearCard(Component... inside) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");
        card.getStyle().set("gap", "12px");
        card.add(inside);
        return card;
    }

    private Component crearHeader(String titulo, Component accionDerecha) {
        H3 h = new H3(titulo);
        h.getStyle().set("margin", "0");

        HorizontalLayout top = new HorizontalLayout(h);
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        top.setAlignItems(FlexComponent.Alignment.CENTER);

        if (accionDerecha != null) {
            top.add(accionDerecha);
        }

        return top;
    }

    private Component crearBloqueFiltros() {
        // Fila 1: fechas, estado opcional, mesa
        HorizontalLayout fila1 = new HorizontalLayout();
        fila1.setWidthFull();
        fila1.setSpacing(false);
        fila1.getStyle().set("gap", "12px");
        fila1.setAlignItems(Alignment.END);

        if (usarFiltroEstado()) {
            fila1.add(desde, hasta, filtroEstado, filtroMesa);
        } else {
            fila1.add(desde, hasta, filtroMesa);
        }

        // Fila 2: acciones
        btnBuscar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnBuscar.getStyle().set("font-weight", "700");
        btnBuscar.addClickListener(e -> {
            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        btnLimpiar.getStyle().set("font-weight", "700");
        btnLimpiar.addClickListener(e -> {
            limpiarFiltros();
            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        HorizontalLayout fila2 = new HorizontalLayout(btnBuscar, btnLimpiar);
        fila2.setSpacing(false);
        fila2.getStyle().set("gap", "12px");

        return crearCard(new H3("Filtros"), fila1, fila2);
    }

    private Component crearBloqueGrid(String heightPx) {
        grid.setWidthFull();
        grid.setAllRowsVisible(false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        if (heightPx != null && !heightPx.isBlank()) {
            grid.setHeight(heightPx);
        }

        return crearCard(grid);
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

    private void configurarFiltros() {
        if (usarFiltroEstado()) {
            filtroEstado.setItems(EstadoCocina.values());
            filtroEstado.setClearButtonVisible(true);
        } else {
            filtroEstado.setVisible(false);
        }

        filtroMesa.setMin(1);
        filtroMesa.setStepButtonsVisible(true);
        filtroMesa.setClearButtonVisible(true);
        filtroMesa.setValueChangeMode(ValueChangeMode.LAZY);
    }

    private void configurarGridBase() {
        grid.removeAllColumns();
    }

    protected final void cargarPagina(int index) {
        try {
            Pageable pageable = PageRequest.of(index, pageSize);

            LocalDateTime d = toStartOfDay(desde.getValue());
            LocalDateTime h = toEndOfDay(hasta.getValue());
            EstadoCocina estado = usarFiltroEstado() ? filtroEstado.getValue() : null;
            Integer mesa = filtroMesa.getValue();

            Page<Pedido> page = buscar(pageable, d, h, estado, mesa);

            totalItems = page.getTotalElements();
            grid.setItems(page.getContent());

            int maxPage = (int) Math.max(0, (totalItems - 1) / pageSize);
            prev.setEnabled(index > 0);
            next.setEnabled(index < maxPage);

            long from = totalItems == 0 ? 0 : (index * (long) pageSize + 1L);
            long to = Math.min(totalItems, (long) (index + 1) * pageSize);
            infoPagina.setText("Mostrando " + from + "-" + to + " de " + totalItems);

        } catch (Exception ex) {
            totalItems = 0;
            grid.setItems();
            prev.setEnabled(false);
            next.setEnabled(false);
            infoPagina.setText("");
            notifyError("Error cargando pedidos: " + ex.getMessage());
        }
    }

    protected final Button navButton(String texto, String route) {
        Button b = new Button(texto);
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        b.getStyle().set("font-weight", "700");
        b.addClickListener(e -> UI.getCurrent().navigate(route));
        return b;
    }
}