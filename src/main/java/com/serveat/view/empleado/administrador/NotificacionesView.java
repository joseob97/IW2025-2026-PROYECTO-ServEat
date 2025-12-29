package com.serveat.view.empleado.administrador;

import com.serveat.domain.notificaciones.PushNotificacion;
import com.serveat.service.notificaciones.PushNotificacionService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Notificaciones | ServEat")
@Route(value = "admin/notificaciones", layout = MainLayout.class)
public class NotificacionesView extends VerticalLayout {

    private final PushNotificacionService pushNotificacionService;
    private final Grid<PushNotificacion> grid = new Grid<>(PushNotificacion.class, false);

    public NotificacionesView(PushNotificacionService pushNotificacionService) {
        this.pushNotificacionService = pushNotificacionService;

        setSizeFull();
        setWidthFull();
        setPadding(false);
        setSpacing(true);

        H2 titulo = new H2("Notificaciones");
        titulo.getStyle().set("padding", "var(--lumo-space-m)");

        configurarGrid();

        add(titulo, grid);
        expand(grid);
    }

    private void configurarGrid() {
        grid.setWidthFull();
        grid.setHeightFull();

        grid.addColumn(PushNotificacion::getTitulo)
                .setHeader("Título")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(PushNotificacion::getMensaje)
                .setHeader("Mensaje")
                .setFlexGrow(1);

        grid.addColumn(PushNotificacion::getCreadaEn)
                .setHeader("Fecha")
                .setAutoWidth(true);

        grid.addComponentColumn(notificacion -> {
            Button ver = new Button("Ver", e -> mostrarDetalle(notificacion));
            Button eliminar = new Button("Eliminar", e -> {
                pushNotificacionService.eliminarNotificacion(notificacion.getId());
                refrescar();
            });

            HorizontalLayout acciones = new HorizontalLayout(ver, eliminar);
            return acciones;
        }).setHeader("Acciones");

        refrescar();
    }


    private void refrescar() {
        grid.setItems(pushNotificacionService.listarNotificaciones());
    }

    // MODAL PARA LEER MENSAJE COMPLETO
    private void mostrarDetalle(PushNotificacion notificacion) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        H2 titulo = new H2(notificacion.getTitulo());

        Paragraph mensaje = new Paragraph(notificacion.getMensaje());
        mensaje.getStyle().set("white-space", "pre-line");

        Button cerrar = new Button("Cerrar", e -> dialog.close());

        VerticalLayout contenido = new VerticalLayout(titulo, mensaje, cerrar);
        contenido.setPadding(true);
        contenido.setSpacing(true);

        dialog.add(contenido);
        dialog.open();
    }
}

