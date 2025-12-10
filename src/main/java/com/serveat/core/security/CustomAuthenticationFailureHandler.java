package com.serveat.core.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        // ----------------------------------------------------
        // IMPORTANTE: Spring suele envolver DisabledException
        // dentro de otra AuthenticationException.
        // Recorremos la cadena de causas para detectarlo.
        // ----------------------------------------------------
        boolean disabled = false;
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof DisabledException) {
                disabled = true;
                break;
            }
            cause = cause.getCause();
        }

        if (disabled) {
            // Usuario desactivado -> nuestro mensaje personalizado
            getRedirectStrategy().sendRedirect(request, response, "/login?disabled");
        } else {
            // Cualquier otro error de login -> error genérico
            getRedirectStrategy().sendRedirect(request, response, "/login?error");
        }
    }
}