package com.serveat.view.publico.inicio;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("login")
@PageTitle("Iniciar Sesión | ServEat")
public class LoginView extends VerticalLayout {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Iniciar sesión en ServEat");

        // Configuración del formulario
        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        add(title, loginForm);

        // ENLACE DE REGISTRO
        Span noCuenta = new Span("¿No tienes cuenta aún?");
        RouterLink registrate = new RouterLink(" Regístrate", RegistroClienteView.class);
        registrate.getStyle().set("color", "#0366d6");
        registrate.getStyle().set("font-weight", "bold");

        HorizontalLayout registroLayout = new HorizontalLayout(noCuenta, registrate);
        registroLayout.setSpacing(false);
        registroLayout.setPadding(false);

        add(registroLayout);

        // Manejar error de login
        loginForm.addAttachListener(event -> {
            if (event.getUI().getInternals().getActiveViewLocation()
                    .getQueryParameters().getParameters().containsKey("error")) {
                loginForm.setError(true);
            }
        });
    }
}
