package com.serveat.view.cliente.pedido;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pago.PagoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@PageTitle("Pedido Online | Cliente")
@Route(value = "cliente/pedido/online", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class CrearPedidoOnlineView extends VerticalLayout {

    // Servicios
    private final transient PedidoService pedidoService;
    private final transient ProductoService productoService;
    private final transient CategoriaService categoriaService;
    private final transient PagoService pagoService;

    // Estado
    private transient Pedido pedidoEnMemoria = new Pedido();

    // Filtros producto
    private final TextField buscarProducto = new TextField("Buscar producto");
    private final ComboBox<String> filtroCategoria = new ComboBox<>("Categoría");

    private final ComboBox<Producto> comboProducto = new ComboBox<>("Producto");
    private final IntegerField cantidad = new IntegerField("Cantidad");
    private final Button anadir = new Button("Añadir al carrito");

    // Carrito
    private final Grid<LineaPedido> gridCarrito = new Grid<>(LineaPedido.class, false);
    private final Span total = new Span("Total: 0 €");

    // Pago
    private final ComboBox<MetodoPago> metodoPago = new ComboBox<>("Método de pago");
    private final Button pagar = new Button("✅ Pagar y enviar pedido");

    public CrearPedidoOnlineView(PedidoService pedidoService,
                                 ProductoService productoService,
                                 CategoriaService categoriaService,
                                 PagoService pagoService) {

        this.pedidoService = pedidoService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.pagoService = pagoService;

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
        pagar.addClickListener(e -> confirmarPago());

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
            refrescarCarrito();
            Notification.show("Añadido al carrito", 1500, Notification.Position.BOTTOM_START);
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void confirmarPago() {

        if (pedidoEnMemoria.getLineaPedidos() == null || pedidoEnMemoria.getLineaPedidos().isEmpty()) {
            Notification.show("El carrito está vacío", 3000, Notification.Position.MIDDLE);
            return;
        }

        MetodoPago metodo = metodoPago.getValue();
        if (metodo == null) {
            Notification.show("Selecciona un método de pago", 3000, Notification.Position.MIDDLE);
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar pago");
        dialog.setText("¿Deseas pagar y enviar el pedido a cocina?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Sí, pagar");
        dialog.addConfirmListener(e -> ejecutarPago());
        dialog.open();
    }

    private void ejecutarPago() {

        try {
            String username = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName();

            // 1) Persistir pedido desde el carrito + asignar cliente
            Pedido pedidoCreado = pedidoService.crearPedidoDesdeCliente(pedidoEnMemoria, username);

            // 2) Crear pago y confirmarlo (referencia simulada)
            Pago pago = pagoService.iniciarPago(pedidoCreado, metodoPago.getValue());
            pagoService.confirmarPago(pago.getId(), generarReferencia());

            Notification.show("Pedido pagado y enviado a cocina", 4000, Notification.Position.MIDDLE);

            // 3) Reset carrito
            pedidoEnMemoria = new Pedido();
            metodoPago.clear();
            refrescarCarrito();

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private String generarReferencia() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void refrescarCarrito() {
        gridCarrito.setItems(
                pedidoEnMemoria != null ? pedidoEnMemoria.getLineaPedidos() : List.of()
        );

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