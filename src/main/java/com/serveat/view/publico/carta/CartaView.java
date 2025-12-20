package com.serveat.view.publico.carta;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@PageTitle("Carta | Serveat")
@Route(value = "carta", layout = MainLayout.class)
public class CartaView extends VerticalLayout {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    private final ComboBox<String> categoriaFiltro = new ComboBox<>("Categoría");
    private final TextField buscador = new TextField("Buscar");

    private final VerticalLayout contenido = new VerticalLayout();

    public CartaView(ProductoService productoService, CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;

        setWidthFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("gap", "14px");
        getStyle().set("max-width", "1200px");
        getStyle().set("margin", "0 auto");

        H2 titulo = new H2("Carta del restaurante");
        titulo.getStyle().set("margin", "0");

        configurarFiltros();

        HorizontalLayout filtros = new HorizontalLayout(categoriaFiltro, buscador);
        filtros.setWidthFull();
        filtros.setSpacing(false);
        filtros.getStyle().set("gap", "12px");

        categoriaFiltro.setWidth("260px");
        buscador.setWidth("360px");

        contenido.setWidthFull();
        contenido.setPadding(false);
        contenido.setSpacing(false);
        contenido.getStyle().set("gap", "18px");

        add(titulo, filtros, contenido);

        cargarProductos();
    }

    private void configurarFiltros() {
        categoriaFiltro.setItems(
                categoriaService.listarCategorias()
                        .stream()
                        .map(Categoria::getNombre)
                        .toList()
        );
        categoriaFiltro.setClearButtonVisible(true);
        categoriaFiltro.addValueChangeListener(e -> cargarProductos());

        buscador.setPlaceholder("Buscar por nombre...");
        buscador.setClearButtonVisible(true);
        buscador.setValueChangeMode(ValueChangeMode.EAGER);
        buscador.addValueChangeListener(e -> cargarProductos());
    }

    private void cargarProductos() {
        String categoria = categoriaFiltro.getValue();
        String texto = buscador.getValue() != null ? buscador.getValue() : "";

        List<Producto> productos = productoService.buscarPorNombreParcial(texto);

        if (categoria != null && !categoria.isBlank()) {
            productos = productoService.buscarPorCategoria(categoria);
            if (!texto.isBlank()) {
                String t = texto.toLowerCase();
                productos = productos.stream()
                        .filter(p -> p.getNombre() != null && p.getNombre().toLowerCase().contains(t))
                        .toList();
            }
        }

        renderizarPorCategorias(productos);
    }

    private void renderizarPorCategorias(List<Producto> productos) {
        contenido.removeAll();

        // Agrupar por categoría (evita NPEs)
        Map<String, List<Producto>> porCategoria = productos.stream()
                .collect(Collectors.groupingBy(p ->
                        p.getCategoria() != null ? p.getCategoria().getNombre() : "Otros"
                ));

        // Ordenar categorías
        List<String> categoriasOrdenadas = new ArrayList<>(porCategoria.keySet());
        categoriasOrdenadas.sort(String::compareToIgnoreCase);

        for (String nombreCat : categoriasOrdenadas) {

            H3 tituloCat = new H3(nombreCat);
            tituloCat.getStyle().set("margin", "10px 0 0 0");

            FlexLayout grid = new FlexLayout();
            grid.setWidthFull();
            grid.getStyle().set("gap", "14px");
            grid.getStyle().set("flex-wrap", "wrap");
            grid.getStyle().set("align-items", "stretch");

            List<Producto> lista = porCategoria.get(nombreCat).stream()
                    .sorted(Comparator.comparing(Producto::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();

            for (Producto p : lista) {
                grid.add(crearCardProducto(p));
            }

            contenido.add(tituloCat, grid);
        }

        if (productos.isEmpty()) {
            Span vacio = new Span("No hay productos con esos filtros.");
            vacio.getStyle().set("color", "var(--lumo-secondary-text-color)");
            contenido.add(vacio);
        }
    }

    private Component crearCardProducto(Producto p) {

        Image img = new Image(
                p.getImagenUrl() != null ? p.getImagenUrl() : "/images/productos/placeholder.png",
                p.getNombre() != null ? p.getNombre() : "Producto"
        );
        img.setWidthFull();
        img.setHeight("160px");
        img.getStyle().set("object-fit", "cover");
        img.getStyle().set("border-radius", "12px");

        Span nombre = new Span(p.getNombre() != null ? p.getNombre() : "-");
        nombre.getStyle().set("font-weight", "700");
        nombre.getStyle().set("font-size", "1.05rem");

        Span desc = new Span(p.getDescripcion() != null ? p.getDescripcion() : "");
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");
        desc.getStyle().set("font-size", "0.95rem");

        Span precio = new Span(p.getPrecio() != null ? (p.getPrecio() + " €") : "-");
        precio.getStyle().set("font-weight", "700");

        VerticalLayout card = new VerticalLayout(img, nombre, desc, precio);
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle().set("gap", "8px");

        // tamaño card (responsive)
        card.getStyle().set("width", "260px");
        card.getStyle().set("min-height", "290px");

        // estilo
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");

        return card;
    }
}