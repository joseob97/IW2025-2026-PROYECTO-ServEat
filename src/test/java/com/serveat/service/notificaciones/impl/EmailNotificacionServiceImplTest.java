package com.serveat.service.notificaciones.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificacionServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificacionServiceImpl service;

    @Test
    void enviarNotificacion_envia_email_con_datos_correctos() {
        String destinatario = "test@correo.com";
        String asunto = "Asunto prueba";
        String mensaje = "Mensaje de prueba";

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        service.enviarNotificacion(destinatario, asunto, mensaje);

        verify(mailSender).send(captor.capture());
        verifyNoMoreInteractions(mailSender);

        SimpleMailMessage enviado = captor.getValue();

        assertThat(enviado.getFrom()).isEqualTo("serveat2526@gmail.com");
        assertThat(enviado.getTo()).containsExactly(destinatario);
        assertThat(enviado.getSubject()).isEqualTo(asunto);
        assertThat(enviado.getText()).isEqualTo(mensaje);
    }

    @Test
    void enviarNotificacion_si_mailSender_lanza_excepcion_no_propagada() {
        String destinatario = "test@correo.com";
        String asunto = "Asunto error";
        String mensaje = "Mensaje error";

        doThrow(new RuntimeException("Error SMTP"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        // No debe lanzar excepción
        service.enviarNotificacion(destinatario, asunto, mensaje);

        verify(mailSender).send(any(SimpleMailMessage.class));
        verifyNoMoreInteractions(mailSender);
    }
}