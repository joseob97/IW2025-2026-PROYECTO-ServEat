package com.serveat.view.publico.contacto;

import com.serveat.domain.establecimiento.DatosLocal;
import com.serveat.service.establecimiento.DatosLocalService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Contacto | ServEat")
@Route(value = "contacto", layout = MainLayout.class)
public class ContactoView extends VerticalLayout {

    public ContactoView(DatosLocalService datosLocalService) {

        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        DatosLocal datos = datosLocalService.obtenerDatos();

        add(
                new H2(getTranslation("contacto.titulo")),
                new Paragraph(getTranslation("contacto.direccion") + ": " + datos.getDireccion()),
                new Paragraph(getTranslation("contacto.telefono") + ": " + datos.getTelefono()),
                new Paragraph(getTranslation("contacto.horario") + ": " + datos.getHorario()),
                new Paragraph(getTranslation("contacto.email") + ": " + datos.getEmail())
        );
    }
}
