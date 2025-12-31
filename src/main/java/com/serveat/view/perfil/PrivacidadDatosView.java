package com.serveat.view.perfil;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "privacidad", layout = MainLayout.class)
@PageTitle("Privacidad y datos personales")
@RolesAllowed({
        "CLIENTE",
        "ADMIN",
        "CAMARERO",
        "COCINERO",
        "REPARTIDOR"
})
public class PrivacidadDatosView extends VerticalLayout {

    public PrivacidadDatosView() {
        setMaxWidth("900px");
        setPadding(true);
        setSpacing(true);
        getStyle().set("margin", "0 auto");
        getStyle().set("gap", "14px");

        H2 titulo = new H2("Privacidad y datos personales");
        titulo.getStyle().set("margin", "0");

        Paragraph intro = new Paragraph(
                "En Serveat tratamos tus datos personales para poder ofrecerte el servicio del restaurante " +
                        "y gestionar tu cuenta, pedidos y comunicaciones relacionadas."
        );

        add(titulo, intro);

        add(seccionQueDatosRecogemos());
        add(seccionFinalidad());
        add(seccionConservacion());
        add(seccionTerceros());
        add(seccionDerechos());
        add(seccionContacto());

        add(barraVolver());
    }

    private VerticalLayout seccionQueDatosRecogemos() {
        VerticalLayout box = seccion("¿Qué datos personales se recopilan?");
        UnorderedList ul = new UnorderedList(
                new ListItem("Datos de cuenta: nombre, usuario y email."),
                new ListItem("Datos de contacto: teléfono y dirección (si el usuario los proporciona)."),
                new ListItem("Datos de pedidos: historial de pedidos, estado y detalles necesarios para la preparación y entrega."),
                new ListItem("Datos técnicos básicos de sesión para mantener la seguridad (por ejemplo, autenticación).")
        );
        box.add(ul);
        return box;
    }

    private VerticalLayout seccionFinalidad() {
        VerticalLayout box = seccion("¿Para qué se usan tus datos?");
        UnorderedList ul = new UnorderedList(
                new ListItem("Gestionar tu registro e inicio de sesión."),
                new ListItem("Tramitar pedidos (en mesa, recogida o domicilio) y su estado."),
                new ListItem("Contactarte si es necesario por motivos del servicio (por ejemplo, incidencias con el pedido)."),
                new ListItem("Mejorar la experiencia de uso y la seguridad de la plataforma.")
        );
        box.add(ul);
        return box;
    }

    private VerticalLayout seccionConservacion() {
        VerticalLayout box = seccion("¿Cuánto tiempo se conservan los datos?");
        box.add(new Paragraph(
                "Tus datos se conservan mientras tu cuenta esté activa. " +
                        "Si desactivas o eliminas tu cuenta, se aplicará el proceso correspondiente de baja y supresión."
        ));
        return box;
    }

    private VerticalLayout seccionTerceros() {
        VerticalLayout box = seccion("¿Se comparten tus datos con terceros?");
        box.add(new Paragraph(
                "No se comparten tus datos con terceros salvo obligación legal o cuando sea estrictamente necesario " +
                        "para prestar el servicio (por ejemplo, proveedores técnicos de infraestructura)."
        ));
        return box;
    }

    private VerticalLayout seccionDerechos() {
        VerticalLayout box = seccion("Tus derechos (GDPR)");
        UnorderedList ul = new UnorderedList(
                new ListItem("Consultar tus datos: puedes verlos en la sección “Mi perfil”."),
                new ListItem("Modificar tus datos: puedes editarlos desde “Mi perfil”."),
                new ListItem("Suprimir tus datos: puedes eliminar tu cuenta desde “Mi perfil”."),
                new ListItem("Desactivar tu cuenta: puedes desactivarla desde “Mi perfil”.")
        );
        box.add(ul);

        box.add(new Paragraph(
                "Si necesitas ejercer algún derecho adicional o tienes dudas, puedes contactar con el restaurante."
        ));
        return box;
    }

    private VerticalLayout seccionContacto() {
        VerticalLayout box = seccion("Contacto");
        box.add(new Paragraph(
                "Responsable del tratamiento: el restaurante propietario de la plataforma Serveat. " +
                        "Para cualquier consulta sobre privacidad, contacta con el restaurante."
        ));
        return box;
    }

    private VerticalLayout seccion(String titulo) {
        VerticalLayout box = new VerticalLayout();
        box.setPadding(false);
        box.setSpacing(false);
        box.getStyle().set("gap", "8px");
        box.getStyle().set("padding", "14px");
        box.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        box.getStyle().set("border-radius", "12px");
        box.getStyle().set("background", "var(--lumo-base-color)");

        H3 h3 = new H3(titulo);
        h3.getStyle().set("margin", "0");

        box.add(h3);
        return box;
    }

    private HorizontalLayout barraVolver() {
        Button volver = new Button("⬅ Volver a mi perfil");
        volver.getStyle().set("font-weight", "700");
        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("perfil")));

        HorizontalLayout hl = new HorizontalLayout(volver);
        hl.setWidthFull();
        hl.setJustifyContentMode(JustifyContentMode.START);
        hl.getStyle().set("margin-top", "6px");
        return hl;
    }
}