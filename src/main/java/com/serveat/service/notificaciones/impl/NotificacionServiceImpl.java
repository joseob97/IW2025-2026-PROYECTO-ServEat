package com.serveat.service.notificaciones.impl;

import com.serveat.service.notificaciones.NotificacionService;
import org.springframework.stereotype.Service;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    @Override
    public void enviarNotificacion(String destinatario, String asunto, String mensaje) {

        // Email (simulado)
        System.out.println("📧 Email enviado a: " + destinatario);
        System.out.println("Asunto: " + asunto);
        System.out.println(mensaje);

        // Push (placeholder)
        System.out.println("📲 Notificación push enviada");
    }
}
