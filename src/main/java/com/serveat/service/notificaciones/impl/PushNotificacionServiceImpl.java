package com.serveat.service.notificaciones.impl;

import com.serveat.domain.notificaciones.PushNotificacion;
import com.serveat.repository.notificaciones.PushNotificacionRepository;
import com.serveat.service.notificaciones.PushNotificacionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PushNotificacionServiceImpl
        implements PushNotificacionService {

    private final PushNotificacionRepository repository;

    public PushNotificacionServiceImpl(PushNotificacionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void enviarNotificacion(String titulo, String mensaje) {
        // Reutilizamos la lógica que YA tienes implementada
        crearNotificacion(titulo, mensaje);
    }

    @Override
    public void crearNotificacion(String titulo, String mensaje) {
        repository.save(new PushNotificacion(titulo, mensaje));
    }

    @Override
    public List<PushNotificacion> listarNotificaciones() {
        return repository.findAllByOrderByCreadaEnDesc();
    }

    @Override
    public void eliminarNotificacion(Long id) {
        repository.deleteById(id);
    }
}
