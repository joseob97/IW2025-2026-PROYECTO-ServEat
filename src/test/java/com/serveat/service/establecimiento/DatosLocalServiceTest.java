package com.serveat.service.establecimiento;

import com.serveat.domain.establecimiento.DatosLocal;
import com.serveat.repository.establecimiento.DatosLocalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatosLocalServiceTest {

    @Mock
    private DatosLocalRepository repository;

    @InjectMocks
    private DatosLocalService service;

    @Test
    void obtenerDatos_si_repo_tiene_un_registro_devuelve_el_primero_y_no_crea_por_defecto() {
        DatosLocal existente = new DatosLocal();
        existente.setNombreLocal("Mi Local");

        when(repository.findAll()).thenReturn(List.of(existente));

        DatosLocal res = service.obtenerDatos();

        assertThat(res).isSameAs(existente);

        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void obtenerDatos_si_repo_devuelve_vacio_crea_por_defecto_y_lo_guarda() {
        when(repository.findAll()).thenReturn(List.of());

        when(repository.save(any(DatosLocal.class))).thenAnswer(inv -> inv.getArgument(0));

        DatosLocal res = service.obtenerDatos();

        assertThat(res).isNotNull();
        assertThat(res.getNombreLocal()).isEqualTo("ServEat");
        assertThat(res.getDescripcion()).isNotBlank();
        assertThat(res.getDescripcion2()).isNotBlank();
        assertThat(res.getHorario()).isNotBlank();
        assertThat(res.getTelefono()).isNotBlank();
        assertThat(res.getEmail()).isNotBlank();
        assertThat(res.getDireccion()).isNotBlank();

        verify(repository).findAll();
        verify(repository).save(any(DatosLocal.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void obtenerDatos_si_repo_devuelve_lista_con_null_lo_ignora_y_crea_por_defecto() {
        when(repository.findAll()).thenReturn(Collections.singletonList(null));

        when(repository.save(any(DatosLocal.class))).thenAnswer(inv -> inv.getArgument(0));

        DatosLocal res = service.obtenerDatos();

        assertThat(res).isNotNull();
        assertThat(res.getNombreLocal()).isEqualTo("ServEat");

        verify(repository).findAll();
        verify(repository).save(any(DatosLocal.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void obtenerDatos_si_repo_devuelve_null_y_luego_un_registro_devuelve_el_registro_y_no_crea_por_defecto() {
        DatosLocal existente = new DatosLocal();
        existente.setNombreLocal("Local Real");

        when(repository.findAll()).thenReturn(Arrays.asList(null, existente));

        DatosLocal res = service.obtenerDatos();

        assertThat(res).isSameAs(existente);

        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void guardar_guarda_y_devuelve_lo_que_devuelve_el_repo() {
        DatosLocal input = new DatosLocal();
        input.setNombreLocal("Nuevo Local");

        DatosLocal saved = new DatosLocal();
        saved.setNombreLocal("Nuevo Local Guardado");

        when(repository.save(input)).thenReturn(saved);

        DatosLocal res = service.guardar(input);

        assertThat(res).isSameAs(saved);

        verify(repository).save(input);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void obtenerDatos_por_defecto_setea_campos_esperados() {
        when(repository.findAll()).thenReturn(List.of());

        when(repository.save(any(DatosLocal.class))).thenAnswer(inv -> inv.getArgument(0));

        service.obtenerDatos();

        ArgumentCaptor<DatosLocal> captor = ArgumentCaptor.forClass(DatosLocal.class);
        verify(repository).save(captor.capture());

        DatosLocal creado = captor.getValue();
        assertThat(creado.getNombreLocal()).isEqualTo("ServEat");
        assertThat(creado.getDescripcion()).contains("ServEat");
        assertThat(creado.getDescripcion2()).isNotBlank();
        assertThat(creado.getHorario()).contains("13:00");
        assertThat(creado.getTelefono()).isEqualTo("123456789");
        assertThat(creado.getEmail()).isEqualTo("contacto@serveat.com");
        assertThat(creado.getDireccion()).contains("Calle");

        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }
}