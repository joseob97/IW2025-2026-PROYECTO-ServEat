package com.serveat.view.empleado.repartidor;

import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.repartidor.RepartidorService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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

@PageTitle("Pedidos disponibles | Repartidor")
@Route(value = "empleado/repartidor/pedidos-disponibles", layout = MainLayout.class)
@Secured("ROLE_REPARTIDOR")
public class PedidosDisponiblesView extends VerticalLayout {

    private final transient RepartidorService repartidorService;

    // filtros
    private final DatePicker desde = new DatePicker("Desde");
    private final DatePicker hasta = new DatePicker("Hasta");
    private final Button btnBuscar = new Button("Buscar");
    private final Button btnLimpiar = new Button("Limpiar");
    private final Button refrescar = new Button("🔄 Refrescar");

    // grid + paginación
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);
    private final Button prev = new Button("◀ Anterior");
    private final Button next = new Button("Siguiente ▶");
    private final Span infoPagina = new Span("");

    private int pageIndex = 0;
    private final int pageSize = 10;
    private long totalItems = 0;

    private final Span info = new Span("Pedidos a domicilio pendientes de asignación.");

    public PedidosDisponiblesView(RepartidorService repartidorService) {
        this.repartidorService = repartidorService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Pedidos disponibles");
        titulo.getStyle().set("margin", "0");

        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(titulo, crearBloqueFiltros(), crearBloqueGrid(), crearBloquePaginacion());

        configurarGrid();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        cargarPagina(0);
    }

    private Component crearBloqueFiltros() {
        VerticalLayout card = crearCard();

        btnBuscar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnBuscar.getStyle().set("font-weight", "700");
        btnBuscar.addClickListener(e -> {
            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        btnLimpiar.addClickListener(e -> {
            desde.clear();
            hasta.clear();
            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        refrescar.addClickListener(e -> cargarPagina(pageIndex));

        HorizontalLayout fila = new HorizontalLayout(desde, hasta, btnBuscar, btnLimpiar);
        fila.setWidthFull();
        fila.setSpacing(false);
        fila.getStyle().set("gap", "12px");
        fila.setAlignItems(Alignment.END);

        HorizontalLayout barra = new HorizontalLayout(refrescar);
        barra.setWidthFull();
        barra.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        card.add(info, fila, barra);
        return card;
    }

    private Component crearBloqueGrid() {
        VerticalLayout card = crearCard();
        grid.setWidthFull();
        grid.setHeight("520px");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.getStyle().set("border-radius", "10px");
        grid.getStyle().set("overflow", "hidden");
        card.add(grid);
        return card;
    }

    private Component crearBloquePaginacion() {
        HorizontalLayout barra = new HorizontalLayout(prev, infoPagina, next);
        barra.setWidthFull();
        barra.setAlignItems(Alignment.CENTER);
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

    private void configurarGrid() {
        grid.removeAllColumns();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        grid.addColumn(Pedido::getCodigo)
                .setHeader("Pedido")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getFechaCreacion() != null ? p.getFechaCreacion().format(fmt) : "-")
                .setHeader("Recibido")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getCliente() != null ? p.getCliente().getNombre() : "-")
                .setHeader("Cliente")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(p -> p.getDireccionEntrega() != null ? p.getDireccionEntrega() : "-")
                .setHeader("Dirección")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(p -> p.getEstadoReparto() != null ? p.getEstadoReparto().name() : "-")
                .setHeader("Estado reparto")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(p -> {
            Button asignarme = new Button("📌 Asignarme");
            asignarme.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            asignarme.setEnabled(p.getEstadoReparto() == EstadoReparto.PENDIENTE_ASIGNACION);
            asignarme.addClickListener(e -> confirmarAsignacion(p));
            return asignarme;
        }).setHeader("Acción").setAutoWidth(true).setFlexGrow(0);
    }

    private void cargarPagina(int index) {
        try {
            Pageable pageable = PageRequest.of(index, pageSize);

            LocalDateTime d = toStartOfDay(desde.getValue());
            LocalDateTime h = toEndOfDay(hasta.getValue());

            Page<Pedido> page = repartidorService.buscarPedidosDisponibles(d, h, pageable);

            totalItems = page.getTotalElements();
            grid.setItems(page.getContent());

            int maxPage = (int) Math.max(0, (totalItems - 1) / pageSize);
            prev.setEnabled(index > 0);
            next.setEnabled(index < maxPage);

            long from = totalItems == 0 ? 0 : (index * pageSize + 1L);
            long to = Math.min(totalItems, (long) (index + 1) * pageSize);
            infoPagina.setText("Mostrando " + from + "-" + to + " de " + totalItems);

            if (totalItems == 0) {
                Notification.show("No hay pedidos pendientes de asignación", 2500, Notification.Position.BOTTOM_START);
            }

        } catch (Exception ex) {
            totalItems = 0;
            grid.setItems();
            prev.setEnabled(false);
            next.setEnabled(false);
            infoPagina.setText("");
            Notification.show("Error al cargar pedidos: " + ex.getMessage(), 4500, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmarAsignacion(Pedido p) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar asignación");
        dialog.setText("¿Confirma la asignación del pedido " + p.getCodigo() + "?");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        dialog.setConfirmText("Asignar");
        dialog.setConfirmButtonTheme("primary");
        dialog.addConfirmListener(event -> procesarAsignacion(p));
        dialog.open();
    }

    private void procesarAsignacion(Pedido p) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            repartidorService.asignarmePedido(p.getCodigo(), username);
            Notification.show("Pedido asignado correctamente ✅", 2500, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // recargar página actual (si se queda vacía y no es 0, retroceder)
            if (pageIndex > 0 && (totalItems - 1) <= (long) pageIndex * pageSize) {
                pageIndex--;
            }
            cargarPagina(pageIndex);

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE)
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