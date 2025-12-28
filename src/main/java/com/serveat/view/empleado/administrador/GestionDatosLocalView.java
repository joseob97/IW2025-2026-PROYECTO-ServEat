package com.serveat.view.empleado.administrador;

import com.serveat.domain.establecimiento.DatosLocal;
import com.serveat.service.establecimiento.DatosLocalService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/admin/datos-local", layout = MainLayout.class)
@Secured("ROLE_ADMIN")
public class GestionDatosLocalView extends VerticalLayout {

    private final DatosLocalService datosLocalService;
    private final BeanValidationBinder<DatosLocal> binder;

    private DatosLocal datosLocal;

    public GestionDatosLocalView(DatosLocalService datosLocalService) {
        this.datosLocalService = datosLocalService;
        this.binder = new BeanValidationBinder<>(DatosLocal.class);

        setWidth("700px");
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Gestión de la información del local");

        /* ===== CAMPOS ===== */

        TextField nombreLocal = new TextField("Nombre del local");
        TextArea descripcion = new TextArea("Descripción principal");
        TextArea descripcion2 = new TextArea("Descripción secundaria");
        TextField horario = new TextField("Horario");

        TextField telefono = new TextField("Teléfono");
        EmailField email = new EmailField("Email");
        TextField direccion = new TextField("Dirección");

        descripcion.setHeight("120px");
        descripcion2.setHeight("120px");

        FormLayout formulario = new FormLayout(
                nombreLocal,
                descripcion,
                descripcion2,
                horario,
                telefono,
                email,
                direccion
        );

        formulario.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        /* ===== BINDER ===== */
        binder.bind(nombreLocal, "nombreLocal");
        binder.bind(descripcion, "descripcion");
        binder.bind(descripcion2, "descripcion2");
        binder.bind(horario, "horario");
        binder.bind(telefono, "telefono");
        binder.bind(email, "email");
        binder.bind(direccion, "direccion");

        /* ===== CARGA INICIAL ===== */
        datosLocal = datosLocalService.obtenerDatos();
        binder.readBean(datosLocal);

        /* ===== BOTÓN GUARDAR ===== */
        Button guardar = new Button("Guardar cambios");
        guardar.addClickListener(e -> guardar());

        add(titulo, formulario, guardar);
    }

    private void guardar() {
        try {
            binder.writeBean(datosLocal);
            datosLocalService.guardar(datosLocal);

            Notification.show(
                    "Datos del local guardados correctamente",
                    3000,
                    Notification.Position.MIDDLE
            );
        } catch (Exception ex) {
            Notification.show(
                    "Revisa los campos marcados en rojo",
                    3000,
                    Notification.Position.MIDDLE
            );
        }
    }
}
