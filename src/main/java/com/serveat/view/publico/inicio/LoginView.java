package com.serveat.view.publico.inicio;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;

@Route("login")
@PageTitle("Iniciar Sesión | ServEat")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();
    private final Span errorMessage = new Span();

    public LoginView() {

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Iniciar sesión en ServEat");

        // Configuración del formulario
        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        // Mensaje de error personalizado (texto arriba del formulario)
        errorMessage.getStyle().set("color", "red");
        errorMessage.getStyle().set("font-weight", "bold");
        errorMessage.getStyle().set("margin-bottom", "10px");
        errorMessage.setVisible(false);

        add(title, errorMessage, loginForm);

        // ENLACE DE REGISTRO
        Span noCuenta = new Span("¿No tienes cuenta aún?");
        RouterLink registrate = new RouterLink(" Regístrate", RegistroClienteView.class);
        registrate.getStyle().set("color", "#0366d6");
        registrate.getStyle().set("font-weight", "bold");

        HorizontalLayout registroLayout = new HorizontalLayout(noCuenta, registrate);
        registroLayout.setSpacing(false);
        registroLayout.setPadding(false);

        add(registroLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        // Reset de estado
        errorMessage.setVisible(false);
        loginForm.setError(false);

        QueryParameters params = event.getLocation().getQueryParameters();

        if (params.getParameters().containsKey("disabled")) {
            // Usuario desactivado: solo nuestro mensaje, sin cuadro rojo de Vaadin
            errorMessage.setText("Tu usuario está desactivado. Contacta con un administrador.");
            errorMessage.setVisible(true);
            loginForm.setError(false);

        } else if (params.getParameters().containsKey("error")) {
            // Error genérico : cuadro rojo del LoginForm
            errorMessage.setVisible(false);
            loginForm.setError(true);
        }
    }
}
