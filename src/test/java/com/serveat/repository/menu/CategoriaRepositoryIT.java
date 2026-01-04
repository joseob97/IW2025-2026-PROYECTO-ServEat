package com.serveat.repository.menu;

import com.serveat.domain.menu.Categoria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoriaRepositoryIT {

    @Autowired
    private CategoriaRepository repo;

    @PersistenceContext
    private EntityManager em;

    private Categoria crearCategoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return c;
    }

    @Test
    void findByNombre_cuandoExiste_devuelveCategoria() {
        repo.saveAndFlush(crearCategoria("Bebidas"));

        Optional<Categoria> res = repo.findByNombre("Bebidas");

        assertThat(res).isPresent();
        assertThat(res.get().getNombre()).isEqualTo("Bebidas");
    }

    @Test
    void findByNombre_cuandoNoExiste_devuelveEmpty() {
        assertThat(repo.findByNombre("NoExiste")).isEmpty();
    }

    @Test
    void save_incrementaCount() {
        long before = repo.count();

        repo.saveAndFlush(crearCategoria("Postres"));

        assertThat(repo.count()).isEqualTo(before + 1);
        assertThat(repo.findByNombre("Postres")).isPresent();
    }

    @Test
    void delete_eliminaCategoria_sinUsarId() {
        repo.saveAndFlush(crearCategoria("Entrantes"));
        assertThat(repo.findByNombre("Entrantes")).isPresent();

        // Borramos recuperando la entidad (sin getId)
        Categoria entidad = repo.findByNombre("Entrantes").orElseThrow();
        repo.delete(entidad);
        repo.flush();

        assertThat(repo.findByNombre("Entrantes")).isEmpty();
    }

    @Test
    void findAll_contieneLasCategoriasGuardadas() {
        repo.save(crearCategoria("Bebidas"));
        repo.save(crearCategoria("Postres"));
        repo.save(crearCategoria("Entrantes"));
        repo.flush();

        assertThat(repo.findAll())
                .extracting(Categoria::getNombre)
                .contains("Bebidas", "Postres", "Entrantes");
    }

    @Test
    void update_actualizaNombre() {
        repo.saveAndFlush(crearCategoria("Comida"));

        Categoria c = repo.findByNombre("Comida").orElseThrow();
        c.setNombre("Comida Rápida");

        repo.saveAndFlush(c);

        assertThat(repo.findByNombre("Comida")).isEmpty();
        assertThat(repo.findByNombre("Comida Rápida")).isPresent();
    }
}