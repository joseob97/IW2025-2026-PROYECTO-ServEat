package com.serveat.repository.usuario;

import com.serveat.domain.usuario.Cliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClienteRepositoryIT {

    @Autowired
    private ClienteRepository repo;

    // Crea un cliente válido cumpliendo todas las restricciones de la entidad
    private Cliente crearCliente(String nombre, String email, String username, boolean activo) {
        Cliente c = new Cliente();
        c.setNombre(nombre);
        c.setEmail(email);
        c.setUsername(username);
        c.setPassword("password");
        c.setTelefono("600123456");
        c.setDireccion("Calle Principal 1");
        c.setActivo(activo);
        return c;
    }

    // Verifica que findByUsername devuelve el cliente cuando existe
    @Test
    void findByUsername_cuandoExiste_devuelveCliente() {
        Cliente guardado = repo.save(
                crearCliente("Ana", "ana@test.com", "ana1", true)
        );

        Optional<Cliente> res = repo.findByUsername("ana1");

        assertThat(res).isPresent();
        assertThat(res.get().getId()).isEqualTo(guardado.getId());
        assertThat(res.get().getEmail()).isEqualTo("ana@test.com");
    }

    // Verifica que findByUsername devuelve vacío cuando no existe
    @Test
    void findByUsername_cuandoNoExiste_devuelveEmpty() {
        assertThat(repo.findByUsername("inexistente")).isEmpty();
    }

    // Verifica que findByEmail devuelve el cliente cuando existe
    @Test
    void findByEmail_cuandoExiste_devuelveCliente() {
        Cliente guardado = repo.save(
                crearCliente("Luis", "luis@test.com", "luis1", true)
        );

        Optional<Cliente> res = repo.findByEmail("luis@test.com");

        assertThat(res).isPresent();
        assertThat(res.get().getId()).isEqualTo(guardado.getId());
        assertThat(res.get().getUsername()).isEqualTo("luis1");
    }

    // Verifica que findByEmail devuelve vacío cuando no existe
    @Test
    void findByEmail_cuandoNoExiste_devuelveEmpty() {
        assertThat(repo.findByEmail("nadie@test.com")).isEmpty();
    }

    // Verifica existsByEmail cuando el email existe
    @Test
    void existsByEmail_cuandoExiste_devuelveTrue() {
        repo.save(
                crearCliente("Marta", "marta@test.com", "marta1", true)
        );

        assertThat(repo.existsByEmail("marta@test.com")).isTrue();
    }

    // Verifica existsByEmail cuando el email no existe
    @Test
    void existsByEmail_cuandoNoExiste_devuelveFalse() {
        assertThat(repo.existsByEmail("no@test.com")).isFalse();
    }

    // Verifica existsByUsername cuando el username existe
    @Test
    void existsByUsername_cuandoExiste_devuelveTrue() {
        repo.save(
                crearCliente("Pepe", "pepe@test.com", "pepe1", true)
        );

        assertThat(repo.existsByUsername("pepe1")).isTrue();
    }

    // Verifica existsByUsername cuando el username no existe
    @Test
    void existsByUsername_cuandoNoExiste_devuelveFalse() {
        assertThat(repo.existsByUsername("fantasma")).isFalse();
    }

    // Verifica que findByActivoTrue devuelve únicamente clientes activos
    @Test
    void findByActivoTrue_devuelveSoloActivos() {
        repo.save(crearCliente("Activo1", "a1@test.com", "a1", true));
        repo.save(crearCliente("Activo2", "a2@test.com", "a2", true));
        repo.save(crearCliente("Inactivo1", "i1@test.com", "i1", false));

        List<Cliente> activos = repo.findByActivoTrue();

        assertThat(activos).hasSize(2);
        assertThat(activos).allMatch(Cliente::isActivo);
        assertThat(activos)
                .extracting(Cliente::getUsername)
                .containsExactlyInAnyOrder("a1", "a2");
    }

    // Verifica que findByActivoFalse devuelve únicamente clientes inactivos
    @Test
    void findByActivoFalse_devuelveSoloInactivos() {
        repo.save(crearCliente("Activo", "a@test.com", "a", true));
        repo.save(crearCliente("Inactivo1", "i1@test.com", "i1", false));
        repo.save(crearCliente("Inactivo2", "i2@test.com", "i2", false));

        List<Cliente> inactivos = repo.findByActivoFalse();

        assertThat(inactivos).hasSize(2);
        assertThat(inactivos).allMatch(c -> !c.isActivo());
        assertThat(inactivos)
                .extracting(Cliente::getUsername)
                .containsExactlyInAnyOrder("i1", "i2");
    }
}