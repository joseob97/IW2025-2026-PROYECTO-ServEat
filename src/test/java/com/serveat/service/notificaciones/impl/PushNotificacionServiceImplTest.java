package com.serveat.service.notificaciones.impl;

import com.serveat.domain.notificaciones.PushNotificacion;
import com.serveat.repository.notificaciones.PushNotificacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificacionServiceImplTest {

    @Mock
    private PushNotificacionRepository repository;

    @InjectMocks
    private PushNotificacionServiceImpl service;

    @Test
    void enviarNotificacion_reutiliza_crearNotificacion_y_guarda_en_repo() {
        String titulo = "Título";
        String mensaje = "Mensaje";

        ArgumentCaptor<PushNotificacion> captor =
                ArgumentCaptor.forClass(PushNotificacion.class);

        service.enviarNotificacion(titulo, mensaje);

        verify(repository).save(captor.capture());
        verifyNoMoreInteractions(repository);

        PushNotificacion guardada = captor.getValue();

        assertThat(guardada.getTitulo()).isEqualTo(titulo);
        assertThat(guardada.getMensaje()).isEqualTo(mensaje);
        assertThat(guardada.getCreadaEn()).isNotNull();
        assertThat(guardada.isLeida()).isFalse();
    }

    @Test
    void crearNotificacion_guarda_nueva_notificacion() {
        String titulo = "Aviso";
        String mensaje = "Contenido";

        ArgumentCaptor<PushNotificacion> captor =
                ArgumentCaptor.forClass(PushNotificacion.class);

        service.crearNotificacion(titulo, mensaje);

        verify(repository).save(captor.capture());
        verifyNoMoreInteractions(repository);

        PushNotificacion guardada = captor.getValue();

        assertThat(guardada.getTitulo()).isEqualTo(titulo);
        assertThat(guardada.getMensaje()).isEqualTo(mensaje);
        assertThat(guardada.getCreadaEn()).isNotNull();
        assertThat(guardada.isLeida()).isFalse();
    }

    @Test
    void listarNotificaciones_devuelve_lo_que_devuelve_el_repo() {
        PushNotificacion n1 = new PushNotificacion("T1", "M1");
        PushNotificacion n2 = new PushNotificacion("T2", "M2");

        when(repository.findAllByOrderByCreadaEnDesc())
                .thenReturn(List.of(n1, n2));

        List<PushNotificacion> res = service.listarNotificaciones();

        assertThat(res).containsExactly(n1, n2);

        verify(repository).findAllByOrderByCreadaEnDesc();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void eliminarNotificacion_elimina_por_id() {
        Long id = 10L;

        service.eliminarNotificacion(id);

        verify(repository).deleteById(id);
        verifyNoMoreInteractions(repository);
    }
}