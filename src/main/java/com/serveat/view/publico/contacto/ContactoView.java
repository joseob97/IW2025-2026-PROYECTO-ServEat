package com.serveat.view.publico.contacto;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Contacto | ServEat")
@Route(value = "contacto", layout = MainLayout.class)
public class ContactoView extends VerticalLayout {

    public ContactoView() {
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        add(
                new H2("Contacto"),
                new Paragraph("Dirección: Calle Ejemplo 123, Cádiz"),
                new Paragraph("Teléfono: +34 600 000 000"),
                new Paragraph("Horario: L-D 12:00 - 23:30"),
                new Paragraph("Email: contacto@serveat.com")
        );
    }
}