package com.serveat.view.publico.inicio;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = "login", layout = MainLayout.class)
@PageTitle("Login | ServEat")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(LoginView.class);

    private final LoginForm loginForm = new LoginForm();
    private final Span errorMessage = new Span();

    public LoginView() {

        log.info("Acceso a la vista de login");

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout contenedor = new VerticalLayout();
        contenedor.setWidth("400px");
        contenedor.setPadding(true);
        contenedor.setSpacing(true);
        contenedor.setAlignItems(FlexComponent.Alignment.STRETCH);

        contenedor.getStyle()
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", "8px")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.05)");

        H1 title = new H1(getTranslation("login.titulo"));
        title.getStyle().set("text-align", "center");

        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        // ✅ I18N CORRECTO PARA LOGINFORM
        LoginI18n i18n = LoginI18n.createDefault();

        i18n.getForm().setTitle("");
        i18n.getForm().setUsername(getTranslation("login.usuario"));
        i18n.getForm().setPassword(getTranslation("login.password"));
        i18n.getForm().setSubmit(getTranslation("login.boton"));

        i18n.getErrorMessage().setTitle(getTranslation("login.error.titulo"));
        i18n.getErrorMessage().setMessage(getTranslation("login.error.mensaje"));

        loginForm.setI18n(i18n);

        errorMessage.getStyle().set("color", "red");
        errorMessage.getStyle().set("font-weight", "bold");
        errorMessage.getStyle().set("margin-bottom", "10px");
        errorMessage.setVisible(false);

        Span noCuenta = new Span(getTranslation("login.noCuenta"));
        RouterLink registrate =
                new RouterLink(getTranslation("login.registrate"), RegistroClienteView.class);

        registrate.getStyle()
                .set("color", "#0366d6")
                .set("font-weight", "bold");

        HorizontalLayout registroLayout =
                new HorizontalLayout(noCuenta, registrate);

        registroLayout.setSpacing(false);
        registroLayout.setJustifyContentMode(
                FlexComponent.JustifyContentMode.CENTER
        );

        contenedor.add(title, errorMessage, loginForm, registroLayout);
        add(contenedor);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        errorMessage.setVisible(false);
        loginForm.setError(false);

        QueryParameters params = event.getLocation().getQueryParameters();

        if (params.getParameters().containsKey("disabled")) {

            log.warn("Intento de inicio de sesión con usuario desactivado");

            errorMessage.setText(
                    getTranslation("login.usuarioDesactivado")
            );
            errorMessage.setVisible(true);

        } else if (params.getParameters().containsKey("error")) {

            log.warn("Error de autenticación en intento de login");
            loginForm.setError(true);
        }
    }
}
