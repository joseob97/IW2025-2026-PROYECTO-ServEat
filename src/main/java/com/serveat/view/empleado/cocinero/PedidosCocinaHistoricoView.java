package com.serveat.view.empleado.cocinero;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.cocina.CocineroService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.view.layout.MainLayout;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@PageTitle("Cocina | Histórico de pedidos")
@Route(value = "empleado/cocinero/historico", layout = MainLayout.class)
@Secured("ROLE_COCINERO")
public class PedidosCocinaHistoricoView extends VerticalLayout {

    private final transient CocineroService cocineroService;
    private final transient PedidoCalculoService pedidoCalculoService;

    // Filtros
    private final DatePicker desde = new DatePicker("Desde");
    private final DatePicker hasta = new DatePicker("Hasta");
    private final ComboBox<EstadoCocina> filtroEstado = new ComboBox<>("Estado cocina");
    private final IntegerField filtroMesa = new IntegerField("Mesa");

    private final Button btnBuscar = new Button("Buscar");
    private final Button btnLimpiar = new Button("Limpiar");

    // Grid + paginación
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);
    private final Button prev = new Button("◀ Anterior");
    private final Button next = new Button("Siguiente ▶");
    private final Span infoPagina = new Span("");

    private int pageIndex = 0;
    private final int pageSize = 15;
    private long totalItems = 0;

    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PedidosCocinaHistoricoView(CocineroService cocineroService,
                                      PedidoCalculoService pedidoCalculoService) {
        this.cocineroService = cocineroService;
        this.pedidoCalculoService = pedidoCalculoService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1280px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Histórico de pedidos (Cocina)");
        titulo.getStyle().set("margin", "0");

        Button irHoy = new Button("📆 Ver pedidos de hoy");
        irHoy.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        irHoy.getStyle().set("font-weight", "700");
        irHoy.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("empleado/cocinero/hoy")));

        HorizontalLayout top = new HorizontalLayout(titulo, irHoy);
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        top.setAlignItems(FlexComponent.Alignment.CENTER);

        add(top);

        add(crearBloqueFiltros());
        add(crearBloqueGrid());
        add(crearBloquePaginacion());

        configurarGrid();
        configurarFiltros();

        // Carga inicial
        cargarPagina(0);
    }

    private Component crearBloqueFiltros() {
        VerticalLayout card = crearCard();

        HorizontalLayout fila1 = new HorizontalLayout(desde, hasta, filtroEstado, filtroMesa);
        fila1.setWidthFull();
        fila1.setSpacing(false);
        fila1.getStyle().set("gap", "12px");
        fila1.setAlignItems(Alignment.END);

        btnBuscar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnBuscar.getStyle().set("font-weight", "700");
        btnBuscar.addClickListener(e -> {
            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        btnLimpiar.addClickListener(e -> {
            desde.clear();
            hasta.clear();
            filtroEstado.clear();
            filtroMesa.clear();
            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        HorizontalLayout fila2 = new HorizontalLayout(btnBuscar, btnLimpiar);
        fila2.setSpacing(false);
        fila2.getStyle().set("gap", "12px");

        card.add(new H3("Filtros"), fila1, fila2);
        return card;
    }

    private Component crearBloqueGrid() {
        VerticalLayout card = crearCard();

        grid.setWidthFull();
        grid.setAllRowsVisible(false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("560px");

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

    private void configurarFiltros() {
        filtroEstado.setItems(EstadoCocina.values());
        filtroEstado.setClearButtonVisible(true);

        filtroMesa.setMin(1);
        filtroMesa.setStepButtonsVisible(true);
        filtroMesa.setClearButtonVisible(true);
        filtroMesa.setValueChangeMode(ValueChangeMode.LAZY);
    }

    private void configurarGrid() {
        grid.removeAllColumns();

        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(FECHA_HORA_FMT) : "-")
                .setHeader("Fecha/Hora")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(Pedido::getCodigo)
                .setHeader("Código")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> (p.getReservaMesa() != null && p.getReservaMesa().getNumeroMesa() != null)
                        ? "Mesa " + p.getReservaMesa().getNumeroMesa()
                        : "Cliente")
                .setHeader("Origen")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getEstadoCocina() != null ? p.getEstadoCocina().name() : "-")
                .setHeader("Estado cocina")
                .setAutoWidth(true)
                .setFlexGrow(0);

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
            Button ver = new Button("🔎 Ver/Actualizar");
            ver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            ver.getStyle().set("font-weight", "700");
            ver.addClickListener(e -> {
                if (p.getId() == null) {
                    Notification.show("Pedido sin ID", 2500, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                UI.getCurrent().navigate(DetalleComandaView.class, p.getId().toString());
            });
            return ver;
        }).setHeader("Acciones").setAutoWidth(true).setFlexGrow(0);
    }

    private void cargarPagina(int index) {
        try {
            Pageable pageable = PageRequest.of(index, pageSize);

            LocalDateTime d = toStartOfDay(desde.getValue());
            LocalDateTime h = toEndOfDay(hasta.getValue());

            EstadoCocina estado = filtroEstado.getValue();
            Integer mesa = filtroMesa.getValue();

            Page<Pedido> page = cocineroService.buscarPedidosCocinaHistorico(d, h, estado, mesa, pageable);

            totalItems = page.getTotalElements();
            grid.setItems(page.getContent());

            int maxPage = (int) Math.max(0, (totalItems - 1) / pageSize);
            prev.setEnabled(index > 0);
            next.setEnabled(index < maxPage);

            long from = totalItems == 0 ? 0 : (index * pageSize + 1L);
            long to = Math.min(totalItems, (long) (index + 1) * pageSize);
            infoPagina.setText("Mostrando " + from + "-" + to + " de " + totalItems);

        } catch (Exception ex) {
            totalItems = 0;
            grid.setItems();
            prev.setEnabled(false);
            next.setEnabled(false);
            infoPagina.setText("");
            Notification.show("Error cargando histórico: " + ex.getMessage(), 4500, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
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