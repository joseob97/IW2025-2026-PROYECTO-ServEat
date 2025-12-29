package com.serveat.service.notificaciones;

public interface EmailNotificacionService {

    void enviarNotificacion(String destinatario, String asunto, String mensaje);
}
