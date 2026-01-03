package com.serveat.repository.usuario;

import com.serveat.domain.usuario.Empleado;
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
class EmpleadoRepositoryIT {

    @Autowired
    private EmpleadoRepository repo;

    // Crea un empleado válido cumpliendo todas las restricciones de la entidad
    private Empleado crearEmpleado(String nombre,
                                   String username,
                                   String email,
                                   String rol,
                                   boolean enabled) {
        return new Empleado(
                nombre,
                username,
                "password",
                "600123456",
                email,
                "Calle Principal 1",
                rol,
                enabled
        );
    }

    // Verifica que findByUsername devuelve el empleado cuando existe
    @Test
    void findByUsername_cuandoExiste_devuelveEmpleado() {
        Empleado guardado = repo.save(
                crearEmpleado("Ana", "ana1", "ana@test.com", "CAMARERO", true)
        );

        Optional<Empleado> res = repo.findByUsername("ana1");

        assertThat(res).isPresent();
        assertThat(res.get().getId()).isEqualTo(guardado.getId());
        assertThat(res.get().getEmail()).isEqualTo("ana@test.com");
        assertThat(res.get().getRol()).isEqualTo("CAMARERO");
    }

    // Verifica que findByUsername devuelve vacío cuando no existe
    @Test
    void findByUsername_cuandoNoExiste_devuelveEmpty() {
        assertThat(repo.findByUsername("inexistente")).isEmpty();
    }

    // Verifica que findByEmail devuelve el empleado cuando existe
    @Test
    void findByEmail_cuandoExiste_devuelveEmpleado() {
        Empleado guardado = repo.save(
                crearEmpleado("Luis", "luis1", "luis@test.com", "COCINERO", true)
        );

        Optional<Empleado> res = repo.findByEmail("luis@test.com");

        assertThat(res).isPresent();
        assertThat(res.get().getId()).isEqualTo(guardado.getId());
        assertThat(res.get().getUsername()).isEqualTo("luis1");
        assertThat(res.get().getRol()).isEqualTo("COCINERO");
    }

    // Verifica que findByEmail devuelve vacío cuando no existe
    @Test
    void findByEmail_cuandoNoExiste_devuelveEmpty() {
        assertThat(repo.findByEmail("nadie@test.com")).isEmpty();
    }

    // Verifica que findByEnabledTrue devuelve únicamente empleados habilitados
    @Test
    void findByEnabledTrue_devuelveSoloHabilitados() {
        repo.save(crearEmpleado("Hab1", "h1", "h1@test.com", "ADMIN", true));
        repo.save(crearEmpleado("Hab2", "h2", "h2@test.com", "CAMARERO", true));
        repo.save(crearEmpleado("Des1", "d1", "d1@test.com", "COCINERO", false));

        List<Empleado> habilitados = repo.findByEnabledTrue();

        assertThat(habilitados).hasSize(2);
        assertThat(habilitados).allMatch(Empleado::isEnabled);
        assertThat(habilitados)
                .extracting(Empleado::getUsername)
                .containsExactlyInAnyOrder("h1", "h2");
    }

    // Verifica que findByEnabledFalse devuelve únicamente empleados deshabilitados
    @Test
    void findByEnabledFalse_devuelveSoloDeshabilitados() {
        repo.save(crearEmpleado("Hab", "h1", "h1@test.com", "ADMIN", true));
        repo.save(crearEmpleado("Des1", "d1", "d1@test.com", "CAMARERO", false));
        repo.save(crearEmpleado("Des2", "d2", "d2@test.com", "COCINERO", false));

        List<Empleado> deshabilitados = repo.findByEnabledFalse();

        assertThat(deshabilitados).hasSize(2);
        assertThat(deshabilitados).allMatch(e -> !e.isEnabled());
        assertThat(deshabilitados)
                .extracting(Empleado::getUsername)
                .containsExactlyInAnyOrder("d1", "d2");
    }

    // Verifica que findByRol devuelve únicamente empleados con ese rol
    @Test
    void findByRol_devuelveSoloEseRol() {
        repo.save(crearEmpleado("Admin1", "a1", "a1@test.com", "ADMIN", true));
        repo.save(crearEmpleado("Admin2", "a2", "a2@test.com", "ADMIN", false));
        repo.save(crearEmpleado("Camarero1", "c1", "c1@test.com", "CAMARERO", true));

        List<Empleado> admins = repo.findByRol("ADMIN");

        assertThat(admins).hasSize(2);
        assertThat(admins).allMatch(e -> "ADMIN".equals(e.getRol()));
        assertThat(admins)
                .extracting(Empleado::getUsername)
                .containsExactlyInAnyOrder("a1", "a2");
    }

    // Verifica que findByRol devuelve lista vacía si no hay coincidencias
    @Test
    void findByRol_cuandoNoHayCoincidencias_devuelveVacio() {
        repo.save(crearEmpleado("Camarero1", "c1", "c1@test.com", "CAMARERO", true));

        assertThat(repo.findByRol("REPARTIDOR")).isEmpty();
    }
}