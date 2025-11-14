package com.serveat.view.publico.carta;

import com.serveat.domain.menu.Producto;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@PageTitle("Carta | Serveat")
@Route(value = "carta", layout = MainLayout.class)
public class CartaView extends VerticalLayout {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    private final Grid<Producto> grid = new Grid<>(Producto.class, false);
    private final ComboBox<String> categoriaFiltro = new ComboBox<>("Categoría");
    private final TextField buscador = new TextField("Buscar");

    public CartaView(ProductoService productoService, CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;

        setSizeFull();
        setPadding(true);

        H2 titulo = new H2("Carta del restaurante");

        configurarFiltros();
        configurarGrid();

        HorizontalLayout filtros = new HorizontalLayout(categoriaFiltro, buscador);
        filtros.setWidthFull();

        add(titulo, filtros, grid);

        cargarProductos();
    }

    private void configurarFiltros() {

        categoriaFiltro.setItems(
                categoriaService.listarCategorias()
                        .stream()
                        .map(c -> c.getNombre())
                        .toList()
        );

        categoriaFiltro.addValueChangeListener(e -> cargarProductos());

        buscador.setPlaceholder("Buscar nombre parcial...");
        buscador.setValueChangeMode(ValueChangeMode.EAGER);
        buscador.addValueChangeListener(e -> cargarProductos());
    }

    private void configurarGrid() {
        grid.addColumn(Producto::getCodigo).setHeader("Código");
        grid.addColumn(Producto::getNombre).setHeader("Nombre");
        grid.addColumn(Producto::getDescripcion).setHeader("Descripción").setFlexGrow(1);
        grid.addColumn(p -> p.getPrecio() + "€").setHeader("Precio");
        grid.addColumn(p -> p.getCategoria() != null ? p.getCategoria().getNombre() : "-")
                .setHeader("Categoría");

        grid.setSizeFull();
    }

    private void cargarProductos() {

        String categoria = categoriaFiltro.getValue();
        String texto = buscador.getValue();

        List<Producto> resultado = productoService.buscarPorNombreParcial(texto != null ? texto : "");

        if (categoria != null) {
            resultado = productoService.buscarPorCategoria(categoria);
        }

        grid.setItems(resultado);
    }
}