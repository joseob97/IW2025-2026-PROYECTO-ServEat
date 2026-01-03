package com.serveat.core.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log =
            LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String ip = request.getRemoteAddr();
        String sessionId = (request.getSession(false) != null)
                ? request.getSession(false).getId()
                : "no-session";

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
            log.warn(
                    "Intento de login con usuario DESHABILITADO. sessionId={}, ip={}",
                    sessionId,
                    ip
            );

            getRedirectStrategy().sendRedirect(request, response, "/login?disabled");
        } else {
            log.warn(
                    "Fallo de autenticación. sessionId={}, ip={}, motivo={}",
                    sessionId,
                    ip,
                    exception.getClass().getSimpleName()
            );

            getRedirectStrategy().sendRedirect(request, response, "/login?error");
        }
    }
}