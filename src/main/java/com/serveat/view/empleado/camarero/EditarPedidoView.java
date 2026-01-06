package com.serveat.view.empleado.camarero;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.ProductoIngrediente;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.*;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

@PageTitle("Editar Pedido | Camarero")
@Route(value = "empleado/camarero/pedidos/editar", layout = MainLayout.class)
@Secured("ROLE_CAMARERO")
public class EditarPedidoView extends VerticalLayout implements HasUrlParameter<String> {

    private final transient PedidoService pedidoService;
    private final transient PedidoCalculoService calculoService;
    private final transient FeatureService featureService;

    private transient Pedido pedidoEditable;
    private boolean hayCambios = false;

    private final Span info = new Span("");
    private final Grid<LineaPedido> gridLineas = new Grid<>(LineaPedido.class, false);
    private final Button confirmarCambios = new Button("✅ Confirmar cambios");
    private final Button volver = new Button("← Volver a pedidos");

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public EditarPedidoView(PedidoService pedidoService,
                            PedidoCalculoService calculoService,
                            FeatureService featureService) {
        this.pedidoService = pedidoService;
        this.calculoService = calculoService;
        this.featureService = featureService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Editar pedido (Camarero)");
        titulo.getStyle().set("margin", "0");

        volver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        volver.getStyle().set("font-weight", "700");
        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(PedidosCamareroView.class)));

        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        configurarGridLineas();

        confirmarCambios.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmarCambios.getStyle().set("font-weight", "800");
        confirmarCambios.setEnabled(false);
        confirmarCambios.addClickListener(e -> guardarCambios());

        HorizontalLayout barraTop = new HorizontalLayout(volver, confirmarCambios);
        barraTop.setWidthFull();
        barraTop.setJustifyContentMode(JustifyContentMode.BETWEEN);
        barraTop.setAlignItems(Alignment.CENTER);

        add(titulo, info, barraTop, crearCard(gridLineas));
        bloquearEdicion();
    }

    private boolean personalizacionHabilitada() {
        return featureService != null && featureService.tieneFeature(Feature.INGREDIENTES);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String codigo) {
        if (codigo == null || codigo.isBlank()) {
            limpiarVista("No se ha indicado código de pedido.");
            return;
        }

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Pedido p = pedidoService.cargarPedidoEditableCamarero(codigo, username);

            this.pedidoEditable = p;
            this.hayCambios = false;
            confirmarCambios.setEnabled(false);

            String fecha = (p.getFechaCreacion() != null) ? p.getFechaCreacion().format(FECHA_FMT) : "-";
            String mesa = (p.getReservaMesa() != null) ? String.valueOf(p.getReservaMesa().getNumeroMesa()) : "-";
            info.setText("Pedido: " + safe(p.getCodigo()) + " | Fecha: " + fecha + " | Mesa: " + mesa);

            refrescarLineas();
            desbloquearEdicion();

        } catch (Exception ex) {
            limpiarVista("No se pudo cargar el pedido: " + ex.getMessage());
        }
    }

    private void configurarGridLineas() {
        gridLineas.setWidthFull();
        gridLineas.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        gridLineas.addColumn(lp -> lp.getProducto() != null ? safe(lp.getProducto().getNombre()) : "-")
                .setHeader("Producto")
                .setFlexGrow(1);

        gridLineas.addComponentColumn(lp -> {
            IntegerField qty = new IntegerField();
            qty.setMin(1);
            qty.setStepButtonsVisible(true);
            qty.setWidth("140px");
            qty.setValue(Math.max(1, lp.getCantidad()));

            qty.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;
                if (pedidoEditable == null) return;

                Integer nueva = ev.getValue();
                if (nueva == null || nueva <= 0) {
                    qty.setValue(Math.max(1, lp.getCantidad()));
                    Notification.show("Cantidad inválida", 2500, Notification.Position.MIDDLE);
                    return;
                }

                try {
                    pedidoService.aplicarCantidadLinea(pedidoEditable, lp.getCodigo(), nueva);
                    marcarCambio();
                    gridLineas.getDataProvider().refreshItem(lp);
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                    refrescarLineas();
                }
            });

            return qty;
        }).setHeader("Cantidad").setAutoWidth(true).setFlexGrow(0);

        gridLineas.addComponentColumn(lp -> {
            if (!personalizacionHabilitada()) {
                return new Span("");
            }

            boolean tieneIngredientes;
            try {
                tieneIngredientes = !pedidoService.obtenerIngredientesDisponiblesLinea(lp).isEmpty();
            } catch (Exception ex) {
                tieneIngredientes = false;
            }

            if (!tieneIngredientes) return new Span("");

            Button ing = new Button("Ingredientes");
            ing.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            ing.getStyle().set("font-weight", "700");
            ing.addClickListener(e -> abrirEditorIngredientes(lp));
            return ing;

        }).setHeader("Editar").setAutoWidth(true).setFlexGrow(0);

        gridLineas.addColumn(lp -> {
            try {
                return calculoService.calcularPrecioLinea(lp) + " €";
            } catch (Exception ex) {
                return "-";
            }
        }).setHeader("Subtotal").setAutoWidth(true).setFlexGrow(0);

        gridLineas.addComponentColumn(lp -> {
            Button borrar = new Button("❌");
            borrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            borrar.addClickListener(e -> {
                if (pedidoEditable == null) return;

                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Cancelar producto");
                dialog.setText("¿Seguro que deseas eliminar este producto del pedido?\nEsta acción no se puede deshacer.");

                dialog.setConfirmText("Sí, eliminar");
                dialog.setCancelText("Cancelar");

                dialog.setConfirmButtonTheme("error primary");
                dialog.setCancelable(true);

                dialog.addConfirmListener(ev -> {
                    try {
                        pedidoService.eliminarLinea(pedidoEditable, lp.getCodigo());
                        marcarCambio();
                        refrescarLineas();
                    } catch (Exception ex) {
                        Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                    }
                });

                dialog.open();
            });

            return borrar;
        }).setHeader("Eliminar").setAutoWidth(true).setFlexGrow(0);
    }

    private void refrescarLineas() {
        if (pedidoEditable == null) {
            gridLineas.setItems(List.of());
            return;
        }
        List<LineaPedido> items = pedidoService.ordenarLineasParaVista(pedidoEditable.getLineaPedidos());
        gridLineas.setItems(items);
    }

    private void abrirEditorIngredientes(LineaPedido lp) {
        if (lp == null || pedidoEditable == null) return;

        if (!personalizacionHabilitada()) {
            Notification.show("La personalización de ingredientes requiere el módulo de ingredientes activo.", 3500, Notification.Position.MIDDLE);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setWidth("860px");
        dialog.setHeaderTitle("Ingredientes - " + (lp.getProducto() != null ? safe(lp.getProducto().getNombre()) : "-"));

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "12px");

        List<Ingrediente> disponibles;
        Map<UUID, ProductoIngrediente> recetaMap;
        try {
            disponibles = pedidoService.obtenerIngredientesDisponiblesLinea(lp);
            recetaMap = pedidoService.obtenerRecetaPorIngrediente(lp);
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            return;
        }

        if (disponibles.isEmpty()) {
            Span s = new Span("Este producto no tiene ingredientes configurados para editar.");
            s.getStyle().set("color", "var(--lumo-secondary-text-color)");
            content.add(s);
        } else {
            List<Ingrediente> porDefecto = new ArrayList<>();
            List<Ingrediente> noPorDefecto = new ArrayList<>();

            for (Ingrediente ing : disponibles) {
                if (ing == null || ing.getId() == null) continue;
                ProductoIngrediente pi = recetaMap != null ? recetaMap.get(ing.getId()) : null;
                boolean esPorDefecto = pi != null && pi.isPorDefecto();
                if (esPorDefecto) porDefecto.add(ing);
                else noPorDefecto.add(ing);
            }

            Comparator<Ingrediente> byNombre = Comparator.comparing(i -> safe(i.getNombre()), String.CASE_INSENSITIVE_ORDER);
            porDefecto.sort(byNombre);
            noPorDefecto.sort(byNombre);

            if (!porDefecto.isEmpty()) {
                H3 h = new H3("Por defecto");
                h.getStyle().set("margin", "0");
                h.getStyle().set("font-size", "var(--lumo-font-size-m)");
                content.add(h);
                for (Ingrediente ing : porDefecto) {
                    content.add(crearFilaIngrediente(lp, ing, recetaMap));
                }
            }

            if (!noPorDefecto.isEmpty()) {
                H3 h = new H3("No por defecto");
                h.getStyle().set("margin", porDefecto.isEmpty() ? "0" : "14px 0 0 0");
                h.getStyle().set("font-size", "var(--lumo-font-size-m)");
                content.add(h);
                for (Ingrediente ing : noPorDefecto) {
                    content.add(crearFilaIngrediente(lp, ing, recetaMap));
                }
            }
        }

        Button cerrar = new Button("Cerrar");
        cerrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cerrar.getStyle().set("font-weight", "800");
        cerrar.addClickListener(e -> dialog.close());

        dialog.add(content);
        dialog.getFooter().add(cerrar);
        dialog.open();
    }

    private Component crearFilaIngrediente(LineaPedido lp, Ingrediente ing, Map<UUID, ProductoIngrediente> recetaMap) {
        ProductoIngrediente pi = (recetaMap != null && ing != null) ? recetaMap.get(ing.getId()) : null;

        boolean esPorDefecto = pi != null && pi.isPorDefecto();
        boolean esOpcional = (pi == null) || pi.isOpcional();

        BigDecimal precioExtra = BigDecimal.ZERO;
        if (pi != null && pi.getPrecioExtra() != null) precioExtra = pi.getPrecioExtra();
        else if (ing.getPrecioExtra() != null) precioExtra = ing.getPrecioExtra();

        String precioExtraTxt = precioExtra.setScale(2, RoundingMode.HALF_UP).toPlainString();

        LineaPedidoIngrediente sel = pedidoService.obtenerSeleccionIngrediente(lp, ing.getId());
        boolean incluidoActual = (sel == null || sel.isIncluido());

        Span nombre = new Span(safe(ing.getNombre()));
        nombre.getStyle().set("font-weight", "900");

        Span sub = new Span(esPorDefecto ? "Por defecto" : "No por defecto");
        sub.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        sub.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout nombreBox = new VerticalLayout(nombre, sub);
        nombreBox.setPadding(false);
        nombreBox.setSpacing(false);
        nombreBox.getStyle().set("gap", "2px");

        Span badge = new Span();
        badge.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        badge.getStyle().set("font-weight", "900");
        badge.getStyle().set("padding", "4px 10px");
        badge.getStyle().set("border-radius", "999px");

        Checkbox quitar = new Checkbox("Quitar");
        quitar.setValue(!incluidoActual);

        IntegerField extra = new IntegerField();
        extra.setLabel("Extra\n+ " + precioExtraTxt + " € / extra");
        extra.setMin(0);
        extra.setStepButtonsVisible(true);
        extra.setWidth("220px");
        extra.setValue(sel != null ? Math.max(0, sel.getExtraCantidad()) : 0);

        HorizontalLayout fila = new HorizontalLayout(nombreBox, badge, quitar, extra);
        fila.setWidthFull();
        fila.setAlignItems(Alignment.CENTER);
        fila.expand(nombreBox);
        fila.getStyle().set("gap", "12px");

        Runnable aplicarEstiloYEstado = () -> {
            boolean incluido = !Boolean.TRUE.equals(quitar.getValue());

            if (incluido) {
                fila.getStyle().set("background", "var(--lumo-success-10pct)");
                fila.getStyle().set("border", "1px solid var(--lumo-success-50pct)");
                badge.setText("INCLUIDO");
                badge.getStyle().set("background", "var(--lumo-success-50pct)");
                badge.getStyle().set("color", "var(--lumo-base-color)");
                nombre.getStyle().remove("text-decoration");
                nombre.getStyle().remove("opacity");
            } else {
                fila.getStyle().set("background", "var(--lumo-error-10pct)");
                fila.getStyle().set("border", "1px solid var(--lumo-error-50pct)");
                badge.setText("QUITADO");
                badge.getStyle().set("background", "var(--lumo-error-50pct)");
                badge.getStyle().set("color", "var(--lumo-base-color)");
                nombre.getStyle().set("text-decoration", "line-through");
                nombre.getStyle().set("opacity", "0.85");
            }

            fila.getStyle().set("border-radius", "12px");
            fila.getStyle().set("padding", "10px 12px");

            boolean incluidoNow = !Boolean.TRUE.equals(quitar.getValue());
            if (!incluidoNow) {
                if (!Objects.equals(extra.getValue(), 0)) extra.setValue(0);
                extra.setEnabled(false);
            } else {
                extra.setEnabled(esOpcional);
            }
        };

        aplicarEstiloYEstado.run();

        if (!esOpcional) {
            quitar.setEnabled(false);
            extra.setEnabled(false);
        } else {
            quitar.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;

                boolean incluido = !Boolean.TRUE.equals(ev.getValue());
                try {
                    if (!incluido) {
                        if (!Objects.equals(extra.getValue(), 0)) extra.setValue(0);
                    }
                    pedidoService.aplicarSeleccionIngrediente(lp, ing, incluido, valueOrZero(extra.getValue()));
                    marcarCambio();
                    aplicarEstiloYEstado.run();
                    gridLineas.getDataProvider().refreshItem(lp);
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                    quitar.setValue(!incluido);
                    aplicarEstiloYEstado.run();
                }
            });

            extra.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;

                int v = valueOrZero(ev.getValue());
                if (!Objects.equals(extra.getValue(), v)) extra.setValue(v);

                if (Boolean.TRUE.equals(quitar.getValue())) quitar.setValue(false);

                boolean incluido = !Boolean.TRUE.equals(quitar.getValue());
                try {
                    pedidoService.aplicarSeleccionIngrediente(lp, ing, incluido, v);
                    marcarCambio();
                    aplicarEstiloYEstado.run();
                    gridLineas.getDataProvider().refreshItem(lp);
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });
        }

        return fila;
    }

    private void guardarCambios() {
        if (pedidoEditable == null) return;

        try {
            String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
            pedidoService.confirmarCambiosPedido(pedidoEditable, usuario);

            Notification.show("Cambios guardados", 2500, Notification.Position.BOTTOM_START);

            hayCambios = false;
            confirmarCambios.setEnabled(false);

            pedidoEditable = pedidoService.obtenerPorCodigo(pedidoEditable.getCodigo());
            refrescarLineas();

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
        }
    }

    private void marcarCambio() {
        hayCambios = true;
        confirmarCambios.setEnabled(true);
    }

    private void bloquearEdicion() {
        gridLineas.setEnabled(false);
        confirmarCambios.setEnabled(false);
    }

    private void desbloquearEdicion() {
        gridLineas.setEnabled(true);
        confirmarCambios.setEnabled(hayCambios);
    }

    private void limpiarVista(String mensaje) {
        this.pedidoEditable = null;
        this.hayCambios = false;
        info.setText(mensaje);
        gridLineas.setItems(List.of());
        bloquearEdicion();
    }

    private Component crearCard(Component inside) {
        VerticalLayout card = new VerticalLayout(inside);
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

    private int valueOrZero(Integer v) {
        return v == null ? 0 : Math.max(0, v);
    }
}