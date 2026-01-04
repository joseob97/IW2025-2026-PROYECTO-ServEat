package com.serveat.repository.menu;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Menu;
import com.serveat.domain.menu.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MenuRepositoryIT {

    @Autowired
    private MenuRepository repo;

    @PersistenceContext
    private EntityManager em;

    private Categoria crearCategoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return c;
    }

    private Producto crearProducto(String codigo, String nombre, BigDecimal precio, Categoria categoria) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setDescripcion("Desc " + nombre);
        p.setCategoria(categoria);
        p.setImagenUrl(null);
        return p;
    }

    private Menu crearMenu(String nombre, boolean activo, BigDecimal precioFijo, List<Producto> productos) {
        Menu m = new Menu();
        m.setNombre(nombre);
        m.setDescripcion("Desc " + nombre);
        m.setActivo(activo);
        m.setPrecioFijo(precioFijo);
        m.setProductos(productos);
        return m;
    }

    @Test
    void findByActivoTrue_devuelveSoloMenusActivos() {
        // Datos: categoría + productos
        Categoria cat = crearCategoria("Burgers");
        em.persist(cat);

        Producto p1 = crearProducto("PR-001", "Cheeseburger", new BigDecimal("7.50"), cat);
        Producto p2 = crearProducto("PR-002", "Fries", new BigDecimal("2.50"), cat);
        em.persist(p1);
        em.persist(p2);

        // Menús: 2 activos, 1 inactivo
        Menu activo1 = crearMenu("Menu Activo 1", true, new BigDecimal("9.99"), List.of(p1, p2));
        Menu activo2 = crearMenu("Menu Activo 2", true, new BigDecimal("8.99"), List.of(p1));
        Menu inactivo = crearMenu("Menu Inactivo", false, new BigDecimal("10.99"), List.of(p2));

        repo.save(activo1);
        repo.save(activo2);
        repo.save(inactivo);
        repo.flush();

        List<Menu> res = repo.findByActivoTrue();

        assertThat(res).hasSize(2);
        assertThat(res).allMatch(Menu::isActivo);
        assertThat(res).extracting(Menu::getNombre)
                .containsExactlyInAnyOrder("Menu Activo 1", "Menu Activo 2");
    }

    @Test
    void findMenusActivosConProductos_devuelveSoloActivos_yCargaProductosConJoinFetch() {
        Categoria cat = crearCategoria("Combos");
        em.persist(cat);

        Producto p1 = crearProducto("PR-101", "Pizza", new BigDecimal("6.00"), cat);
        Producto p2 = crearProducto("PR-102", "Refresco", new BigDecimal("1.50"), cat);
        Producto p3 = crearProducto("PR-103", "Helado", new BigDecimal("2.00"), cat);
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);

        Menu activoCon2 = crearMenu("Menu Cliente 1", true, new BigDecimal("7.99"), List.of(p1, p2));
        Menu activoCon0 = crearMenu("Menu Cliente 2", true, new BigDecimal("5.99"), List.of());
        Menu inactivo = crearMenu("Menu Cliente Inactivo", false, new BigDecimal("9.99"), List.of(p3));

        repo.save(activoCon2);
        repo.save(activoCon0);
        repo.save(inactivo);
        repo.flush();

        // Simula caso real: fuera de sesión/lazy -> limpiamos contexto
        em.clear();

        List<Menu> res = repo.findMenusActivosConProductos();

        assertThat(res).hasSize(2);
        assertThat(res).extracting(Menu::getNombre)
                .containsExactlyInAnyOrder("Menu Cliente 1", "Menu Cliente 2");

        // Verifica que productos vienen cargados (JOIN FETCH)
        Menu m1 = res.stream().filter(m -> m.getNombre().equals("Menu Cliente 1")).findFirst().orElseThrow();
        assertThat(m1.getProductos()).isNotNull();
        assertThat(m1.getProductos()).extracting(Producto::getCodigo)
                .containsExactlyInAnyOrder("PR-101", "PR-102");

        Menu m2 = res.stream().filter(m -> m.getNombre().equals("Menu Cliente 2")).findFirst().orElseThrow();
        assertThat(m2.getProductos()).isNotNull();
        assertThat(m2.getProductos()).isEmpty();
    }

    @Test
    void findMenusActivosConProductos_noDuplicaMenusAunqueTenganVariosProductos() {
        Categoria cat = crearCategoria("DupTest");
        em.persist(cat);

        Producto p1 = crearProducto("PR-201", "A", new BigDecimal("1.00"), cat);
        Producto p2 = crearProducto("PR-202", "B", new BigDecimal("2.00"), cat);
        Producto p3 = crearProducto("PR-203", "C", new BigDecimal("3.00"), cat);
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);

        Menu menu = crearMenu("Menu Multi", true, new BigDecimal("5.00"), List.of(p1, p2, p3));
        repo.saveAndFlush(menu);

        em.clear();

        List<Menu> res = repo.findMenusActivosConProductos();

        // Por el DISTINCT del query no debe venir duplicado
        assertThat(res).extracting(Menu::getNombre).contains("Menu Multi");
        assertThat(res.stream().filter(m -> m.getNombre().equals("Menu Multi")).count()).isEqualTo(1);

        Menu obtenido = res.stream().filter(m -> m.getNombre().equals("Menu Multi")).findFirst().orElseThrow();
        assertThat(obtenido.getProductos()).extracting(Producto::getCodigo)
                .containsExactlyInAnyOrder("PR-201", "PR-202", "PR-203");
    }
}