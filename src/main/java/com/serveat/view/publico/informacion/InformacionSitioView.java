package com.serveat.view.publico.informacion;

import com.serveat.domain.establecimiento.DatosLocal;
import com.serveat.service.establecimiento.DatosLocalService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Información | ServEat")
@Route(value = "info", layout = MainLayout.class)
public class InformacionSitioView extends VerticalLayout {

    public InformacionSitioView(DatosLocalService datosLocalService) {

        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        DatosLocal datos = datosLocalService.obtenerDatos();

        add(
                new H2(getTranslation("info.titulo")),
                new Paragraph(datos.getDescripcion()),
                new Paragraph(datos.getDescripcion2())
        );
    }
}
