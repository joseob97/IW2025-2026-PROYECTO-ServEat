package com.serveat.view.cliente.pedido;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pago.AjustePagoDTO;
import com.serveat.service.pago.AjustePagoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PageTitle("Modificar pedido | Cliente")
@Route(value = "cliente/pedidos/modificar", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class ModificarPedidoClienteView extends VerticalLayout implements HasUrlParameter<String> {

    private static final String SESSION_KEY_PREFIX = "AJUSTE_BORRADOR_"; // + codigoAjuste

    private final transient PedidoService pedidoService;
    private final transient PedidoCarritoService pedidoCarritoService;
    private final transient PedidoCalculoService pedidoCalculoService;
    private final transient ProductoService productoService;
    private final transient CategoriaService categoriaService;
    private final transient AjustePagoService ajustePagoService;

    private transient Pedido pedidoActual;   // leído de BD (para info/permiso)
    private transient Pedido pedidoEditable; // borrador en memoria (lo editas aquí)
    private String codigoPedido;

    private final Span info = new Span("Cargando pedido...");
    private final Span total = new Span("Total: 0 €");
    private final Span infoPago = new Span("");

    private final Grid<LineaPedido> gridLineas = new Grid<>(LineaPedido.class, false);

    private final TextField buscarProducto = new TextField("Buscar producto");
    private final ComboBox<String> filtroCategoria = new ComboBox<>("Categoría");
    private final ComboBox<Producto> comboProducto = new ComboBox<>("Producto");
    private final IntegerField cantidad = new IntegerField("Cantidad");

    private final Button anadir = new Button("Añadir");
    private final Button guardarCambios = new Button("✅ Guardar cambios");
    private final Button volver = new Button("⬅ Volver a mis pedidos");

    public ModificarPedidoClienteView(PedidoService pedidoService,
                                      PedidoCarritoService pedidoCarritoService,
                                      PedidoCalculoService pedidoCalculoService,
                                      ProductoService productoService,
                                      CategoriaService categoriaService,
                                      AjustePagoService ajustePagoService) {
        this.pedidoService = pedidoService;
        this.pedidoCarritoService = pedidoCarritoService;
        this.pedidoCalculoService = pedidoCalculoService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.ajustePagoService = ajustePagoService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Modificar pedido");
        titulo.getStyle().set("margin", "0");

        info.getStyle().set("color", "var(--lumo-secondary-text-color)");
        infoPago.getStyle().set("color", "var(--lumo-secondary-text-color)");
        infoPago.setVisible(false);

        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(ConsultaPedidosView.class)));
        add(titulo, info, infoPago, volver);

        VerticalLayout cardLineas = crearCard();
        configurarGridLineas();

        gridLineas.setWidthFull();
        gridLineas.setHeight("340px");
        gridLineas.getStyle().set("border-radius", "10px");
        gridLineas.getStyle().set("overflow", "hidden");

        total.getStyle().set("font-weight", "600");
        total.getStyle().set("font-size", "1.05rem");

        HorizontalLayout pie = new HorizontalLayout(total);
        pie.setWidthFull();
        pie.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        cardLineas.add(gridLineas, pie);
        add(cardLineas);

        VerticalLayout cardProductos = crearCard();
        cardProductos.getStyle().set("gap", "14px");

        configurarFiltrosProducto();
        configurarComboProducto();

        buscarProducto.setWidth("360px");
        filtroCategoria.setWidth("260px");

        HorizontalLayout filtros = new HorizontalLayout(buscarProducto, filtroCategoria);
        filtros.setWidthFull();
        filtros.setSpacing(false);
        filtros.getStyle().set("gap", "14px");
        filtros.setAlignItems(FlexComponent.Alignment.END);

        cantidad.setMin(1);
        cantidad.setStepButtonsVisible(true);
        cantidad.setValue(1);
        cantidad.setWidth("160px");

        anadir.setWidth("420px");
        anadir.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        anadir.getStyle().set("font-weight", "700");
        anadir.addClickListener(e -> anadirAlPedido());

        VerticalLayout bloqueProducto = new VerticalLayout(comboProducto, anadir);
        bloqueProducto.setPadding(false);
        bloqueProducto.setSpacing(false);
        bloqueProducto.getStyle().set("gap", "10px");
        bloqueProducto.setWidth("420px");

        HorizontalLayout filaAdd = new HorizontalLayout(bloqueProducto, cantidad);
        filaAdd.setWidthFull();
        filaAdd.setSpacing(false);
        filaAdd.getStyle().set("gap", "14px");
        filaAdd.setAlignItems(FlexComponent.Alignment.END);

        cardProductos.add(filtros, filaAdd);
        add(cardProductos);

        VerticalLayout cardGuardar = crearCard();
        cardGuardar.getStyle().set("gap", "10px");

        guardarCambios.setWidth("360px");
        guardarCambios.getStyle().set("font-weight", "600");
        guardarCambios.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        guardarCambios.addClickListener(e -> confirmarGuardado());

        HorizontalLayout filaGuardar = new HorizontalLayout(guardarCambios);
        filaGuardar.setWidthFull();
        filaGuardar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        cardGuardar.add(filaGuardar);
        add(cardGuardar);

        setUiEditable(false);
    }

    @Override
    public void setParameter(BeforeEvent event, String codigo) {
        this.codigoPedido = codigo;

        if (codigoPedido == null || codigoPedido.isBlank()) {
            Notification.show("Falta el código del pedido", 3000, Notification.Position.MIDDLE);
            event.forwardTo(ConsultaPedidosView.class);
            return;
        }

        cargarPedido();
        recargarProductos();
        refrescar();
    }

    private void cargarPedido() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            pedidoActual = pedidoService.cargarDetalleCliente(codigoPedido, username);

            // OJO: si aquí haces "pedidoEditable = pedidoActual", hay problema
            // Debe ser un borrador independiente (PedidoCarritoService ya lo trata como carrito/borrador).
            pedidoEditable = ajustePagoService.crearBorradorPedidoParaEdicion(pedidoActual);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String fecha = pedidoActual.getFechaCreacion() != null ? pedidoActual.getFechaCreacion().format(fmt) : "-";

            info.setText("Pedido " + pedidoActual.getCodigo()
                    + " | " + fecha
                    + " | Estado: " + safe(pedidoActual.getEstado())
                    + " | Cocina: " + safe(pedidoActual.getEstadoCocina()));

            boolean editable = pedidoService.puedeModificarCliente(pedidoActual);
            setUiEditable(editable);

            if (!editable) {
                Notification.show("Este pedido ya no se puede modificar", 3500, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate(ConsultaPedidosView.class));
        }
    }

    private void configurarGridLineas() {
        gridLineas.addColumn(lp -> lp.getProducto() != null ? lp.getProducto().getNombre() : "-")
                .setHeader("Producto").setAutoWidth(true).setFlexGrow(1);

        gridLineas.addColumn(LineaPedido::getCantidad)
                .setHeader("Cantidad").setAutoWidth(true);

        gridLineas.addColumn(lp -> {
                    if (lp.getPrecioUnitario() != null) return lp.getPrecioUnitario() + " €";
                    if (lp.getProducto() != null && lp.getProducto().getPrecio() != null) return lp.getProducto().getPrecio() + " €";
                    return "-";
                })
                .setHeader("Precio ud.").setAutoWidth(true);

        gridLineas.addColumn(lp -> pedidoCalculoService.calcularPrecioLinea(lp) + " €")
                .setHeader("Subtotal").setAutoWidth(true);

        gridLineas.addComponentColumn(lp -> {
            IntegerField qty = new IntegerField();
            qty.setMin(1);
            qty.setStepButtonsVisible(true);
            qty.setValue(lp.getCantidad());
            qty.setValueChangeMode(ValueChangeMode.ON_CHANGE);
            qty.setWidth("140px");

            qty.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;
                if (!pedidoService.puedeModificarCliente(pedidoActual)) return;

                Integer nueva = ev.getValue();
                if (nueva == null || nueva <= 0) {
                    Notification.show("Cantidad inválida", 2500, Notification.Position.MIDDLE);
                    qty.setValue(lp.getCantidad());
                    return;
                }

                try {
                    pedidoEditable = pedidoCarritoService.actualizarCantidadLinea(pedidoEditable, lp.getCodigo(), nueva);
                    refrescar();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                    refrescar();
                }
            });

            qty.setEnabled(pedidoService.puedeModificarCliente(pedidoActual));
            return qty;
        }).setHeader("Modificar");

        gridLineas.addComponentColumn(lp -> {
            Button borrar = new Button("❌");
            borrar.addClickListener(e -> {
                if (!pedidoService.puedeModificarCliente(pedidoActual)) return;

                try {
                    pedidoEditable = pedidoCarritoService.eliminarLinea(pedidoEditable, lp.getCodigo());
                    refrescar();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });

            borrar.setEnabled(pedidoService.puedeModificarCliente(pedidoActual));
            return borrar;
        }).setHeader("Eliminar");
    }

    private void configurarFiltrosProducto() {
        buscarProducto.setPlaceholder("Buscar por nombre");
        buscarProducto.setClearButtonVisible(true);
        buscarProducto.setValueChangeMode(ValueChangeMode.EAGER);

        filtroCategoria.setItems(
                categoriaService.listarCategorias().stream().map(Categoria::getNombre).toList()
        );
        filtroCategoria.setClearButtonVisible(true);

        buscarProducto.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                filtroCategoria.clear();
                recargarProductos();
            }
        });

        filtroCategoria.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                buscarProducto.clear();
                recargarProductos();
            }
        });
    }

    private void configurarComboProducto() {
        comboProducto.setItemLabelGenerator(p -> p.getNombre() + " - " + p.getPrecio() + "€");
        comboProducto.setWidth("420px");
    }

    private void recargarProductos() {
        String texto = buscarProducto.getValue();
        String categoria = filtroCategoria.getValue();

        if (texto != null && !texto.isBlank()) {
            comboProducto.setItems(productoService.buscarPorNombreParcial(texto));
            return;
        }

        if (categoria != null && !categoria.isBlank()) {
            comboProducto.setItems(productoService.buscarPorCategoria(categoria));
            return;
        }

        comboProducto.setItems(productoService.buscarPorNombreParcial(""));
    }

    private void anadirAlPedido() {
        if (!pedidoService.puedeModificarCliente(pedidoActual)) {
            Notification.show("Este pedido ya no se puede modificar", 3000, Notification.Position.MIDDLE);
            return;
        }

        Producto prod = comboProducto.getValue();
        Integer qty = cantidad.getValue();

        if (prod == null || qty == null || qty <= 0) {
            Notification.show("Producto o cantidad inválidos", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoEditable = pedidoCarritoService.agregarProducto(pedidoEditable, prod, qty);
            cantidad.setValue(1);
            comboProducto.clear();
            refrescar();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void confirmarGuardado() {
        if (!pedidoService.puedeModificarCliente(pedidoActual)) {
            Notification.show("Este pedido ya no se puede modificar", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (pedidoEditable == null || pedidoEditable.getLineaPedidos() == null || pedidoEditable.getLineaPedidos().isEmpty()) {
            Notification.show("El pedido no puede quedar vacío", 3000, Notification.Position.MIDDLE);
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar cambios");
        dialog.setText("¿Deseas continuar con el ajuste del pedido " + codigoPedido + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Sí, continuar");
        dialog.addConfirmListener(e -> prepararAjusteYNavegar());
        dialog.open();
    }

    /**
     * NO persiste el pedido.
     * Solo prepara el ajuste (pendiente) y guarda el borrador en sesión para usarlo en la pantalla de ajuste.
     */
    private void prepararAjusteYNavegar() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            AjustePagoDTO res = pedidoService.prepararAjusteCambiosCliente(pedidoEditable, username);

            if (res == null || res.getCodigoAjuste() == null || res.getCodigoAjuste().isBlank()) {
                // Si no hace falta ajuste (o es efectivo), aquí decides qué hacer.
                Notification.show(
                        (res != null && res.getMensaje() != null && !res.getMensaje().isBlank())
                                ? res.getMensaje()
                                : "No se ha generado ajuste.",
                        4500, Notification.Position.MIDDLE
                );
                getUI().ifPresent(ui -> ui.navigate(ConsultaPedidosView.class));
                return;
            }

            // Guardamos el borrador en sesión, ligado al ajuste.
            VaadinSession.getCurrent().setAttribute(SESSION_KEY_PREFIX + res.getCodigoAjuste(), pedidoEditable);

            // Ir a pantalla de ajuste
            getUI().ifPresent(ui -> ui.navigate("cliente/ajustes/" + res.getCodigoAjuste()));

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
        }
    }

    private void refrescar() {
        Collection<LineaPedido> col =
                (pedidoEditable != null && pedidoEditable.getLineaPedidos() != null)
                        ? pedidoEditable.getLineaPedidos()
                        : List.of();

        List<LineaPedido> items = new ArrayList<>(col);

        gridLineas.setItems(items);

        BigDecimal totalCalc = pedidoCalculoService.calcularTotalPedido(pedidoEditable);
        total.setText("Total: " + totalCalc + " €");

        boolean editable = pedidoService.puedeModificarCliente(pedidoActual);
        guardarCambios.setEnabled(editable && !items.isEmpty());
    }

    private void setUiEditable(boolean editable) {
        buscarProducto.setEnabled(editable);
        filtroCategoria.setEnabled(editable);
        comboProducto.setEnabled(editable);
        cantidad.setEnabled(editable);
        anadir.setEnabled(editable);
        gridLineas.setEnabled(true);
        guardarCambios.setEnabled(editable);
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
}