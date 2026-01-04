package com.serveat.repository.menu;

import com.serveat.domain.menu.Ingrediente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IngredienteRepositoryIT {

    @Autowired
    private IngredienteRepository repo;

    @PersistenceContext
    private EntityManager em;

    private Ingrediente crearIngrediente(String nombre, BigDecimal precioExtra) {
        Ingrediente i = new Ingrediente();
        i.setNombre(nombre);
        i.setPrecioExtra(precioExtra);
        return i;
    }

    @Test
    void findByNombre_cuandoExiste_devuelveIngrediente() {
        repo.saveAndFlush(crearIngrediente("Queso", new BigDecimal("0.50")));

        Optional<Ingrediente> res = repo.findByNombre("Queso");

        assertThat(res).isPresent();
        assertThat(res.get().getNombre()).isEqualTo("Queso");
        assertThat(res.get().getPrecioExtra()).isEqualByComparingTo("0.50");
    }

    @Test
    void findByNombre_cuandoNoExiste_devuelveEmpty() {
        assertThat(repo.findByNombre("NoExiste")).isEmpty();
    }

    @Test
    void save_incrementaCount() {
        long before = repo.count();

        repo.saveAndFlush(crearIngrediente("Bacon", new BigDecimal("1.00")));

        assertThat(repo.count()).isEqualTo(before + 1);
        assertThat(repo.findByNombre("Bacon")).isPresent();
    }

    @Test
    void delete_eliminaIngrediente_sinUsarId() {
        repo.saveAndFlush(crearIngrediente("Cebolla", new BigDecimal("0.20")));
        assertThat(repo.findByNombre("Cebolla")).isPresent();

        Ingrediente entidad = repo.findByNombre("Cebolla").orElseThrow();
        repo.delete(entidad);
        repo.flush();

        assertThat(repo.findByNombre("Cebolla")).isEmpty();
    }

    @Test
    void findByNombreContainingIgnoreCaseOrderByNombreAsc_filtraPorSubcadena_ignorandoMayusculas_yOrdenaAsc() {
        repo.save(crearIngrediente("Queso", new BigDecimal("0.50")));
        repo.save(crearIngrediente("queso azul", new BigDecimal("0.80")));
        repo.save(crearIngrediente("Queso de cabra", new BigDecimal("0.90")));
        repo.save(crearIngrediente("Bacon", new BigDecimal("1.00")));
        repo.flush();

        List<Ingrediente> res = repo.findByNombreContainingIgnoreCaseOrderByNombreAsc("QuEsO");

        // Debe devolver solo los que contienen "queso" (case-insensitive) y ordenados por nombre ASC
        assertThat(res).extracting(Ingrediente::getNombre)
                .containsExactly("Queso", "Queso de cabra", "queso azul");
    }

    @Test
    void findByNombreContainingIgnoreCaseOrderByNombreAsc_cuandoNoHayCoincidencias_devuelveListaVacia() {
        repo.saveAndFlush(crearIngrediente("Tomate", new BigDecimal("0.10")));

        List<Ingrediente> res = repo.findByNombreContainingIgnoreCaseOrderByNombreAsc("queso");

        assertThat(res).isEmpty();
    }

    @Test
    void update_actualizaPrecioExtra() {
        repo.saveAndFlush(crearIngrediente("Salsa", new BigDecimal("0.30")));

        Ingrediente i = repo.findByNombre("Salsa").orElseThrow();
        i.setPrecioExtra(new BigDecimal("0.45"));

        repo.saveAndFlush(i);

        Ingrediente actualizado = repo.findByNombre("Salsa").orElseThrow();
        assertThat(actualizado.getPrecioExtra()).isEqualByComparingTo("0.45");
    }
}