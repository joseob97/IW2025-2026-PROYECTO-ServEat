package com.serveat.security;

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
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String redirectUrl = determineTargetUrl(authentication);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String determineTargetUrl(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        String defaultUrl = "/";

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();

            switch (role) {
                case "ROLE_CAMARERO":
                    return "/empleado/camarero";   // panel camarero
                case "ROLE_COCINERO":
                    return "/empleado/cocinero";   // panel cocinero
                case "ROLE_REPARTIDOR":
                    return "/empleado/repartidor"; // panel repartidor
                case "ROLE_ADMIN":
                    return "/empleado/admin";       // panel admin
                case "ROLE_CLIENTE":
                    return "/carta";        // panel de cliente
            }
        }
        return defaultUrl;
    }
}
