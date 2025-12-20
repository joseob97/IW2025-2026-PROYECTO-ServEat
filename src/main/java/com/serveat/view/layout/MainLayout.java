package com.serveat.view.layout;

import com.serveat.view.cliente.inicio.InicioClienteView;
import com.serveat.view.cliente.pedido.PanelPedidoClienteView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.html.Image;

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

        Image logoImg = new Image("/images/logo.jpg", "ServEat");
        logoImg.setHeight("64px");
        H1 logoText = new H1("ServEat");
        logoText.getStyle().set("font-size", "24px");
        HorizontalLayout logo = new HorizontalLayout(logoImg, logoText);
        logo.setAlignItems(FlexComponent.Alignment.CENTER);

        RouterLink linkInicio = new RouterLink("Inicio", InicioView.class);
        RouterLink linkPedidos = new RouterLink("Pedidos", PanelPedidoClienteView.class); // Solo si es cliente
        RouterLink linkCarta = new RouterLink("Carta", CartaView.class);
        RouterLink linkContacto = new RouterLink("Contacto", ContactoView.class);
        RouterLink linkInfo = new RouterLink("Información", InformacionSitioView.class);
        RouterLink linkLogin = new RouterLink("Login", LoginView.class);

        Anchor logout = new Anchor("/logout", "Salir");
        logout.getElement().setAttribute("router-ignore", true);

        // DETECTAR SI HAY USUARIO LOGEADO
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isLogged = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        // Texto del usuario conectado
        Span usuarioConectado = new Span();
        usuarioConectado.getStyle().set("margin-right", "20px");
        usuarioConectado.getStyle().set("font-weight", "bold");
        usuarioConectado.getStyle().set("color", "#0366d6");

        // Botón Panel dinámico
        RouterLink linkPanel = null;

        if (isLogged) {
            String username = auth.getName();
            usuarioConectado.setText("Conectado como: " + username);

            linkLogin.setVisible(false); // ocultar login

            // Obtener el rol
            String role = auth.getAuthorities().iterator().next().getAuthority();

            // Inicio personalizado para el cliente
            if ("ROLE_CLIENTE".equals(role)) {
                linkInicio = new RouterLink("Inicio", InicioClienteView.class);
                linkPedidos.setVisible(true);
            }

            // Determinar si es empleado
            boolean esEmpleado = role.startsWith("ROLE_") && !role.equals("ROLE_CLIENTE");

            // Ocultar carta si es empleado
            if (esEmpleado) {
                linkCarta.setVisible(false);
            }

            // Asignar vista del Panel según el rol
            switch (role) {
                case "ROLE_ADMIN":
                    linkPanel = new RouterLink("Panel", com.serveat.view.empleado.administrador.PanelAdminView.class);
                    break;
                case "ROLE_CAMARERO":
                    linkPanel = new RouterLink("Panel", com.serveat.view.empleado.camarero.PanelCamareroView.class);
                    break;
                case "ROLE_COCINERO":
                    linkPanel = new RouterLink("Panel", com.serveat.view.empleado.cocinero.PanelCocineroView.class);
                    break;
                case "ROLE_REPARTIDOR":
                    linkPanel = new RouterLink("Panel", com.serveat.view.empleado.repartidor.PanelRepartidorView.class);
                    break;
            }

        } else {
            usuarioConectado.setVisible(false);
            logout.setVisible(false);
        }

        // MONTAR EL HEADER
        Span spacer = new Span();
        spacer.getStyle().set("flex-grow", "0.9");
        HorizontalLayout header;

        if (isLogged && linkPanel != null) {
            header = new HorizontalLayout(
                    logo,
                    spacer,
                    usuarioConectado,
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
                    usuarioConectado,
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
        header.getStyle().set("flex-wrap", "nowrap");
        header.getStyle().set("white-space", "nowrap");
        header.getStyle().set("gap", "10px");

        addToNavbar(header);
    }
}
