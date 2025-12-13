package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/admin/exportar", layout = MainLayout.class)
@PageTitle("Exportar datos | Admin")
@Secured("ROLE_ADMIN")
public class ExportarDatosView extends VerticalLayout {

    public ExportarDatosView(FeatureService featureService) {
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Exportar datos");

        if (!featureService.tieneFeature(Feature.EXPORTAR_DATOS)) {
            add(titulo,
                    new Paragraph("La exportación avanzada requiere el plan PRO."),
                    new Paragraph("Ve a “Suscripción / Plan” para activarla."));
            return;
        }

        Button exportarCsv = new Button("Exportar CSV (demo)", e ->
                Notification.show("Exportación demo (se implementa en el próximo sprint).")
        );

        add(titulo, new Paragraph("Exporta ventas/pedidos/productos (sprint siguiente)."), exportarCsv);
    }
}