package com.serveat.repository.notificaciones;

import com.serveat.domain.notificaciones.NotificacionAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionAdminRepository extends JpaRepository<NotificacionAdmin, Long> {

    List<NotificacionAdmin> findAllByOrderByCreadaEnDesc();
}
