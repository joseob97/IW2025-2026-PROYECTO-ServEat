package com.serveat.repository.notificaciones;

import com.serveat.domain.notificaciones.PushNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PushNotificacionRepository
        extends JpaRepository<PushNotificacion, Long> {

    List<PushNotificacion> findAllByOrderByCreadaEnDesc();
}
