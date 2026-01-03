package com.serveat.view.publico.inicio;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Inicio | ServEat")
@Route(value = "", layout = MainLayout.class)
public class InicioView extends VerticalLayout implements BeforeEnterObserver {

    public InicioView() {

        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        H2 titulo = new H2(getTranslation("inicio.titulo"));

        Paragraph subtitulo = new Paragraph(
                getTranslation("inicio.subtitulo")
        );

        Button verCarta = new Button(
                getTranslation("inicio.verCarta"),
                e -> getUI().ifPresent(ui -> ui.navigate("carta"))
        );
        verCarta.getStyle().set("margin-top", "12px");

        add(titulo, subtitulo, verCarta);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        boolean vieneDeLogout = event.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("logout");

        if (vieneDeLogout) {

            Notification notification = Notification.show(
                    getTranslation("inicio.logout.ok"),
                    3000,
                    Notification.Position.MIDDLE
            );

            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Limpiar la URL (quitar ?logout)
            UI.getCurrent().getPage().getHistory().replaceState(null, "");
        }
    }
}
