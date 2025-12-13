package com.serveat.view.empleado.administrador;

import com.serveat.service.seguridad.PlanPremiumService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/admin/suscripcion", layout = MainLayout.class)
@PageTitle("Suscripción | Admin")
@Secured("ROLE_ADMIN")
public class SuscripcionAdminView extends VerticalLayout {

    private final PlanPremiumService planPremiumService;

    private final Paragraph planActual = new Paragraph();
    private final Paragraph features = new Paragraph();

    public SuscripcionAdminView(PlanPremiumService planPremiumService) {
        this.planPremiumService = planPremiumService;

        setPadding(true);
        setSpacing(true);

        add(new H2("Suscripción del establecimiento"), planActual, features);

        Button planBasico = new Button("Activar plan BÁSICO", e -> cambiarPlan("BASICO"));
        Button planPro = new Button("Activar plan PRO", e -> cambiarPlan("PRO"));

        add(planBasico, planPro);

        refrescar();
    }

    private void cambiarPlan(String codigoPlan) {
        try {
            planPremiumService.cambiarPlanActual(codigoPlan);
            Notification.show("Plan actualizado a: " + codigoPlan);
            refrescar();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void refrescar() {
        String plan = planPremiumService.obtenerCodigoPlanActual();
        planActual.setText("Plan actual: " + plan);

        features.setText("Features activos: " + String.join(", ", planPremiumService.listarFeaturesActivos()));
    }
}