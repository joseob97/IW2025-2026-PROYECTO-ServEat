package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.empleado.administrador.estadisticas.EstadisticasAdminView;
import com.serveat.view.empleado.administrador.productos.GestionProductosView;
import com.serveat.view.empleado.administrador.productos.GestionIngredientesView;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/admin", layout = MainLayout.class)
@Secured("ROLE_ADMIN")
public class PanelAdminView extends VerticalLayout {

    private final FeatureService featureService;

    public PanelAdminView(FeatureService featureService) {
        this.featureService = featureService;
        setSpacing(true);
        setPadding(true);

        H2 titulo = new H2("Panel de administración");

        RouterLink gestionarEmpleadosLink =
                new RouterLink("Gestionar empleados", GestionEmpleadosView.class);

        /* Gestión de usuarios */
        add(new RouterLink("Gestionar usuarios", GestionClientesView.class));

        RouterLink suscripcionLink =
                new RouterLink("Suscripción / Plan", SuscripcionAdminView.class);

        add(titulo, gestionarEmpleadosLink, suscripcionLink);

        /* Productos */
        add(new RouterLink("Gestión de Productos", GestionProductosView.class));

        /* Ingredientes */
        if (featureService.tieneFeature(Feature.INGREDIENTES)) {
            add(new RouterLink("Gestión de Ingredientes", GestionIngredientesView.class));
        } else {
            add(botonBloqueado("Gestión de Ingredientes (requiere PRO)"));
        }

        /* Promociones */
        if (featureService.tieneFeature(Feature.PROMOCIONES)) {
            add(new RouterLink("Gestionar promociones", GestionPromosView.class));
        } else {
            add(botonBloqueado("Gestionar promociones (requiere PRO)"));
        }

        /* Estadísticas */
        if (featureService.tieneFeature(Feature.ESTADISTICAS)) {
            add(new RouterLink("Estadísticas", EstadisticasAdminView.class));
        } else {
            add(botonBloqueado("Estadísticas (requiere PRO)"));
        }

        /* Exportar datos */
        if (featureService.tieneFeature(Feature.EXPORTAR_DATOS)) {
            add(new RouterLink("Exportar datos", ExportarDatosView.class));
        } else {
            add(botonBloqueado("Exportar datos (requiere PRO)"));
        }
    }

    private Button botonBloqueado(String texto) {
        Button b = new Button(texto);
        b.setEnabled(false);
        b.addClickListener(e ->
                Notification.show("Funcionalidad disponible con plan PRO",
                        3000, Notification.Position.MIDDLE)
        );
        return b;
    }
}