package com.serveat.service.notificaciones;

import com.serveat.service.administrador.estadisticas.EstadisticasSnapshot;
import org.springframework.stereotype.Service;

@Service
public class NotificacionGananciasService {

    private final EmailNotificacionService emailNotificacionService;
    private final PushNotificacionService pushNotificacionService;

    public NotificacionGananciasService(
            EmailNotificacionService emailNotificacionService,
            PushNotificacionService pushNotificacionService
    ) {
        this.emailNotificacionService = emailNotificacionService;
        this.pushNotificacionService = pushNotificacionService;
    }

    public void notificarGanancias(EstadisticasSnapshot snapshot) {

        String titulo = "Ganancias del mes";

        String mensaje = """
                Ganancias del mes en curso

                Periodo: %s → %s
                Facturación acumulada: %s €

                ServEat
                """.formatted(
                snapshot.getDesde(),
                snapshot.getHasta(),
                snapshot.getTotalFacturado()
        );

        // PUSH (panel admin)
        pushNotificacionService.crearNotificacion(
                titulo,
                mensaje
        );

        // EMAIL
        emailNotificacionService.enviarNotificacion(
                "admin@serveat.com",
                "Preaviso fin de mes - Ganancias",
                mensaje
        );
    }
}
