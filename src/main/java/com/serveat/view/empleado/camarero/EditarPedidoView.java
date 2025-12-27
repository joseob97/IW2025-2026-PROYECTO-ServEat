package com.serveat.view.empleado.camarero;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
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

import java.util.List;

@PageTitle("Editar Pedido | Camarero")
@Route(value = "empleado/camarero/pedidos/editar", layout = MainLayout.class)
@Secured("ROLE_CAMARERO")
public class EditarPedidoView extends VerticalLayout {

    // Servicios
    private final transient PedidoService pedidoService;
    private final transient PedidoCarritoService carritoService;
    private final transient ProductoService productoService;
    private final transient CategoriaService categoriaService;

    // Estado
    private transient Pedido pedidoOriginal;
    private transient Pedido pedidoEditable;
    private boolean hayCambios = false;

    // UI pedido
    private final IntegerField filtroMesa = new IntegerField("Filtrar por mesa");
    private final Grid<Pedido> gridPedidos = new Grid<>(Pedido.class, false);
    private final TextField codigoPedido = new TextField("Código pedido");

    // UI productos
    private final TextField buscarProducto = new TextField("Buscar producto");
    private final ComboBox<String> filtroCategoria = new ComboBox<>("Categoría");
    private final ComboBox<Producto> comboProducto = new ComboBox<>("Producto");
    private final IntegerField cantidad = new IntegerField("Cantidad");
    private final Button anadir = new Button("Añadir");

    // UI edición
    private final Grid<LineaPedido> gridLineas = new Grid<>(LineaPedido.class, false);
    private final Button confirmarCambios = new Button("✅ Confirmar cambios");

    public EditarPedidoView(PedidoService pedidoService,
                            PedidoCarritoService carritoService,
                            ProductoService productoService,
                            CategoriaService categoriaService) {

        this.pedidoService = pedidoService;
        this.carritoService = carritoService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        add(new H3("Editar pedido en curso"));

        crearBloqueSeleccionPedido();
        crearBloqueEdicion();

        cargarPedidos();
        recargarProductos();
        bloquearEdicion();
    }

    // SELECCIÓN PEDIDO

    private void crearBloqueSeleccionPedido() {

        VerticalLayout card = crearCard();

        filtroMesa.setMin(1);
        filtroMesa.setClearButtonVisible(true);
        filtroMesa.setValueChangeMode(ValueChangeMode.LAZY);
        filtroMesa.addValueChangeListener(e -> cargarPedidos());

        gridPedidos.addColumn(Pedido::getCodigo).setHeader("Código").setAutoWidth(true);
        gridPedidos.addColumn(p -> p.getReservaMesa() != null ? p.getReservaMesa().getNumeroMesa() : "-")
                .setHeader("Mesa").setAutoWidth(true);
        gridPedidos.addColumn(p -> p.getEstado() != null ? p.getEstado().name() : "-")
                .setHeader("Estado").setAutoWidth(true);

        gridPedidos.addSelectionListener(e -> {
            pedidoOriginal = e.getFirstSelectedItem().orElse(null);

            if (pedidoOriginal == null) {
                pedidoEditable = null;
                codigoPedido.clear();
                refrescarLineas();
                bloquearEdicion();
                return;
            }

            try {
                // Cargamos detalle real
                pedidoEditable = pedidoService.obtenerPorCodigo(pedidoOriginal.getCodigo());
                codigoPedido.setValue(pedidoEditable.getCodigo());
                refrescarLineas();

                hayCambios = false;
                confirmarCambios.setEnabled(false);
                desbloquearEdicion();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                bloquearEdicion();
            }
        });

        gridPedidos.setWidthFull();
        gridPedidos.setHeight("300px");

        card.add(filtroMesa, gridPedidos);
        add(card);
    }

    private void cargarPedidos() {
        Integer mesa = filtroMesa.getValue();

        if (mesa == null || mesa <= 0) {
            gridPedidos.setItems(pedidoService.listarPedidosModificables());
        } else {
            gridPedidos.setItems(pedidoService.listarPedidosModificablesPorMesa(mesa));
        }
    }

    // EDICIÓN

    private void crearBloqueEdicion() {

        VerticalLayout card = crearCard();

        codigoPedido.setReadOnly(true);
        codigoPedido.setWidth("320px");

        configurarFiltrosProducto();
        configurarComboProducto();

        cantidad.setMin(1);
        cantidad.setValue(1);
        cantidad.setStepButtonsVisible(true);
        cantidad.setWidth("160px");

        anadir.addClickListener(e -> anadirProducto());

        configurarGridLineas();

        confirmarCambios.setWidth("360px");
        confirmarCambios.setEnabled(false);
        confirmarCambios.addClickListener(e -> confirmarCambios());

        HorizontalLayout filaConfirmar = new HorizontalLayout(confirmarCambios);
        filaConfirmar.setWidthFull();
        filaConfirmar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        HorizontalLayout filaFiltros = new HorizontalLayout(buscarProducto, filtroCategoria);
        filaFiltros.setWidthFull();
        filaFiltros.getStyle().set("gap", "12px");

        HorizontalLayout filaAdd = new HorizontalLayout(comboProducto, cantidad, anadir);
        filaAdd.setWidthFull();
        filaAdd.getStyle().set("gap", "12px");
        filaAdd.setAlignItems(FlexComponent.Alignment.END);

        card.add(
                codigoPedido,
                filaFiltros,
                filaAdd,
                gridLineas,
                filaConfirmar
        );

        add(card);
    }

    // PRODUCTOS

    private void configurarFiltrosProducto() {

        buscarProducto.setPlaceholder("Buscar por nombre");
        buscarProducto.setClearButtonVisible(true);
        buscarProducto.setValueChangeMode(ValueChangeMode.EAGER);
        buscarProducto.setWidth("360px");

        filtroCategoria.setItems(
                categoriaService.listarCategorias().stream()
                        .map(Categoria::getNombre)
                        .toList()
        );
        filtroCategoria.setClearButtonVisible(true);
        filtroCategoria.setWidth("260px");

        buscarProducto.addValueChangeListener(e -> {
            if (!e.isFromClient()) return;
            filtroCategoria.clear();
            recargarProductos();
        });

        filtroCategoria.addValueChangeListener(e -> {
            if (!e.isFromClient()) return;
            buscarProducto.clear();
            recargarProductos();
        });
    }

    private void configurarComboProducto() {
        comboProducto.setItemLabelGenerator(
                p -> (p.getNombre() != null ? p.getNombre() : p.getCodigo()) + " - " + p.getPrecio() + "€"
        );
        comboProducto.setWidth("420px");
    }

    private void recargarProductos() {
        String texto = buscarProducto.getValue() != null ? buscarProducto.getValue().trim() : "";
        String cat = filtroCategoria.getValue();

        if (!texto.isBlank()) {
            comboProducto.setItems(productoService.buscarPorNombreParcial(texto));
            return;
        }

        if (cat != null && !cat.isBlank()) {
            comboProducto.setItems(productoService.buscarPorCategoria(cat));
            return;
        }

        comboProducto.setItems(productoService.buscarPorNombreParcial(""));
    }

    // GRID LÍNEAS

    private void configurarGridLineas() {

        gridLineas.addColumn(lp -> lp.getProducto() != null ? lp.getProducto().getNombre() : "-")
                .setHeader("Producto")
                .setAutoWidth(true)
                .setFlexGrow(1);

        gridLineas.addColumn(LineaPedido::getCantidad)
                .setHeader("Cantidad")
                .setAutoWidth(true);

        gridLineas.addComponentColumn(lp -> {
            IntegerField qty = new IntegerField();
            qty.setValue(lp.getCantidad());
            qty.setMin(1);
            qty.setStepButtonsVisible(true);
            qty.setWidth("160px");
            qty.setValueChangeMode(ValueChangeMode.ON_CHANGE);

            qty.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;
                if (pedidoEditable == null) return;

                Integer nueva = ev.getValue();
                if (nueva == null || nueva <= 0) {
                    qty.setValue(lp.getCantidad());
                    Notification.show("Cantidad inválida", 2500, Notification.Position.MIDDLE);
                    return;
                }

                try {
                    carritoService.actualizarCantidadLinea(pedidoEditable, lp.getCodigo(), nueva);
                    marcarCambio();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                    refrescarLineas();
                }
            });

            return qty;
        }).setHeader("Modificar");

        gridLineas.addComponentColumn(lp -> {
            Button borrar = new Button("❌");
            borrar.addClickListener(e -> {
                if (pedidoEditable == null) return;

                try {
                    carritoService.eliminarLinea(pedidoEditable, lp.getCodigo());
                    marcarCambio();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });
            return borrar;
        }).setHeader("Eliminar");

        gridLineas.setWidthFull();
        gridLineas.setHeight("320px");
    }

    private void refrescarLineas() {
        gridLineas.setItems(pedidoEditable != null && pedidoEditable.getLineaPedidos() != null
                ? pedidoEditable.getLineaPedidos()
                : List.of());
    }

    // ACCIONES

    private void anadirProducto() {

        if (pedidoEditable == null) {
            Notification.show("Selecciona un pedido", 2500, Notification.Position.MIDDLE);
            return;
        }

        Producto p = comboProducto.getValue();
        Integer qty = cantidad.getValue();

        if (p == null) {
            Notification.show("Selecciona un producto", 2500, Notification.Position.MIDDLE);
            return;
        }
        if (qty == null || qty <= 0) {
            Notification.show("Cantidad inválida", 2500, Notification.Position.MIDDLE);
            return;
        }

        try {
            carritoService.agregarProducto(pedidoEditable, p, qty);

            comboProducto.clear();
            cantidad.setValue(1);

            marcarCambio();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void confirmarCambios() {

        if (pedidoEditable == null) return;

        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            pedidoService.confirmarCambiosPedido(pedidoEditable, usuario);
            Notification.show("Cambios guardados correctamente", 3000, Notification.Position.MIDDLE);

            cargarPedidos();
            bloquearEdicion();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4500, Notification.Position.MIDDLE);
        }
    }

    private void marcarCambio() {
        hayCambios = true;
        confirmarCambios.setEnabled(true);
        refrescarLineas();
    }

    // UI

    private void bloquearEdicion() {
        buscarProducto.setEnabled(false);
        filtroCategoria.setEnabled(false);
        comboProducto.setEnabled(false);
        cantidad.setEnabled(false);
        anadir.setEnabled(false);
        gridLineas.setEnabled(false);
        confirmarCambios.setEnabled(false);
    }

    private void desbloquearEdicion() {
        buscarProducto.setEnabled(true);
        filtroCategoria.setEnabled(true);
        comboProducto.setEnabled(true);
        cantidad.setEnabled(true);
        anadir.setEnabled(true);
        gridLineas.setEnabled(true);
    }

    private VerticalLayout crearCard() {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setWidthFull();
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");
        card.getStyle().set("gap", "12px");
        return card;
    }
}