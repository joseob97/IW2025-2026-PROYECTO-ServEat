package com.serveat.service.notificaciones;

public interface NotificacionService {

    void enviarNotificacion(String destinatario, String asunto, String mensaje);
}
