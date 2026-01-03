package com.serveat.view.layout;

import com.serveat.view.cliente.inicio.InicioClienteView;
import com.serveat.view.cliente.pedido.PanelPedidoClienteView;
import com.serveat.view.perfil.PerfilView;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.combobox.ComboBox;

import com.serveat.view.publico.inicio.InicioView;
import com.serveat.view.publico.inicio.LoginView;
import com.serveat.view.publico.carta.CartaView;
import com.serveat.view.publico.contacto.ContactoView;
import com.serveat.view.publico.informacion.InformacionSitioView;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Locale;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
    }

    private void createHeader() {

        /* ================= LOGO ================= */
        Image logoImg = new Image("/images/logo.jpg", "ServEat");
        logoImg.setHeight("64px");

        H1 logoText = new H1("ServEat");
        logoText.getStyle().set("font-size", "24px");

        HorizontalLayout logo = new HorizontalLayout(logoImg, logoText);
        logo.setAlignItems(FlexComponent.Alignment.CENTER);

        /* ================= SELECTOR IDIOMA ================= */
        ComboBox<Locale> selectorIdioma = new ComboBox<>();
        selectorIdioma.setItems(
                new Locale("es", "ES"),
                Locale.ENGLISH
        );

        selectorIdioma.setItemLabelGenerator(locale ->
                locale.getLanguage().equals("es") ? "ES" : "EN"
        );

        selectorIdioma.setValue(UI.getCurrent().getLocale());

        selectorIdioma.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().setLocale(event.getValue());
                UI.getCurrent().getPage().reload();
            }
        });

        selectorIdioma.setWidth("80px");

        /* ================= LINKS ================= */
        RouterLink linkInicio = new RouterLink(getTranslation("nav.inicio"), InicioView.class);
        RouterLink linkPedidos = new RouterLink(getTranslation("nav.pedidos"), PanelPedidoClienteView.class);
        RouterLink linkCarta = new RouterLink(getTranslation("nav.carta"), CartaView.class);
        RouterLink linkContacto = new RouterLink(getTranslation("nav.contacto"), ContactoView.class);
        RouterLink linkInfo = new RouterLink(getTranslation("nav.informacion"), InformacionSitioView.class);
        RouterLink linkLogin = new RouterLink(getTranslation("nav.login"), LoginView.class);

        /* ================= LOGOUT CON CONFIRMACIÓN ================= */
        Button logout = new Button(getTranslation("nav.logout"), e -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader(getTranslation("logout.titulo"));
            dialog.setText(getTranslation("logout.mensaje"));
            dialog.setCancelable(true);
            dialog.setConfirmText(getTranslation("logout.confirmar"));
            dialog.setCancelText(getTranslation("logout.cancelar"));

            dialog.addConfirmListener(ev ->
                    UI.getCurrent().getPage().setLocation("/logout")
            );

            dialog.open();
        });

        /* ================= AUTH ================= */
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isLogged = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        /* ================= USUARIO + ICONO ================= */
        Span usuarioConectado = new Span();
        usuarioConectado.getStyle()
                .set("font-weight", "bold")
                .set("color", "#0366d6");

        Icon userIcon = new Icon(VaadinIcon.USER);
        userIcon.getStyle()
                .set("margin-left", "6px")
                .set("cursor", "pointer");

        RouterLink perfilLink = new RouterLink();
        perfilLink.setRoute(PerfilView.class);
        perfilLink.add(userIcon);

        HorizontalLayout bloqueUsuario =
                new HorizontalLayout(usuarioConectado, perfilLink);
        bloqueUsuario.setAlignItems(FlexComponent.Alignment.CENTER);
        bloqueUsuario.setSpacing(false);

        /* ================= PANEL POR ROL ================= */
        RouterLink linkPanel = null;

        if (isLogged) {

            String username = auth.getName();
            usuarioConectado.setText(getTranslation("nav.conectado") + " " + username);
            bloqueUsuario.setVisible(true);

            linkLogin.setVisible(false);

            String role = auth.getAuthorities().iterator().next().getAuthority();

            if ("ROLE_CLIENTE".equals(role)) {
                linkInicio = new RouterLink(getTranslation("nav.inicio"), InicioClienteView.class);
                linkPedidos.setVisible(true);
            }

            boolean esEmpleado = role.startsWith("ROLE_")
                    && !role.equals("ROLE_CLIENTE");

            if (esEmpleado) {
                linkCarta.setVisible(false);
            }

            switch (role) {
                case "ROLE_ADMIN":
                    linkPanel = new RouterLink(
                            getTranslation("nav.panel"),
                            com.serveat.view.empleado.administrador.PanelAdminView.class
                    );
                    break;
                case "ROLE_CAMARERO":
                    linkPanel = new RouterLink(
                            getTranslation("nav.panel"),
                            com.serveat.view.empleado.camarero.PanelCamareroView.class
                    );
                    break;
                case "ROLE_COCINERO":
                    linkPanel = new RouterLink(
                            getTranslation("nav.panel"),
                            com.serveat.view.empleado.cocinero.PanelCocineroView.class
                    );
                    break;
                case "ROLE_REPARTIDOR":
                    linkPanel = new RouterLink(
                            getTranslation("nav.panel"),
                            com.serveat.view.empleado.repartidor.PanelRepartidorView.class
                    );
                    break;
            }

        } else {
            bloqueUsuario.setVisible(false);
            logout.setVisible(false);
            linkPedidos.setVisible(false);
        }

        /* ================= HEADER ================= */
        Span spacer = new Span();
        spacer.getStyle().set("flex-grow", "0.9");

        HorizontalLayout header = new HorizontalLayout(
                logo,
                spacer,
                selectorIdioma,
                bloqueUsuario,
                linkInicio,
                linkCarta,
                linkContacto,
                linkInfo,
                linkLogin,
                logout
        );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.setSpacing(true);
        header.getStyle()
                .set("flex-wrap", "nowrap")
                .set("white-space", "nowrap")
                .set("gap", "10px");

        if (linkPanel != null) {
            header.addComponentAtIndex(4, linkPanel);
        }

        addToNavbar(header);
    }
}
