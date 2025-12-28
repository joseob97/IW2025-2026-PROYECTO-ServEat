package com.serveat.view.administrador;

import com.serveat.domain.notificaciones.PushNotificacion;
import com.serveat.service.notificaciones.PushNotificacionService;
import com.serveat.view.layout.AdminLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Notificaciones | ServEat")
@Route(value = "admin/notificaciones", layout = AdminLayout.class)
public class NotificacionesAdminView extends VerticalLayout {

    private final PushNotificacionService pushNotificacionService;
    private final Grid<PushNotificacion> grid = new Grid<>(PushNotificacion.class, false);

    public NotificacionesAdminView(PushNotificacionService pushNotificacionService) {
        this.pushNotificacionService = pushNotificacionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Notificaciones"));

        configurarGrid();

        add(grid);
    }

    private void configurarGrid() {

        grid.addColumn(PushNotificacion::getCreadaEn)
                .setHeader("Fecha")
                .setAutoWidth(true);

        grid.addColumn(PushNotificacion::getTitulo)
                .setHeader("Título")
                .setAutoWidth(true);

        grid.addColumn(PushNotificacion::getMensaje)
                .setHeader("Mensaje")
                .setFlexGrow(1);

        grid.addComponentColumn(notificacion -> {
            Button eliminar = new Button("Eliminar");
            eliminar.addClickListener(e -> {
                pushNotificacionService.eliminarNotificacion(notificacion.getId());
                refrescar();
            });
            return eliminar;
        }).setHeader("Acciones");

        refrescar();
        grid.setSizeFull();
    }

    private void refrescar() {
        grid.setItems(pushNotificacionService.listarNotificaciones());
    }
}
