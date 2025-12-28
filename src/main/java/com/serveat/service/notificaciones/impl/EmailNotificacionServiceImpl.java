package com.serveat.service.notificaciones.impl;

import com.serveat.service.notificaciones.EmailNotificacionService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class EmailNotificacionServiceImpl implements EmailNotificacionService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailNotificacionServiceImpl.class);

    @Override
    public void enviarNotificacion(String destinatario, String asunto, String mensaje) {

        log.info("📧 [EMAIL] Enviando email");
        log.info("📧 Destinatario: {}", destinatario);
        log.info("📧 Asunto: {}", asunto);
        log.info("📧 Mensaje:\n{}", mensaje);
    }
}


