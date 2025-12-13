package com.serveat.view.empleado.administrador;

import com.serveat.service.seguridad.PlanPremiumService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.access.annotation.Secured;
import com.vaadin.flow.component.notification.Notification;

@Route(value = "empleado/admin", layout = MainLayout.class)
@Secured("ROLE_ADMIN")
public class PanelAdminView extends VerticalLayout {
    private final PlanPremiumService planPremiumService;

    public PanelAdminView(PlanPremiumService planPremiumService) {
        this.planPremiumService = planPremiumService;
        setSpacing(true);
        setPadding(true);

        H2 titulo = new H2("Panel de administración");

        // Menú de opciones para el admin
        RouterLink gestionarEmpleadosLink =
                new RouterLink("Gestionar empleados", GestionEmpleadosView.class);
        RouterLink suscripcionLink =
                new RouterLink("Suscripción / Plan", SuscripcionAdminView.class);

        add(titulo, gestionarEmpleadosLink, suscripcionLink);

        // SEGÚN EL PLAN QUE TENGA EL ADMIN
        // PROMOCIONES
        if (planPremiumService.tieneFeature("PROMOCIONES")) {
            add(new RouterLink("Gestionar promociones", GestionPromosView.class));
        } else {
            add(botonBloqueado("Gestionar promociones (requiere PRO)"));
        }

        // ESTADÍSTICAS
        if (planPremiumService.tieneFeature("ESTADISTICAS")) {
            add(new RouterLink("Estadísticas", EstadisticasAdminView.class));
        } else {
            add(botonBloqueado("Estadísticas (requiere PRO)"));
        }

        // EXPORTAR DATOS (puedes decidir si es PRO o BASIC)
        if (planPremiumService.tieneFeature("ESTADISTICAS")) {
            add(new RouterLink("Exportar datos", ExportarDatosView.class));
        } else {
            add(botonBloqueado("Exportar datos (requiere PRO)"));
        }

        // GESTIÓN DE USUARIOS (normalmente BASIC)
        add(new RouterLink("Gestionar usuarios", GestionUsuariosView.class));
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