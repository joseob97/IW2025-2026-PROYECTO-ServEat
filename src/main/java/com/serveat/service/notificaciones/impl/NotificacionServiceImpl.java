package com.serveat.service.notificaciones.impl;

import com.serveat.service.notificaciones.NotificacionService;
import org.springframework.stereotype.Service;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    @Override
    public void enviarNotificacion(String email, String asunto, String mensaje) {

        System.out.println("📧 [NOTIFICACION] Enviando notificación");
        System.out.println("📧 Para: " + email);
        System.out.println("📧 Asunto: " + asunto);
        System.out.println("📧 Mensaje:");
        System.out.println(mensaje);
    }
}

