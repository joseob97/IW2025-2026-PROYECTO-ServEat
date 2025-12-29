package com.serveat.service.notificaciones.impl;

import com.serveat.service.notificaciones.EmailNotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificacionServiceImpl implements EmailNotificacionService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailNotificacionServiceImpl.class);

    private final JavaMailSender mailSender;

    public EmailNotificacionServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarNotificacion(String destinatario, String asunto, String mensaje) {

        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom("serveat2526@gmail.com"); // REMITENTE
            email.setTo(destinatario);              // DESTINATARIO
            email.setSubject(asunto);
            email.setText(mensaje);

            mailSender.send(email);

            log.info("📧 [EMAIL REAL] Enviado correctamente a {}", destinatario);

        } catch (Exception e) {
            log.error("Error enviando email", e);
        }
    }
}



