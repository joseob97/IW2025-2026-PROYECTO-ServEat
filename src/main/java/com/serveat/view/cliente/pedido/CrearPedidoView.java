package com.serveat.view.cliente.pedido;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
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

import java.util.List;

@PageTitle("Crear Pedido | Cliente")
@Route(value = "cliente/pedidos/crear", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class CrearPedidoView extends VerticalLayout {

    private final transient PedidoService pedidoService;
    private final transient ProductoService productoService;
    private final transient CategoriaService categoriaService;

    private transient Pedido pedidoEditable = new Pedido();

    private final TextField buscarProducto = new TextField("Buscar producto");
    private final ComboBox<String> filtroCategoria = new ComboBox<>("Categoría");
    private final ComboBox<Producto> comboProducto = new ComboBox<>("Producto");
    private final IntegerField cantidad = new IntegerField("Cantidad");
    private final Button anadir = new Button("Añadir");

    private final Grid<LineaPedido> gridLineas = new Grid<>(LineaPedido.class, false);
    private final Span total = new Span("Total: 0 €");

    private final Button confirmarPedido = new Button("✅ Confirmar pedido (Recoger)");

    public CrearPedidoView(PedidoService pedidoService,
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

        H3 titulo = new H3("Crear pedido para recoger");
        titulo.getStyle().set("margin", "0");
        add(titulo);

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
        anadir.addClickListener(e -> anadirProductoEnMemoria());

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

        H3 tituloPedido = new H3("Tu pedido");
        tituloPedido.getStyle().set("margin", "6px 0 0 0");
        add(tituloPedido);

        VerticalLayout cardPedido = crearCard();
        cardPedido.getStyle().set("gap", "12px");

        configurarGridLineas();

        gridLineas.setWidthFull();
        gridLineas.setHeight("360px");
        gridLineas.getStyle().set("border-radius", "10px");
        gridLineas.getStyle().set("overflow", "hidden");

        total.getStyle().set("font-weight", "600");
        total.getStyle().set("font-size", "1.05rem");

        HorizontalLayout pie = new HorizontalLayout(total);
        pie.setWidthFull();
        pie.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        cardPedido.add(gridLineas, pie);
        add(cardPedido);

        VerticalLayout cardConfirmar = crearCard();
        cardConfirmar.setPadding(true);
        cardConfirmar.getStyle().set("gap", "10px");

        confirmarPedido.setEnabled(false);
        confirmarPedido.getStyle().set("font-weight", "600");
        confirmarPedido.setWidth("360px");
        confirmarPedido.addClickListener(e -> confirmarPedido());

        HorizontalLayout filaConfirmar = new HorizontalLayout(confirmarPedido);
        filaConfirmar.setWidthFull();
        filaConfirmar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        cardConfirmar.add(filaConfirmar);
        add(cardConfirmar);

        recargarProductos();
        refrescarLineas();
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

    private void configurarGridLineas() {

        gridLineas.addColumn(lp -> lp.getProducto().getNombre())
                .setHeader("Producto")
                .setAutoWidth(true)
                .setFlexGrow(1);

        gridLineas.addColumn(LineaPedido::getCantidad)
                .setHeader("Cantidad")
                .setAutoWidth(true);

        gridLineas.addColumn(lp -> lp.calcularPrecio() + " €")
                .setHeader("Subtotal")
                .setAutoWidth(true);

        gridLineas.addComponentColumn(lp -> {
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
                    pedidoEditable = pedidoService.actualizarCantidadEnMemoria(
                            pedidoEditable,
                            lp.getProducto().getCodigo(),
                            nueva
                    );
                    refrescarLineas();
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
                    refrescarLineas();
                }
            });

            return qty;
        }).setHeader("Modificar");

        gridLineas.addComponentColumn(lp -> {
            Button borrar = new Button("❌");
            borrar.addClickListener(e -> {
                try {
                    pedidoEditable = pedidoService.eliminarProductoEnMemoria(
                            pedidoEditable,
                            lp.getProducto().getCodigo()
                    );
                    refrescarLineas();
                } catch (Exception ex) {
                    Notification.show("Error eliminando: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });
            return borrar;
        }).setHeader("Eliminar");
    }

    private void anadirProductoEnMemoria() {

        Producto prod = comboProducto.getValue();
        Integer qty = cantidad.getValue();

        if (prod == null || qty == null || qty <= 0) {
            Notification.show("Producto o cantidad inválidos", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoEditable = pedidoService.agregarProductoEnMemoria(pedidoEditable, prod, qty);

            cantidad.setValue(1);
            comboProducto.clear();

            refrescarLineas();

        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void confirmarPedido() {

        if (pedidoEditable.getLineaPedidos().isEmpty()) {
            Notification.show("Añade al menos un producto", 3000, Notification.Position.MIDDLE);
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar pedido");
        dialog.setText("¿Quieres confirmar el pedido para recoger?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Sí, confirmar");

        dialog.addConfirmListener(e -> ejecutarConfirmacion());
        dialog.open();
    }

    private void ejecutarConfirmacion() {
        try {
            Pedido creado = pedidoService.crearPedidoDesdeCliente(pedidoEditable);

            Notification.show("Pedido creado: " + creado.getCodigo(), 3500, Notification.Position.MIDDLE);

            pedidoEditable = new Pedido();
            refrescarLineas();

        } catch (Exception ex) {
            Notification.show("Error creando pedido: " + ex.getMessage(), 4500, Notification.Position.MIDDLE);
        }
    }

    private void refrescarLineas() {
        gridLineas.setItems(pedidoEditable != null ? pedidoEditable.getLineaPedidos() : List.of());
        total.setText("Total: " + (pedidoEditable != null ? pedidoEditable.calcularPrecioTotal() : "0") + " €");
        confirmarPedido.setEnabled(pedidoEditable != null && !pedidoEditable.getLineaPedidos().isEmpty());
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