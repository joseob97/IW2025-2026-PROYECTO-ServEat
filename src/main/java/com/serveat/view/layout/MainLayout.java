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

import com.serveat.view.publico.inicio.InicioView;
import com.serveat.view.publico.inicio.LoginView;
import com.serveat.view.publico.carta.CartaView;
import com.serveat.view.publico.contacto.ContactoView;
import com.serveat.view.publico.informacion.InformacionSitioView;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

        /* ================= LINKS ================= */
        RouterLink linkInicio = new RouterLink("Inicio", InicioView.class);
        RouterLink linkPedidos = new RouterLink("Pedidos", PanelPedidoClienteView.class);
        RouterLink linkCarta = new RouterLink("Carta", CartaView.class);
        RouterLink linkContacto = new RouterLink("Contacto", ContactoView.class);
        RouterLink linkInfo = new RouterLink("Información", InformacionSitioView.class);
        RouterLink linkLogin = new RouterLink("Login", LoginView.class);

        Button logout = new Button("Salir", e ->
                UI.getCurrent().getPage().setLocation("/logout")
        );

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
            usuarioConectado.setText("Conectado como: " + username);
            bloqueUsuario.setVisible(true);

            linkLogin.setVisible(false);

            String role = auth.getAuthorities().iterator().next().getAuthority();

            if ("ROLE_CLIENTE".equals(role)) {
                linkInicio = new RouterLink("Inicio", InicioClienteView.class);
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
                            "Panel",
                            com.serveat.view.empleado.administrador.PanelAdminView.class
                    );
                    break;
                case "ROLE_CAMARERO":
                    linkPanel = new RouterLink(
                            "Panel",
                            com.serveat.view.empleado.camarero.PanelCamareroView.class
                    );
                    break;
                case "ROLE_COCINERO":
                    linkPanel = new RouterLink(
                            "Panel",
                            com.serveat.view.empleado.cocinero.PanelCocineroView.class
                    );
                    break;
                case "ROLE_REPARTIDOR":
                    linkPanel = new RouterLink(
                            "Panel",
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

        HorizontalLayout header;

        if (isLogged && linkPanel != null) {
            header = new HorizontalLayout(
                    logo,
                    spacer,
                    bloqueUsuario,
                    linkInicio,
                    linkPanel,
                    linkCarta,
                    linkContacto,
                    linkInfo,
                    logout
            );
        } else {
            header = new HorizontalLayout(
                    logo,
                    spacer,
                    bloqueUsuario,
                    linkInicio,
                    linkPedidos,
                    linkCarta,
                    linkContacto,
                    linkInfo,
                    linkLogin,
                    logout
            );
        }

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.setSpacing(true);
        header.getStyle()
                .set("flex-wrap", "nowrap")
                .set("white-space", "nowrap")
                .set("gap", "10px");

        addToNavbar(header);
    }
}
