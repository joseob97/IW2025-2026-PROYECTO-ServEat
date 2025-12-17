package com.serveat.view.empleado.camarero;

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
    private final transient ProductoService productoService;
    private final transient CategoriaService categoriaService;

    // Estado
    private Pedido pedidoOriginal;
    private Pedido pedidoEditable;
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
                            ProductoService productoService,
                            CategoriaService categoriaService) {

        this.pedidoService = pedidoService;
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
        filtroMesa.addValueChangeListener(e -> cargarPedidos());

        gridPedidos.addColumn(Pedido::getCodigo).setHeader("Código");
        gridPedidos.addColumn(p -> p.getReservaMesa() != null
                ? p.getReservaMesa().getNumeroMesa() : "-").setHeader("Mesa");
        gridPedidos.addColumn(p -> p.getEstado().name()).setHeader("Estado");

        gridPedidos.addSelectionListener(e -> {
            pedidoOriginal = e.getFirstSelectedItem().orElse(null);

            if (pedidoOriginal == null) {
                bloquearEdicion();
                return;
            }

            // Pedido editable en memoria
            pedidoEditable = pedidoService.obtenerPorCodigo(pedidoOriginal.getCodigo());
            codigoPedido.setValue(pedidoEditable.getCodigo());
            refrescarLineas();

            hayCambios = false;
            confirmarCambios.setEnabled(false);
            desbloquearEdicion();
        });

        gridPedidos.setWidthFull();
        gridPedidos.setHeight("300px");

        card.add(filtroMesa, gridPedidos);
        add(card);
    }

    private void cargarPedidos() {
        Integer mesa = filtroMesa.getValue();

        if (mesa == null) {
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

        anadir.addClickListener(e -> anadirProducto());

        configurarGridLineas();

        confirmarCambios.setWidth("360px");
        confirmarCambios.setEnabled(false);
        confirmarCambios.addClickListener(e -> confirmarCambios());

        HorizontalLayout filaConfirmar = new HorizontalLayout(confirmarCambios);
        filaConfirmar.setWidthFull();
        filaConfirmar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        card.add(
                codigoPedido,
                new HorizontalLayout(buscarProducto, filtroCategoria),
                new HorizontalLayout(comboProducto, cantidad, anadir),
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

        filtroCategoria.setItems(
                categoriaService.listarCategorias().stream()
                        .map(Categoria::getNombre)
                        .toList()
        );

        buscarProducto.addValueChangeListener(e -> {
            filtroCategoria.clear();
            recargarProductos();
        });

        filtroCategoria.addValueChangeListener(e -> {
            buscarProducto.clear();
            recargarProductos();
        });
    }

    private void configurarComboProducto() {
        comboProducto.setItemLabelGenerator(
                p -> p.getNombre() + " - " + p.getPrecio() + "€"
        );
    }

    private void recargarProductos() {

        if (!buscarProducto.getValue().isBlank()) {
            comboProducto.setItems(
                    productoService.buscarPorNombreParcial(buscarProducto.getValue())
            );
            return;
        }

        if (filtroCategoria.getValue() != null) {
            comboProducto.setItems(
                    productoService.buscarPorCategoria(filtroCategoria.getValue())
            );
            return;
        }

        comboProducto.setItems(
                productoService.buscarPorNombreParcial("")
        );
    }

    // GRID LINEAS

    private void configurarGridLineas() {

        gridLineas.addColumn(lp -> lp.getProducto().getNombre())
                .setHeader("Producto");

        gridLineas.addColumn(LineaPedido::getCantidad)
                .setHeader("Cantidad");

        gridLineas.addComponentColumn(lp -> {
            IntegerField qty = new IntegerField();
            qty.setValue(lp.getCantidad());
            qty.setMin(1);

            qty.addValueChangeListener(e -> {
                pedidoEditable = pedidoService.actualizarCantidadEnMemoria(
                        pedidoEditable,
                        lp.getProducto().getCodigo(),
                        e.getValue()
                );
                marcarCambio();
            });

            return qty;
        }).setHeader("Modificar");

        gridLineas.addComponentColumn(lp -> {
            Button borrar = new Button("❌");
            borrar.addClickListener(e -> {
                pedidoEditable = pedidoService.eliminarProductoEnMemoria(
                        pedidoEditable,
                        lp.getProducto().getCodigo()
                );
                marcarCambio();
            });
            return borrar;
        }).setHeader("Eliminar");
    }

    private void refrescarLineas() {
        gridLineas.setItems(
                pedidoEditable != null ? pedidoEditable.getLineaPedidos() : List.of()
        );
    }

    // ACCIONES

    private void anadirProducto() {

        if (comboProducto.getValue() == null) {
            Notification.show("Selecciona un producto");
            return;
        }

        pedidoEditable = pedidoService.agregarProductoEnMemoria(
                pedidoEditable,
                comboProducto.getValue(),
                cantidad.getValue()
        );

        marcarCambio();
    }

    private void confirmarCambios() {

        String usuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        pedidoService.confirmarCambiosPedido(pedidoEditable, usuario);

        Notification.show("Cambios guardados correctamente");

        cargarPedidos();
        bloquearEdicion();
    }

    private void marcarCambio() {
        hayCambios = true;
        confirmarCambios.setEnabled(true);
        refrescarLineas();
    }

    // UI

    private void bloquearEdicion() {
        comboProducto.setEnabled(false);
        cantidad.setEnabled(false);
        anadir.setEnabled(false);
        gridLineas.setEnabled(false);
        confirmarCambios.setEnabled(false);
    }

    private void desbloquearEdicion() {
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
        return card;
    }
}