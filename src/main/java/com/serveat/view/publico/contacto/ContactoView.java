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
                new H2("Contacto"),
                new Paragraph("Dirección: " + datos.getDireccion()),
                new Paragraph("Teléfono: " + datos.getTelefono()),
                new Paragraph("Horario: " + datos.getHorario()),
                new Paragraph("Email: " + datos.getEmail())
        );
    }
}
