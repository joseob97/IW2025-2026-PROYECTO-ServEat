package com.serveat.view.empleado.administrador.productos;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.service.menu.IngredienteService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.math.BigDecimal;
import java.util.UUID;

@PageTitle("Ingredientes | Admin")
@Route(value = "empleado/admin/ingredientes", layout = MainLayout.class)
@Secured("ROLE_ADMIN")
public class GestionIngredientesView extends VerticalLayout {

    private final transient IngredienteService ingredienteService;

    private final Grid<Ingrediente> grid = new Grid<>(Ingrediente.class, false);
    private final TextField filtroNombre = new TextField("Buscar por nombre");

    public GestionIngredientesView(IngredienteService ingredienteService) {
        this.ingredienteService = ingredienteService;

        setPadding(true);
        setSpacing(true);

        add(new H2("Gestión de ingredientes"));

        configurarToolbar();
        configurarGrid();

        refrescar();
    }

    private void configurarToolbar() {
        filtroNombre.setPlaceholder("Escribe para filtrar...");
        filtroNombre.setClearButtonVisible(true);
        filtroNombre.addValueChangeListener(e -> refrescar());

        Button nuevo = new Button("➕ Nuevo", e -> abrirDialogo(null));

        Button editar = new Button("✏️ Editar", e -> {
            Ingrediente sel = grid.asSingleSelect().getValue();
            if (sel == null) {
                Notification.show("Selecciona un ingrediente", 2500, Notification.Position.MIDDLE);
                return;
            }
            abrirDialogo(sel);
        });

        Button eliminar = new Button("🗑 Eliminar", e -> {
            Ingrediente sel = grid.asSingleSelect().getValue();
            if (sel == null) {
                Notification.show("Selecciona un ingrediente", 2500, Notification.Position.MIDDLE);
                return;
            }
            try {
                ingredienteService.eliminar(sel.getId());
                Notification.show("Ingrediente eliminado");
                refrescar();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        HorizontalLayout toolbar = new HorizontalLayout(filtroNombre, nuevo, editar, eliminar);
        toolbar.setAlignItems(Alignment.END);
        add(toolbar);
    }

    private void configurarGrid() {
        grid.addColumn(Ingrediente::getNombre).setHeader("Nombre").setFlexGrow(1);
        grid.addColumn(i -> i.getPrecioExtra() == null ? "0.00" : i.getPrecioExtra().setScale(2).toPlainString())
                .setHeader("Precio extra").setAutoWidth(true);

        grid.setWidthFull();
        add(grid);
    }

    private void refrescar() {
        String q = filtroNombre.getValue();
        grid.setItems(ingredienteService.buscarPorNombre(q));
    }

    private void abrirDialogo(Ingrediente base) {
        boolean editando = base != null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(editando ? "Editar ingrediente" : "Nuevo ingrediente");

        TextField nombre = new TextField("Nombre");
        nombre.setWidthFull();

        NumberField precioExtra = new NumberField("Precio extra (€)");
        precioExtra.setStep(0.10);
        precioExtra.setMin(0);
        precioExtra.setWidthFull();

        if (editando) {
            nombre.setValue(base.getNombre() == null ? "" : base.getNombre());
            if (base.getPrecioExtra() != null) {
                precioExtra.setValue(base.getPrecioExtra().doubleValue());
            }
        }

        Button guardar = new Button("Guardar", e -> {
            try {
                BigDecimal precio = (precioExtra.getValue() == null)
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(precioExtra.getValue());

                if (!editando) {
                    ingredienteService.crear(nombre.getValue(), precio);
                    Notification.show("Ingrediente creado");
                } else {
                    UUID id = base.getId();
                    ingredienteService.actualizar(id, nombre.getValue(), precio);
                    Notification.show("Ingrediente actualizado");
                }

                dialog.close();
                refrescar();

            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        Button cancelar = new Button("Cancelar", e -> dialog.close());

        HorizontalLayout acciones = new HorizontalLayout(guardar, cancelar);

        VerticalLayout content = new VerticalLayout(nombre, precioExtra, acciones);
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidth("420px");

        dialog.add(content);
        dialog.open();
    }
}