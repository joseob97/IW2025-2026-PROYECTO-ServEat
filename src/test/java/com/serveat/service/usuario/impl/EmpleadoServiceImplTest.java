package com.serveat.service.usuario.impl;

import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.usuario.EmpleadoRepository;
import com.serveat.service.usuario.exceptions.DuplicadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpleadoServiceImplTest {

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmpleadoServiceImpl service;

    @Test
    void obtenerTodos_devuelve_findAll() {
        List<Empleado> lista = List.of(
                empleado(1L, "a@a.com", "u1", "p1", "CAMARERO"),
                empleado(2L, "b@b.com", "u2", "p2", "REPARTIDOR")
        );
        when(empleadoRepository.findAll()).thenReturn(lista);

        List<Empleado> res = service.obtenerTodos();

        assertThat(res).isSameAs(lista);
    }

    @Test
    void obtenerPorRol_devuelve_findByRol() {
        List<Empleado> lista = List.of(
                empleado(1L, "a@a.com", "u1", "p1", "COCINERO")
        );
        when(empleadoRepository.findByRol("COCINERO")).thenReturn(lista);

        List<Empleado> res = service.obtenerPorRol("COCINERO");

        assertThat(res).isSameAs(lista);
    }

    @Test
    void obtenerPorId_si_existe_lo_devuelve() {
        Empleado e = empleado(7L, "a@a.com", "u1", "p1", "ADMIN");
        when(empleadoRepository.findById(7L)).thenReturn(Optional.of(e));

        Empleado res = service.obtenerPorId(7L);

        assertThat(res).isSameAs(e);
    }

    @Test
    void obtenerPorId_si_no_existe_lanza_excepcion() {
        when(empleadoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Empleado no encontrado");
    }

    @Test
    void obtenerPorUsername_si_existe_lo_devuelve() {
        Empleado e = empleado(3L, "a@a.com", "u1", "p1", "REPARTIDOR");
        when(empleadoRepository.findByUsername("u1")).thenReturn(Optional.of(e));

        Empleado res = service.obtenerPorUsername("u1");

        assertThat(res).isSameAs(e);
    }

    @Test
    void obtenerPorUsername_si_no_existe_lanza_excepcion() {
        when(empleadoRepository.findByUsername("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorUsername("nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Empleado no encontrado");
    }

    @Test
    void guardar_si_email_usado_por_otro_empleado_lanza_duplicadoException() {
        Empleado nuevo = empleado(2L, "dup@a.com", "u2", "plain", "CAMARERO");
        Empleado existente = empleado(1L, "dup@a.com", "otro", "x", "CAMARERO");

        when(empleadoRepository.findByEmail("dup@a.com")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.guardar(nuevo))
                .isInstanceOf(DuplicadoException.class)
                .hasMessage("El email ya está en uso por otro empleado");

        verify(empleadoRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verify(empleadoRepository, never()).findByUsername(any());
    }

    @Test
    void guardar_si_email_usado_pero_mismo_id_no_falla_y_valida_username() {
        Empleado mismo = empleado(5L, "same@a.com", "u5", "plain", "COCINERO");

        when(empleadoRepository.findByEmail("same@a.com")).thenReturn(Optional.of(mismo));
        when(empleadoRepository.findByUsername("u5")).thenReturn(Optional.of(mismo));

        when(passwordEncoder.encode("plain")).thenReturn("$2a$ENC");
        when(empleadoRepository.save(any(Empleado.class))).thenAnswer(inv -> inv.getArgument(0));

        Empleado res = service.guardar(mismo);

        assertThat(res.getPassword()).isEqualTo("$2a$ENC");
        verify(passwordEncoder).encode("plain");
        verify(empleadoRepository).save(mismo);
    }

    @Test
    void guardar_si_username_usado_por_otro_empleado_lanza_duplicadoException() {
        Empleado nuevo = empleado(2L, "a@a.com", "dupUser", "plain", "REPARTIDOR");
        Empleado existente = empleado(1L, "b@b.com", "dupUser", "x", "REPARTIDOR");

        when(empleadoRepository.findByEmail("a@a.com")).thenReturn(Optional.empty());
        when(empleadoRepository.findByUsername("dupUser")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.guardar(nuevo))
                .isInstanceOf(DuplicadoException.class)
                .hasMessage("El nombre de usuario ya está en uso");

        verify(empleadoRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void guardar_si_password_ya_es_bcrypt_no_reencodea_y_guarda() {
        Empleado e = empleado(1L, "a@a.com", "u1", "$2a$YA_ENC", "ADMIN");

        when(empleadoRepository.findByEmail("a@a.com")).thenReturn(Optional.empty());
        when(empleadoRepository.findByUsername("u1")).thenReturn(Optional.empty());
        when(empleadoRepository.save(any(Empleado.class))).thenAnswer(inv -> inv.getArgument(0));

        Empleado res = service.guardar(e);

        assertThat(res.getPassword()).isEqualTo("$2a$YA_ENC");
        verify(passwordEncoder, never()).encode(any());
        verify(empleadoRepository).save(e);
    }

    @Test
    void guardar_si_password_plano_encodea_y_guarda() {
        Empleado e = empleado(1L, "a@a.com", "u1", "plain", "ADMIN");

        when(empleadoRepository.findByEmail("a@a.com")).thenReturn(Optional.empty());
        when(empleadoRepository.findByUsername("u1")).thenReturn(Optional.empty());

        when(passwordEncoder.encode("plain")).thenReturn("$2a$ENC");
        when(empleadoRepository.save(any(Empleado.class))).thenAnswer(inv -> inv.getArgument(0));

        Empleado res = service.guardar(e);

        assertThat(res.getPassword()).isEqualTo("$2a$ENC");
        verify(passwordEncoder).encode("plain");
        verify(empleadoRepository).save(e);
    }

    @Test
    void activar_marca_enabled_true_y_guarda() {
        Empleado e = empleado(1L, "a@a.com", "u1", "p1", "CAMARERO");
        e.setEnabled(false);

        service.activar(e);

        assertThat(e.isEnabled()).isTrue();
        verify(empleadoRepository).save(e);
    }

    @Test
    void desactivar_marca_enabled_false_y_guarda() {
        Empleado e = empleado(1L, "a@a.com", "u1", "p1", "CAMARERO");
        e.setEnabled(true);

        service.desactivar(e);

        assertThat(e.isEnabled()).isFalse();
        verify(empleadoRepository).save(e);
    }

    @Test
    void eliminar_llama_delete() {
        Empleado e = empleado(1L, "a@a.com", "u1", "p1", "CAMARERO");

        service.eliminar(e);

        verify(empleadoRepository).delete(e);
    }

    private static Empleado empleado(Long id, String email, String username, String password, String rol) {
        Empleado e = new Empleado();
        // Empleado no tiene setter para id; lo fijamos por reflexión.
        setId(e, id);
        e.setEmail(email);
        e.setUsername(username);
        e.setPassword(password);
        e.setRol(rol);
        e.setNombre("Nombre");
        e.setTelefono("600000000");
        e.setDireccion("Calle 1");
        e.setEnabled(true);
        return e;
    }

    private static void setId(Empleado e, Long id) {
        try {
            var f = Empleado.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(e, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}