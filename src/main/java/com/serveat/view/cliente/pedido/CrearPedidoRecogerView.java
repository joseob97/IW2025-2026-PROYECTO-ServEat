package com.serveat.view.cliente.pedido;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
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

@Route(value = "cliente/pedido/recoger", layout = MainLayout.class)
@PageTitle("Pedido para recoger")
@Secured("ROLE_CLIENTE")
public class CrearPedidoRecogerView extends VerticalLayout {

    private final PedidoService pedidoService;
    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    protected Pedido carrito = new Pedido();

    private final TextField buscar = new TextField("Buscar producto");
    private final ComboBox<String> categoria = new ComboBox<>("Categoría");
    private final ComboBox<Producto> producto = new ComboBox<>("Producto");
    private final IntegerField cantidad = new IntegerField("Cantidad");
    private final Button anadir = new Button("Añadir");

    private final Grid<LineaPedido> grid = new Grid<>(LineaPedido.class, false);
    private final Span total = new Span("Total: 0 €");

    private final Button continuar = new Button("➡ Continuar al pago");

    public CrearPedidoRecogerView(PedidoService pedidoService,
                                  ProductoService productoService,
                                  CategoriaService categoriaService) {

        this.pedidoService = pedidoService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setAlignItems(Alignment.CENTER);

        add(new H3("Pedido para recoger"));

        configurarFiltros();
        configurarGrid();

        continuar.setEnabled(false);
        continuar.addClickListener(e -> continuarPago());

        add(
                new HorizontalLayout(buscar, categoria),
                new HorizontalLayout(producto, cantidad, anadir),
                grid,
                total,
                continuar
        );

        recargarProductos();
        refrescar();
    }

    private void configurarFiltros() {
        buscar.setValueChangeMode(ValueChangeMode.EAGER);
        buscar.addValueChangeListener(e -> recargarProductos());

        categoria.setItems(
                categoriaService.listarCategorias().stream()
                        .map(Categoria::getNombre)
                        .toList()
        );
        categoria.addValueChangeListener(e -> recargarProductos());

        producto.setItemLabelGenerator(p -> p.getNombre() + " - " + p.getPrecio() + "€");

        cantidad.setMin(1);
        cantidad.setValue(1);

        anadir.addClickListener(e -> {
            try {
                carrito = pedidoService.agregarProductoEnMemoria(
                        carrito, producto.getValue(), cantidad.getValue()
                );
                refrescar();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });
    }

    private void configurarGrid() {
        grid.addColumn(lp -> lp.getProducto().getNombre()).setHeader("Producto");
        grid.addColumn(LineaPedido::getCantidad).setHeader("Cantidad");
        grid.addColumn(lp -> lp.calcularPrecio() + " €").setHeader("Subtotal");
    }

    private void recargarProductos() {
        if (!buscar.isEmpty()) {
            producto.setItems(productoService.buscarPorNombreParcial(buscar.getValue()));
            return;
        }
        if (categoria.getValue() != null) {
            producto.setItems(productoService.buscarPorCategoria(categoria.getValue()));
            return;
        }
        producto.setItems(productoService.buscarPorNombreParcial(""));
    }

    private void refrescar() {
        grid.setItems(carrito.getLineaPedidos());
        total.setText("Total: " + carrito.calcularPrecioTotal() + " €");
        continuar.setEnabled(!carrito.getLineaPedidos().isEmpty());
    }

    protected void continuarPago() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        getUI().ifPresent(ui -> {
            ui.getSession().setAttribute("pedidoOnlineCarrito", carrito);
            ui.getSession().setAttribute("pedidoOnlineTipo", TipoPedidoCliente.RECOGER);
            ui.getSession().setAttribute("pedidoOnlineUsername", username);
            ui.navigate("cliente/pedido/online/pasarela");
        });
    }
}