package com.serveat.service.usuario.impl;

import com.serveat.domain.usuario.Cliente;
import com.serveat.repository.usuario.ClienteRepository;
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
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClienteServiceImpl service;

    @Test
    void obtenerPorUsername_si_existe_lo_devuelve() {
        String username = "u1";
        Cliente c = cliente(1L, "a@a.com", username, "1234");

        when(clienteRepo.findByUsername(username)).thenReturn(Optional.of(c));

        Cliente res = service.obtenerPorUsername(username);

        assertThat(res).isSameAs(c);
    }

    @Test
    void obtenerPorUsername_si_no_existe_lanza_excepcion() {
        when(clienteRepo.findByUsername("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorUsername("nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cliente no encontrado");
    }

    @Test
    void obtenerTodos_devuelve_findAll() {
        List<Cliente> lista = List.of(
                cliente(1L, "a@a.com", "u1", "p1"),
                cliente(2L, "b@b.com", "u2", "p2")
        );

        when(clienteRepo.findAll()).thenReturn(lista);

        List<Cliente> res = service.obtenerTodos();

        assertThat(res).isSameAs(lista);
    }

    @Test
    void obtenerPorId_si_existe_lo_devuelve() {
        Cliente c = cliente(7L, "a@a.com", "u1", "p1");
        when(clienteRepo.findById(7L)).thenReturn(Optional.of(c));

        Cliente res = service.obtenerPorId(7L);

        assertThat(res).isSameAs(c);
    }

    @Test
    void obtenerPorId_si_no_existe_lanza_excepcion() {
        when(clienteRepo.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cliente no encontrado");
    }

    @Test
    void guardar_si_email_duplicado_con_otro_id_lanza_duplicadoException() {
        Cliente nuevo = cliente(2L, "dup@a.com", "u2", "p2");
        Cliente existente = cliente(1L, "dup@a.com", "otro", "p1");

        when(clienteRepo.existsByEmail("dup@a.com")).thenReturn(true);
        when(clienteRepo.findByEmail("dup@a.com")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.guardar(nuevo))
                .isInstanceOf(DuplicadoException.class)
                .hasMessage("El email ya está registrado");

        verify(clienteRepo, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void guardar_si_email_duplicado_pero_mismo_id_no_lanza_y_sigue() {
        Cliente mismo = cliente(5L, "same@a.com", "u5", "plain");

        when(clienteRepo.existsByEmail("same@a.com")).thenReturn(true);
        when(clienteRepo.findByEmail("same@a.com")).thenReturn(Optional.of(mismo));

        when(clienteRepo.existsByUsername("u5")).thenReturn(false);

        when(passwordEncoder.encode("plain")).thenReturn("$2a$ENC");
        when(clienteRepo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente res = service.guardar(mismo);

        assertThat(res.getPassword()).isEqualTo("$2a$ENC");
        verify(clienteRepo).save(mismo);
        verify(passwordEncoder).encode("plain");
    }

    @Test
    void guardar_si_username_duplicado_con_otro_id_lanza_duplicadoException() {
        Cliente nuevo = cliente(2L, "a@a.com", "dupUser", "p2");
        Cliente existente = cliente(1L, "b@b.com", "dupUser", "p1");

        when(clienteRepo.existsByEmail("a@a.com")).thenReturn(false);

        when(clienteRepo.existsByUsername("dupUser")).thenReturn(true);
        when(clienteRepo.findByUsername("dupUser")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.guardar(nuevo))
                .isInstanceOf(DuplicadoException.class)
                .hasMessage("El nombre de usuario ya está en uso");

        verify(clienteRepo, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void guardar_si_username_duplicado_pero_mismo_id_no_lanza_y_sigue() {
        Cliente mismo = cliente(9L, "a@a.com", "u9", "plain");

        when(clienteRepo.existsByEmail("a@a.com")).thenReturn(false);

        when(clienteRepo.existsByUsername("u9")).thenReturn(true);
        when(clienteRepo.findByUsername("u9")).thenReturn(Optional.of(mismo));

        when(passwordEncoder.encode("plain")).thenReturn("$2a$ENC");
        when(clienteRepo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente res = service.guardar(mismo);

        assertThat(res.getPassword()).isEqualTo("$2a$ENC");
        verify(clienteRepo).save(mismo);
        verify(passwordEncoder).encode("plain");
    }

    @Test
    void guardar_si_password_empieza_por_bcrypt_no_reencodea() {
        Cliente c = cliente(1L, "a@a.com", "u1", "$2a$YA_ENC");

        when(clienteRepo.existsByEmail("a@a.com")).thenReturn(false);
        when(clienteRepo.existsByUsername("u1")).thenReturn(false);

        when(clienteRepo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente res = service.guardar(c);

        assertThat(res.getPassword()).isEqualTo("$2a$YA_ENC");
        verify(passwordEncoder, never()).encode(any());
        verify(clienteRepo).save(c);
    }

    @Test
    void guardar_si_password_null_no_encodea_y_guarda() {
        Cliente c = cliente(1L, "a@a.com", "u1", null);

        when(clienteRepo.existsByEmail("a@a.com")).thenReturn(false);
        when(clienteRepo.existsByUsername("u1")).thenReturn(false);

        when(clienteRepo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente res = service.guardar(c);

        assertThat(res.getPassword()).isNull();
        verify(passwordEncoder, never()).encode(any());
        verify(clienteRepo).save(c);
    }

    @Test
    void guardar_si_password_plano_encodea_y_guarda() {
        Cliente c = cliente(1L, "a@a.com", "u1", "plain");

        when(clienteRepo.existsByEmail("a@a.com")).thenReturn(false);
        when(clienteRepo.existsByUsername("u1")).thenReturn(false);

        when(passwordEncoder.encode("plain")).thenReturn("$2a$ENC");
        when(clienteRepo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente res = service.guardar(c);

        assertThat(res.getPassword()).isEqualTo("$2a$ENC");
        verify(passwordEncoder).encode("plain");
        verify(clienteRepo).save(c);
    }

    @Test
    void activar_marca_activo_y_guarda() {
        Cliente c = cliente(1L, "a@a.com", "u1", "p1");
        c.setActivo(false);

        service.activar(c);

        assertThat(c.isActivo()).isTrue();
        verify(clienteRepo).save(c);
    }

    @Test
    void desactivar_marca_inactivo_y_guarda() {
        Cliente c = cliente(1L, "a@a.com", "u1", "p1");
        c.setActivo(true);

        service.desactivar(c);

        assertThat(c.isActivo()).isFalse();
        verify(clienteRepo).save(c);
    }

    @Test
    void eliminar_llama_delete() {
        Cliente c = cliente(1L, "a@a.com", "u1", "p1");

        service.eliminar(c);

        verify(clienteRepo).delete(c);
    }

    private static Cliente cliente(Long id, String email, String username, String password) {
        Cliente c = new Cliente();
        // Cliente tiene setters; id no tiene setter en tu entidad, así que lo fijamos por reflexión.
        setId(c, id);
        c.setEmail(email);
        c.setUsername(username);
        c.setPassword(password);
        c.setNombre("Nombre");
        c.setTelefono("600000000");
        c.setDireccion("Calle 1");
        c.setActivo(true);
        return c;
    }

    private static void setId(Cliente c, Long id) {
        try {
            var f = Cliente.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}