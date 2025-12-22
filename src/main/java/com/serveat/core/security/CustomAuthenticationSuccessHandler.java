package com.serveat.core.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String redirectUrl = determineTargetUrl(authentication);

        // Redirigir según el rol
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }


    private String determineTargetUrl(Authentication authentication) {

        Collection<? extends GrantedAuthority> authorities =
                authentication.getAuthorities();

        for (GrantedAuthority auth : authorities) {

            String role = auth.getAuthority();

            switch (role) {

                case "ROLE_ADMIN":
                    return "/empleado/admin";

                case "ROLE_CAMARERO":
                    return "/empleado/camarero";

                case "ROLE_COCINERO":
                    return "/empleado/cocinero";

                case "ROLE_REPARTIDOR":
                    return "/empleado/repartidor";

                case "ROLE_CLIENTE":
                    return "/cliente/pedido";  // página principal del cliente
            }
        }

        // Si falla algo volvemos a home
        return "/";
    }
}
