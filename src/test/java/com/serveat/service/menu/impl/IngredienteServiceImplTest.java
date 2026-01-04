package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.repository.menu.IngredienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredienteServiceImplTest {

    @Mock
    private IngredienteRepository ingredienteRepo;

    @InjectMocks
    private IngredienteServiceImpl service;

    @Test
    void listar_ordena_por_nombre_ignorando_mayusculas_y_nulos() {
        Ingrediente a = new Ingrediente();
        a.setNombre("Zanahoria");

        Ingrediente b = new Ingrediente();
        b.setNombre("aceite");

        Ingrediente c = new Ingrediente();
        c.setNombre(null);

        when(ingredienteRepo.findAll()).thenReturn(List.of(a, b, c));

        List<Ingrediente> res = service.listar();

        assertThat(res).containsExactly(c, b, a);

        verify(ingredienteRepo).findAll();
        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void buscarPorNombre_si_query_es_null_devuelve_listar() {
        Ingrediente i1 = new Ingrediente();
        i1.setNombre("A");

        when(ingredienteRepo.findAll()).thenReturn(List.of(i1));

        List<Ingrediente> res = service.buscarPorNombre(null);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getNombre()).isEqualTo("A");

        verify(ingredienteRepo).findAll();
        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void buscarPorNombre_si_query_es_blank_devuelve_listar() {
        Ingrediente i1 = new Ingrediente();
        i1.setNombre("A");

        when(ingredienteRepo.findAll()).thenReturn(List.of(i1));

        List<Ingrediente> res = service.buscarPorNombre("   ");

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getNombre()).isEqualTo("A");

        verify(ingredienteRepo).findAll();
        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void buscarPorNombre_si_query_tiene_texto_llama_repo_especifico() {
        Ingrediente i1 = new Ingrediente();
        i1.setNombre("Queso");

        when(ingredienteRepo.findByNombreContainingIgnoreCaseOrderByNombreAsc("que"))
                .thenReturn(List.of(i1));

        List<Ingrediente> res = service.buscarPorNombre("  que  ");

        assertThat(res).containsExactly(i1);

        verify(ingredienteRepo).findByNombreContainingIgnoreCaseOrderByNombreAsc("que");
        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void crear_si_nombre_null_lanza_error() {
        assertThatThrownBy(() -> service.crear(null, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(ingredienteRepo);
    }

    @Test
    void crear_si_nombre_blank_lanza_error() {
        assertThatThrownBy(() -> service.crear("   ", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(ingredienteRepo);
    }

    @Test
    void crear_si_nombre_supera_60_lanza_error() {
        String nombre = "a".repeat(61);

        assertThatThrownBy(() -> service.crear(nombre, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nombre demasiado largo (máx 60)");

        verifyNoInteractions(ingredienteRepo);
    }

    @Test
    void crear_si_ya_existe_nombre_lanza_error() {
        Ingrediente existente = new Ingrediente();
        existente.setNombre("Queso");

        when(ingredienteRepo.findByNombre("Queso")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.crear("  Queso  ", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un ingrediente con ese nombre");

        verify(ingredienteRepo).findByNombre("Queso");
        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void crear_si_no_existe_crea_trim_y_precio_null_a_cero_y_guarda() {
        when(ingredienteRepo.findByNombre("Queso")).thenReturn(Optional.empty());
        when(ingredienteRepo.save(any(Ingrediente.class))).thenAnswer(inv -> inv.getArgument(0));

        Ingrediente res = service.crear("  Queso  ", null);

        assertThat(res.getNombre()).isEqualTo("Queso");
        assertThat(res.getPrecioExtra()).isEqualByComparingTo(BigDecimal.ZERO);

        ArgumentCaptor<Ingrediente> captor = ArgumentCaptor.forClass(Ingrediente.class);
        verify(ingredienteRepo).findByNombre("Queso");
        verify(ingredienteRepo).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Queso");
        assertThat(captor.getValue().getPrecioExtra()).isEqualByComparingTo(BigDecimal.ZERO);

        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void crear_si_precio_no_null_lo_persiste() {
        when(ingredienteRepo.findByNombre("Bacon")).thenReturn(Optional.empty());
        when(ingredienteRepo.save(any(Ingrediente.class))).thenAnswer(inv -> inv.getArgument(0));

        Ingrediente res = service.crear("Bacon", new BigDecimal("1.25"));

        assertThat(res.getNombre()).isEqualTo("Bacon");
        assertThat(res.getPrecioExtra()).isEqualByComparingTo("1.25");

        verify(ingredienteRepo).findByNombre("Bacon");
        verify(ingredienteRepo).save(any(Ingrediente.class));
        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void actualizar_si_id_null_lanza_error() {
        assertThatThrownBy(() -> service.actualizar(null, "Queso", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Id inválido");

        verifyNoInteractions(ingredienteRepo);
    }

    @Test
    void actualizar_si_nombre_invalido_lanza_error() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.actualizar(id, "   ", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(ingredienteRepo);
    }

    @Test
    void actualizar_si_no_existe_lanza_error() {
        UUID id = UUID.randomUUID();

        when(ingredienteRepo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(id, "Queso", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ingrediente no encontrado");

        verify(ingredienteRepo).findById(id);
        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void actualizar_si_existe_otro_con_mismo_nombre_y_distinto_id_lanza_error() {
        UUID idActual = UUID.randomUUID();
        UUID idOtro = UUID.randomUUID();

        Ingrediente actual = new Ingrediente();
        actual.setNombre("Viejo");
        setIdReflectivo(actual, idActual);

        Ingrediente otro = new Ingrediente();
        otro.setNombre("Queso");
        setIdReflectivo(otro, idOtro);

        when(ingredienteRepo.findById(idActual)).thenReturn(Optional.of(actual));
        when(ingredienteRepo.findByNombre("Queso")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> service.actualizar(idActual, "  Queso  ", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un ingrediente con ese nombre");

        verify(ingredienteRepo).findById(idActual);
        verify(ingredienteRepo).findByNombre("Queso");
        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void actualizar_si_findByNombre_devuelve_el_mismo_registro_permite_actualizar_y_guarda() {
        UUID id = UUID.randomUUID();

        Ingrediente actual = new Ingrediente();
        actual.setNombre("Queso");
        setIdReflectivo(actual, id);

        when(ingredienteRepo.findById(id)).thenReturn(Optional.of(actual));
        when(ingredienteRepo.findByNombre("Queso")).thenReturn(Optional.of(actual));
        when(ingredienteRepo.save(any(Ingrediente.class))).thenAnswer(inv -> inv.getArgument(0));

        Ingrediente res = service.actualizar(id, "  Queso  ", new BigDecimal("0.50"));

        assertThat(res.getNombre()).isEqualTo("Queso");
        assertThat(res.getPrecioExtra()).isEqualByComparingTo("0.50");

        verify(ingredienteRepo).findById(id);
        verify(ingredienteRepo).findByNombre("Queso");
        verify(ingredienteRepo).save(actual);
        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void actualizar_si_no_hay_colision_actualiza_trim_precio_null_a_cero_y_guarda() {
        UUID id = UUID.randomUUID();

        Ingrediente actual = new Ingrediente();
        actual.setNombre("Viejo");
        setIdReflectivo(actual, id);

        when(ingredienteRepo.findById(id)).thenReturn(Optional.of(actual));
        when(ingredienteRepo.findByNombre("Nuevo")).thenReturn(Optional.empty());
        when(ingredienteRepo.save(any(Ingrediente.class))).thenAnswer(inv -> inv.getArgument(0));

        Ingrediente res = service.actualizar(id, "  Nuevo  ", null);

        assertThat(res.getNombre()).isEqualTo("Nuevo");
        assertThat(res.getPrecioExtra()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(ingredienteRepo).findById(id);
        verify(ingredienteRepo).findByNombre("Nuevo");
        verify(ingredienteRepo).save(actual);
        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void eliminar_si_id_null_lanza_error() {
        assertThatThrownBy(() -> service.eliminar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Id inválido");

        verifyNoInteractions(ingredienteRepo);
    }

    @Test
    void eliminar_si_no_existe_lanza_error() {
        UUID id = UUID.randomUUID();
        when(ingredienteRepo.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.eliminar(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ingrediente no encontrado");

        verify(ingredienteRepo).existsById(id);
        verifyNoMoreInteractions(ingredienteRepo);
    }

    @Test
    void eliminar_si_existe_borra_por_id() {
        UUID id = UUID.randomUUID();
        when(ingredienteRepo.existsById(id)).thenReturn(true);

        service.eliminar(id);

        verify(ingredienteRepo).existsById(id);
        verify(ingredienteRepo).deleteById(id);
        verifyNoMoreInteractions(ingredienteRepo);
    }

    // Asigna el UUID al campo privado id mediante reflexión para poder probar colisiones de nombre.
    private static void setIdReflectivo(Ingrediente ingrediente, UUID id) {
        try {
            var f = Ingrediente.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(ingrediente, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}