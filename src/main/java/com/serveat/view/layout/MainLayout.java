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
import com.vaadin.flow.server.VaadinSession;

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
        logoImg.setHeight("56px");

        H1 logoText = new H1("ServEat");
        logoText.getStyle().set("font-size", "22px");

        HorizontalLayout logo = new HorizontalLayout(logoImg, logoText);
        logo.setAlignItems(FlexComponent.Alignment.CENTER);
        logo.setSpacing(true);

        /* ================= IDIOMA ================= */
        ComboBox<Locale> selectorIdioma = new ComboBox<>();
        selectorIdioma.setItems(new Locale("es", "ES"), Locale.ENGLISH);
        selectorIdioma.setItemLabelGenerator(l ->
                l.getLanguage().equals("es") ? "ES" : "EN"
        );

        Locale localeActual = VaadinSession.getCurrent().getLocale();
        selectorIdioma.setValue(localeActual != null ? localeActual : new Locale("es", "ES"));
        selectorIdioma.setWidth("80px");


        selectorIdioma.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                VaadinSession.getCurrent().setLocale(e.getValue());

                String rutaActual = UI.getCurrent()
                        .getInternals()
                        .getActiveViewLocation()
                        .getPathWithQueryParameters();

                UI.getCurrent().navigate(rutaActual);
            }
        });

        /* ================= LINKS ================= */
        RouterLink linkInicio = new RouterLink(getTranslation("nav.inicio"), InicioView.class);
        RouterLink linkPedidos = new RouterLink(getTranslation("nav.pedidos"), PanelPedidoClienteView.class);
        RouterLink linkCarta = new RouterLink(getTranslation("nav.carta"), CartaView.class);
        RouterLink linkContacto = new RouterLink(getTranslation("nav.contacto"), ContactoView.class);
        RouterLink linkInfo = new RouterLink(getTranslation("nav.informacion"), InformacionSitioView.class);
        RouterLink linkLogin = new RouterLink(getTranslation("nav.login"), LoginView.class);

        /* ================= LOGOUT ================= */
        Button logout = new Button(getTranslation("nav.logout"), e -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader(getTranslation("logout.titulo"));
            dialog.setText(getTranslation("logout.mensaje"));
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

        /* ================= USUARIO ================= */
        Span usuario = new Span();
        Icon userIcon = new Icon(VaadinIcon.USER);
        RouterLink perfil = new RouterLink("", PerfilView.class);
        perfil.add(userIcon);

        HorizontalLayout bloqueUsuario = new HorizontalLayout(usuario, perfil);
        bloqueUsuario.setAlignItems(FlexComponent.Alignment.CENTER);
        bloqueUsuario.setSpacing(true);

        /* ================= PANEL ================= */
        HorizontalLayout panelContainer = new HorizontalLayout();
        panelContainer.setAlignItems(FlexComponent.Alignment.CENTER);

        if (isLogged) {

            usuario.setText(getTranslation("nav.conectado") + " " + auth.getName());
            linkLogin.setVisible(false);

            String role = auth.getAuthorities().iterator().next().getAuthority();

            if ("ROLE_CLIENTE".equals(role)) {
                linkInicio = new RouterLink(getTranslation("nav.inicio"), InicioClienteView.class);
                linkPedidos.setVisible(true);
            }

            RouterLink panel = null;

            switch (role) {
                case "ROLE_ADMIN" ->
                        panel = new RouterLink(getTranslation("nav.panel"),
                                com.serveat.view.empleado.administrador.PanelAdminView.class);
                case "ROLE_CAMARERO" ->
                        panel = new RouterLink(getTranslation("nav.panel"),
                                com.serveat.view.empleado.camarero.PanelCamareroView.class);
                case "ROLE_COCINERO" ->
                        panel = new RouterLink(getTranslation("nav.panel"),
                                com.serveat.view.empleado.cocinero.PanelCocineroView.class);
                case "ROLE_REPARTIDOR" ->
                        panel = new RouterLink(getTranslation("nav.panel"),
                                com.serveat.view.empleado.repartidor.PanelRepartidorView.class);
            }

            if (panel != null) {
                panelContainer.add(panel);
            }

        } else {
            bloqueUsuario.setVisible(false);
            logout.setVisible(false);
            linkPedidos.setVisible(false);
        }

        /* ================= HEADER ================= */
        Span spacer = new Span();
        spacer.getStyle().set("flex-grow", "1");

        HorizontalLayout header = new HorizontalLayout(
                logo,
                spacer,
                selectorIdioma,
                panelContainer,
                bloqueUsuario,
                linkInicio,
                linkCarta,
                linkContacto,
                linkInfo,
                linkLogin,
                logout
        );

        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        header.getStyle().set("padding", "0 16px");

        addToNavbar(header);
    }
}
