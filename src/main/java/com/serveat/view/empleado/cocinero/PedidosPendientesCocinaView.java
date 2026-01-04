package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.cocina.CocineroService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.value.ValueChangeMode;
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

@Route(value = "empleado/cocinero/pendientes", layout = MainLayout.class)
@PageTitle("Pedidos pendientes | Cocina")
@Secured("ROLE_COCINERO")
public class PedidosPendientesCocinaView extends VerticalLayout {

    private final transient CocineroService cocineroService;

    // Filtros
    private final DatePicker desde = new DatePicker("Desde");
    private final DatePicker hasta = new DatePicker("Hasta");
    private final IntegerField filtroMesa = new IntegerField("Mesa");

    private final Button btnBuscar = new Button("Buscar");
    private final Button btnLimpiar = new Button("Limpiar");

    // Grid + paginación
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);
    private final Button prev = new Button("◀ Anterior");
    private final Button next = new Button("Siguiente ▶");
    private final Span infoPagina = new Span("");

    private int pageIndex = 0;
    private final int pageSize = 10;
    private long totalItems = 0;

    public PedidosPendientesCocinaView(CocineroService cocineroService) {
        this.cocineroService = cocineroService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1280px");
        getStyle().set("margin", "0 auto");

        // 🔝 CABECERA (igual que histórico)
        H2 titulo = new H2("Pedidos pendientes de aceptación");
        titulo.getStyle().set("margin", "0");

        Button verHoy = new Button("📆 Ver pedidos de hoy");
        verHoy.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        verHoy.getStyle().set("font-weight", "700");
        verHoy.addClickListener(e ->
                UI.getCurrent().navigate("empleado/cocinero/hoy")
        );

        HorizontalLayout header = new HorizontalLayout(titulo, verHoy);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        add(header);

        add(crearBloqueFiltros());
        add(crearBloqueGrid());
        add(crearBloquePaginacion());

        configurarGrid();
        cargarPagina(0);
    }

    private Component crearBloqueFiltros() {
        VerticalLayout card = crearCard();

        filtroMesa.setMin(1);
        filtroMesa.setStepButtonsVisible(true);
        filtroMesa.setClearButtonVisible(true);
        filtroMesa.setValueChangeMode(ValueChangeMode.ON_CHANGE);

        btnBuscar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnBuscar.getStyle().set("font-weight", "700");
        btnBuscar.addClickListener(e -> {
            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        btnLimpiar.addClickListener(e -> {
            desde.clear();
            hasta.clear();
            filtroMesa.clear();
            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        HorizontalLayout fila1 = new HorizontalLayout(desde, hasta, filtroMesa);
        fila1.setWidthFull();
        fila1.setSpacing(false);
        fila1.getStyle().set("gap", "12px");

        HorizontalLayout fila2 = new HorizontalLayout(btnBuscar, btnLimpiar);
        fila2.setSpacing(false);
        fila2.getStyle().set("gap", "12px");

        card.add(fila1, fila2);
        return card;
    }

    private Component crearBloqueGrid() {
        VerticalLayout card = crearCard();
        grid.setWidthFull();
        grid.setAllRowsVisible(false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
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

    private void configurarGrid() {
        grid.removeAllColumns();

        grid.addColumn(Pedido::getCodigo)
                .setHeader("Código")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p ->
                        p.getReservaMesa() != null
                                ? "Mesa " + p.getReservaMesa().getNumeroMesa()
                                : "Recogida / Domicilio")
                .setHeader("Origen")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(p -> {
            Button aceptar = new Button("Aceptar");
            aceptar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            aceptar.addClickListener(e -> aceptar(p));

            Button descartar = new Button("Descartar");
            descartar.addThemeVariants(ButtonVariant.LUMO_ERROR);
            descartar.addClickListener(e -> descartar(p));

            return new HorizontalLayout(aceptar, descartar);
        }).setHeader("Acción").setAutoWidth(true);

        grid.addComponentColumn(p -> {
            VerticalLayout l = new VerticalLayout();
            l.setPadding(false);
            l.setSpacing(false);
            l.getStyle().set("gap", "4px");

            if (p.getLineaPedidos() != null) {
                for (LineaPedido lp : p.getLineaPedidos()) {
                    l.add(new Span(
                            lp.getProducto().getNombre() + " x" + lp.getCantidad()
                    ));
                }
            }
            return l;
        }).setHeader("Productos").setFlexGrow(1);
    }

    private void cargarPagina(int index) {
        try {
            Pageable pageable = PageRequest.of(index, pageSize);

            LocalDateTime d = toStartOfDay(desde.getValue());
            LocalDateTime h = toEndOfDay(hasta.getValue());
            Integer mesa = filtroMesa.getValue();

            Page<Pedido> page = cocineroService.buscarPendientesAceptacion(d, h, mesa, pageable);

            totalItems = page.getTotalElements();
            grid.setItems(page.getContent());

            int maxPage = (int) Math.max(0, (totalItems - 1) / pageSize);
            prev.setEnabled(index > 0);
            next.setEnabled(index < maxPage);

            long from = totalItems == 0 ? 0 : (index * pageSize + 1L);
            long to = Math.min(totalItems, (long) (index + 1) * pageSize);
            infoPagina.setText("Mostrando " + from + "-" + to + " de " + totalItems);

        } catch (Exception ex) {
            Notification.show("Error cargando pedidos: " + ex.getMessage(), 4000,
                    Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void aceptar(Pedido p) {
        try {
            String user = SecurityContextHolder.getContext().getAuthentication().getName();
            cocineroService.aceptarPedido(p.getCodigo(), user);
            cargarPagina(pageIndex);
            Notification.show("Pedido aceptado", 3000,
                    Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show(e.getMessage(), 3000,
                    Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void descartar(Pedido p) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Descartar pedido");
        dialog.setText("¿Seguro que deseas descartar el pedido " + p.getCodigo() + "?");
        dialog.setConfirmText("Descartar");
        dialog.setCancelText("Cancelar");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                String user = SecurityContextHolder.getContext().getAuthentication().getName();
                cocineroService.cancelarDesdeCocina(p.getCodigo(), "Descartado por cocina", user);
                cargarPagina(pageIndex);
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000,
                        Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
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
