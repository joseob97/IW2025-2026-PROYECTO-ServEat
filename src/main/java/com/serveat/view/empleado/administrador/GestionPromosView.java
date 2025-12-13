package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/admin/promociones", layout = MainLayout.class)
@PageTitle("Promociones | Admin")
@Secured("ROLE_ADMIN")
public class GestionPromosView extends VerticalLayout {

    public GestionPromosView(FeatureService featureService) {
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Gestión de promociones");

        if (!featureService.tieneFeature(Feature.PROMOCIONES)) {
            add(titulo,
                    new Paragraph("Esta funcionalidad requiere el plan PRO."),
                    new Paragraph("Los clientes tampoco verán promociones si no está activado."));
            return;
        }

        add(titulo,
                new Paragraph("Aquí podrás crear/editar promociones (sprint siguiente)."));
    }
}