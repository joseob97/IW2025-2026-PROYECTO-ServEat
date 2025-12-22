package com.serveat.view.cliente.pedido;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

@PageTitle("Pedido Online | Cliente")
@Route(value = "cliente/pedido/online", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class CrearPedidoOnlineView extends VerticalLayout {

    private final transient PedidoService pedidoService;
    private final transient ProductoService productoService;
    private final transient CategoriaService categoriaService;

    private transient Pedido pedidoEnMemoria = new Pedido();

    private final TextField buscarProducto = new TextField("Buscar producto");
    private final ComboBox<String> filtroCategoria = new ComboBox<>("Categoría");

    private final ComboBox<Producto> comboProducto = new ComboBox<>("Producto");
    private final IntegerField cantidad = new IntegerField("Cantidad");
    private final Button anadir = new Button("Añadir al carrito");

    private final Grid<LineaPedido> gridCarrito = new Grid<>(LineaPedido.class, false);
    private final Span total = new Span("Total: 0 €");

    private final ComboBox<MetodoPago> metodoPago = new ComboBox<>("Método de pago");
    private final Button pagar = new Button("✅ Ir a pasarela de pago");

    public CrearPedidoOnlineView(PedidoService pedidoService,
                                 ProductoService productoService,
                                 CategoriaService categoriaService) {

        this.pedidoService = pedidoService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;

        setSpacing(false);
        setPadding(true);
        setWidthFull();

        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Crear pedido online");
        titulo.getStyle().set("margin", "0");
        add(titulo);

        // Card: productos
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
        filtros.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        cantidad.setMin(1);
        cantidad.setStepButtonsVisible(true);
        cantidad.setValue(1);
        cantidad.setWidth("160px");

        anadir.setWidth("420px");
        anadir.addClickListener(e -> anadirAlCarrito());

        VerticalLayout bloqueProducto = new VerticalLayout(comboProducto, anadir);
        bloqueProducto.setPadding(false);
        bloqueProducto.setSpacing(false);
        bloqueProducto.getStyle().set("gap", "10px");
        bloqueProducto.setAlignItems(FlexComponent.Alignment.STRETCH);
        bloqueProducto.setWidth("420px");

        HorizontalLayout filaAdd = new HorizontalLayout(bloqueProducto, cantidad);
        filaAdd.setWidthFull();
        filaAdd.setSpacing(false);
        filaAdd.getStyle().set("gap", "14px");
        filaAdd.setAlignItems(FlexComponent.Alignment.END);
        filaAdd.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        cardProductos.add(filtros, filaAdd);
        add(cardProductos);

        // Card: carrito
        H3 tituloCarrito = new H3("Carrito");
        tituloCarrito.getStyle().set("margin", "6px 0 0 0");
        add(tituloCarrito);

        VerticalLayout cardCarrito = crearCard();
        cardCarrito.getStyle().set("gap", "12px");

        configurarGridCarrito();

        gridCarrito.setWidthFull();
        gridCarrito.getStyle().set("border-radius", "10px");
        gridCarrito.getStyle().set("overflow", "hidden");

        total.getStyle().set("font-weight", "600");
        total.getStyle().set("font-size", "1.05rem");

        HorizontalLayout pie = new HorizontalLayout(total);
        pie.setWidthFull();
        pie.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        cardCarrito.add(gridCarrito, pie);
        add(cardCarrito);

        // Card: pago
        H3 tituloPago = new H3("Pago");
        tituloPago.getStyle().set("margin", "6px 0 0 0");
        add(tituloPago);

        VerticalLayout cardPago = crearCard();
        cardPago.getStyle().set("gap", "12px");

        metodoPago.setItems(MetodoPago.values());
        metodoPago.setWidth("360px");
        metodoPago.setPlaceholder("Selecciona método");

        pagar.setWidth("360px");
        pagar.getStyle().set("font-weight", "600");
        pagar.addClickListener(e -> confirmarIrPasarela());

        HorizontalLayout filaPago = new HorizontalLayout(pagar);
        filaPago.setWidthFull();
        filaPago.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        cardPago.add(metodoPago, filaPago);
        add(cardPago);

        // Estado inicial
        recargarProductos();
        refrescarCarrito();
    }

    private void configurarFiltrosProducto() {
        buscarProducto.setPlaceholder("Buscar por nombre");
        buscarProducto.setClearButtonVisible(true);
        buscarProducto.setValueChangeMode(ValueChangeMode.EAGER);

        filtroCategoria.setItems(
                categoriaService.listarCategorias().stream()
                        .map(Categoria::getNombre)
                        .toList()
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

    private void configurarGridCarrito() {

        gridCarrito.addColumn(lp -> lp.getProducto().getNombre())
                .setHeader("Producto")
                .setAutoWidth(true)
                .setFlexGrow(1);

        gridCarrito.addColumn(LineaPedido::getCantidad)
                .setHeader("Cantidad")
                .setAutoWidth(true);

        gridCarrito.addColumn(lp -> lp.calcularPrecio() + " €")
                .setHeader("Subtotal")
                .setAutoWidth(true);

        gridCarrito.addComponentColumn(lp -> {
            IntegerField qty = new IntegerField();
            qty.setMin(1);
            qty.setStepButtonsVisible(true);
            qty.setValue(lp.getCantidad());
            qty.setValueChangeMode(ValueChangeMode.ON_CHANGE);
            qty.setWidth("140px");

            qty.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;

                Integer nueva = ev.getValue();
                if (nueva == null || nueva <= 0) {
                    Notification.show("Cantidad inválida", 2500, Notification.Position.MIDDLE);
                    qty.setValue(lp.getCantidad());
                    return;
                }

                try {
                    pedidoEnMemoria = pedidoService.actualizarCantidadEnMemoria(
                            pedidoEnMemoria,
                            lp.getProducto().getCodigo(),
                            nueva
                    );
                    refrescarCarrito();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                    refrescarCarrito();
                }
            });

            return qty;
        }).setHeader("Modificar");

        gridCarrito.addComponentColumn(lp -> {
            Button borrar = new Button("❌");
            borrar.addClickListener(e -> {
                try {
                    pedidoEnMemoria = pedidoService.eliminarProductoEnMemoria(
                            pedidoEnMemoria,
                            lp.getProducto().getCodigo()
                    );
                    refrescarCarrito();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });
            return borrar;
        }).setHeader("Eliminar");
    }

    private void anadirAlCarrito() {

        Producto prod = comboProducto.getValue();
        Integer qty = cantidad.getValue();

        if (prod == null || qty == null || qty <= 0) {
            Notification.show("Producto o cantidad inválidos", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoEnMemoria = pedidoService.agregarProductoEnMemoria(pedidoEnMemoria, prod, qty);
            cantidad.setValue(1);
            comboProducto.clear();
            refrescarCarrito();
            Notification.show("Añadido al carrito", 1500, Notification.Position.BOTTOM_START);
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void confirmarIrPasarela() {

        if (pedidoEnMemoria == null || pedidoEnMemoria.getLineaPedidos() == null || pedidoEnMemoria.getLineaPedidos().isEmpty()) {
            Notification.show("El carrito está vacío", 3000, Notification.Position.MIDDLE);
            return;
        }

        MetodoPago metodo = metodoPago.getValue();
        if (metodo == null) {
            Notification.show("Selecciona un método de pago", 3000, Notification.Position.MIDDLE);
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Ir a pasarela de pago");
        dialog.setText("Te llevaremos a una pasarela simulada para completar el pago.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Continuar");
        dialog.addConfirmListener(e -> irAPasarela());
        dialog.open();
    }

    private void irAPasarela() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            // Guardamos el "carrito" en sesión y nos vamos a la pasarela simulada
            getUI().ifPresent(ui -> {
                ui.getSession().setAttribute("pedidoOnlineCarrito", pedidoEnMemoria);
                ui.getSession().setAttribute("pedidoOnlineMetodoPago", metodoPago.getValue());
                ui.getSession().setAttribute("pedidoOnlineUsername", username);
                ui.navigate(PasarelaPagoSimuladaView.class);
            });

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void refrescarCarrito() {
        List<LineaPedido> items = (pedidoEnMemoria != null && pedidoEnMemoria.getLineaPedidos() != null)
                ? pedidoEnMemoria.getLineaPedidos()
                : List.of();

        gridCarrito.setItems(items);

        BigDecimal totalCalc = BigDecimal.ZERO;
        if (pedidoEnMemoria != null && pedidoEnMemoria.getLineaPedidos() != null) {
            totalCalc = pedidoEnMemoria.calcularPrecioTotal();
        }
        total.setText("Total: " + totalCalc + " €");
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