package com.serveat.view.empleado.administrador

import com.serveat.domain.menu.Categoria;
import com.serveat.service.menu.CategoriaService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Categorías | Admin")
@Route(value = "admin/categorias", layout = MainLayout.class)
public class CategoriaAdminView extends VerticalLayout {

    private final CategoriaService categoriaService;

    private final Grid<Categoria> grid = new Grid<>(Categoria.class, false);
    private final TextField nombre = new TextField("Nombre de categoría");

    public CategoriaAdminView(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;

        setPadding(true);
        setSizeFull();

        H2 titulo = new H2("Gestión de Categorías");

        Button crear = new Button("Crear", e -> crearCategoria());

        grid.addColumn(Categoria::getNombre).setHeader("Nombre");

        add(titulo, nombre, crear, grid);

        refrescar();
    }

    private void crearCategoria() {
        if (nombre.getValue().isBlank()) {
            Notification.show("Introduce un nombre válido");
            return;
        }

        categoriaService.crearCategoria(nombre.getValue());
        Notification.show("Categoría creada");
        nombre.clear();
        refrescar();
    }

    private void refrescar() {
        grid.setItems(categoriaService.listarCategorias());
    }
}