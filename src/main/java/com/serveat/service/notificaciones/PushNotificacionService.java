package com.serveat.service.notificaciones;

import com.serveat.domain.notificaciones.PushNotificacion;
import java.util.List;

public interface PushNotificacionService {

    void enviarNotificacion(String titulo, String mensaje);

    void crearNotificacion(String titulo, String mensaje);

    List<PushNotificacion> listarNotificaciones();

    void eliminarNotificacion(Long id);
}
