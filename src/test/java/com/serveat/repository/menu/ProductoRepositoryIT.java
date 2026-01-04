package com.serveat.repository.menu;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
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
class ProductoRepositoryIT {

    @Autowired
    private ProductoRepository repo;

    @PersistenceContext
    private EntityManager em;

    private Categoria crearCategoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return c;
    }

    private Producto crearProducto(String codigo, String nombre, String descripcion, BigDecimal precio, Categoria categoria) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setCategoria(categoria);
        p.setImagenUrl(null);
        return p;
    }

    private Ingrediente crearIngrediente(String nombre, BigDecimal precioExtra) {
        Ingrediente i = new Ingrediente();
        i.setNombre(nombre);
        i.setPrecioExtra(precioExtra);
        return i;
    }

    private ProductoIngrediente crearProductoIngrediente(Producto producto, Ingrediente ingrediente,
                                                         boolean porDefecto, boolean opcional, BigDecimal precioExtra) {
        return new ProductoIngrediente(producto, ingrediente, porDefecto, opcional, precioExtra);
    }

    @Test
    void findByCategoria_devuelveProductos_yCargaCategoriaConEntityGraph() {
        Categoria catBebidas = crearCategoria("Bebidas");
        Categoria catComida = crearCategoria("Comida");
        em.persist(catBebidas);
        em.persist(catComida);

        Producto p1 = crearProducto("PR-001", "CocaCola", "Refresco", new BigDecimal("2.50"), catBebidas);
        Producto p2 = crearProducto("PR-002", "Agua", "Agua mineral", new BigDecimal("1.50"), catBebidas);
        Producto p3 = crearProducto("PR-003", "Hamburguesa", "Carne", new BigDecimal("7.00"), catComida);
        repo.save(p1);
        repo.save(p2);
        repo.save(p3);
        repo.flush();

        em.clear();

        List<Producto> res = repo.findByCategoria(catBebidas);

        assertThat(res).hasSize(2);
        assertThat(res).extracting(Producto::getCodigo).containsExactlyInAnyOrder("PR-001", "PR-002");

        // EntityGraph: categoria debe venir inicializada
        assertThat(res).allMatch(p -> Hibernate.isInitialized(p.getCategoria()));
        assertThat(res).allMatch(p -> p.getCategoria() != null);
        assertThat(res).allMatch(p -> "Bebidas".equals(p.getCategoria().getNombre()));
    }

    @Test
    void findByNombreLike_filtraCorrecto_yCargaCategoriaConEntityGraph() {
        Categoria cat = crearCategoria("Postres");
        em.persist(cat);

        repo.save(crearProducto("PR-010", "Helado Vainilla", "Postre", new BigDecimal("2.00"), cat));
        repo.save(crearProducto("PR-011", "Helado Chocolate", "Postre", new BigDecimal("2.20"), cat));
        repo.save(crearProducto("PR-012", "Tarta Queso", "Postre", new BigDecimal("3.50"), cat));
        repo.flush();

        em.clear();

        List<Producto> res = repo.findByNombreLike("%Helado%");

        assertThat(res).hasSize(2);
        assertThat(res).extracting(Producto::getCodigo).containsExactlyInAnyOrder("PR-010", "PR-011");
        assertThat(res).allMatch(p -> Hibernate.isInitialized(p.getCategoria()));
        assertThat(res).allMatch(p -> p.getCategoria() != null);
    }

    @Test
    void findByNombre_cuandoExiste_devuelveProducto() {
        Categoria cat = crearCategoria("Snacks");
        em.persist(cat);

        repo.save(crearProducto("PR-020", "Patatas", "Snack", new BigDecimal("1.80"), cat));
        repo.flush();

        Optional<Producto> res = repo.findByNombre("Patatas");

        assertThat(res).isPresent();
        assertThat(res.get().getCodigo()).isEqualTo("PR-020");
    }

    @Test
    void findByDescripcion_yFindByDescripcionLike_filtranCorrecto() {
        Categoria cat = crearCategoria("Sandwich");
        em.persist(cat);

        repo.save(crearProducto("PR-030", "Mixto", "Sandwich mixto", new BigDecimal("3.00"), cat));
        repo.save(crearProducto("PR-031", "Vegetal", "Sandwich vegetal", new BigDecimal("3.20"), cat));
        repo.save(crearProducto("PR-032", "Otro", "Bocadillo", new BigDecimal("4.00"), cat));
        repo.flush();

        List<Producto> exact = repo.findByDescripcion("Sandwich mixto");
        assertThat(exact).hasSize(1);
        assertThat(exact.get(0).getCodigo()).isEqualTo("PR-030");

        List<Producto> like = repo.findByDescripcionLike("%Sandwich%");
        assertThat(like).hasSize(2);
        assertThat(like).extracting(Producto::getCodigo).containsExactlyInAnyOrder("PR-030", "PR-031");
    }

    @Test
    void findByPrecio_filtraCorrecto() {
        Categoria cat = crearCategoria("Precios");
        em.persist(cat);

        repo.save(crearProducto("PR-040", "A", "X", new BigDecimal("9.99"), cat));
        repo.save(crearProducto("PR-041", "B", "X", new BigDecimal("9.99"), cat));
        repo.save(crearProducto("PR-042", "C", "X", new BigDecimal("5.00"), cat));
        repo.flush();

        List<Producto> res = repo.findByPrecio(new BigDecimal("9.99"));

        assertThat(res).hasSize(2);
        assertThat(res).extracting(Producto::getCodigo).containsExactlyInAnyOrder("PR-040", "PR-041");
    }

    @Test
    void findByCodigo_cuandoExiste_devuelveProducto() {
        Categoria cat = crearCategoria("Codigos");
        em.persist(cat);

        repo.save(crearProducto("PR-050", "ProductoX", "Desc", new BigDecimal("1.00"), cat));
        repo.flush();

        Optional<Producto> res = repo.findByCodigo("PR-050");

        assertThat(res).isPresent();
        assertThat(res.get().getNombre()).isEqualTo("ProductoX");
    }

    @Test
    void findWithIngredientesByCodigo_cargaCategoria_eIngredientes_eIngredienteConEntityGraph() {
        Categoria cat = crearCategoria("Pizzas");
        em.persist(cat);

        Producto pizza = crearProducto("PR-060", "Pizza", "Pizza base", new BigDecimal("8.00"), cat);
        repo.save(pizza);

        Ingrediente queso = crearIngrediente("Queso", new BigDecimal("0.50"));
        Ingrediente bacon = crearIngrediente("Bacon", new BigDecimal("0.80"));
        em.persist(queso);
        em.persist(bacon);

        // Relación ProductoIngrediente
        ProductoIngrediente pi1 = crearProductoIngrediente(pizza, queso, true, true, new BigDecimal("0.50"));
        ProductoIngrediente pi2 = crearProductoIngrediente(pizza, bacon, false, true, new BigDecimal("0.80"));
        em.persist(pi1);
        em.persist(pi2);

        repo.flush();
        em.clear();

        Optional<Producto> res = repo.findWithIngredientesByCodigo("PR-060");

        assertThat(res).isPresent();

        Producto p = res.get();

        // EntityGraph: categoria e ingredientes deben estar inicializados
        assertThat(Hibernate.isInitialized(p.getCategoria())).isTrue();
        assertThat(Hibernate.isInitialized(p.getIngredientes())).isTrue();

        assertThat(p.getCategoria()).isNotNull();
        assertThat(p.getCategoria().getNombre()).isEqualTo("Pizzas");

        assertThat(p.getIngredientes()).hasSize(2);

        // EntityGraph: ingredientes.ingrediente inicializado
        assertThat(p.getIngredientes()).allMatch(x -> Hibernate.isInitialized(x.getIngrediente()));
        assertThat(p.getIngredientes()).extracting(x -> x.getIngrediente().getNombre())
                .containsExactlyInAnyOrder("Queso", "Bacon");
    }

    @Test
    void findAll_conEntityGraph_cargaCategoria_eIngredientes() {
        Categoria cat = crearCategoria("AllGraph");
        em.persist(cat);

        Producto p1 = crearProducto("PR-070", "Prod1", "D1", new BigDecimal("1.00"), cat);
        Producto p2 = crearProducto("PR-071", "Prod2", "D2", new BigDecimal("2.00"), cat);
        repo.save(p1);
        repo.save(p2);

        Ingrediente ing = crearIngrediente("Salsa", new BigDecimal("0.30"));
        em.persist(ing);
        em.persist(crearProductoIngrediente(p1, ing, true, true, new BigDecimal("0.30")));

        repo.flush();
        em.clear();

        List<Producto> res = repo.findAll();

        assertThat(res).extracting(Producto::getCodigo).contains("PR-070", "PR-071");
        assertThat(res).allMatch(p -> Hibernate.isInitialized(p.getCategoria()));
        assertThat(res).allMatch(p -> Hibernate.isInitialized(p.getIngredientes()));
    }

    @Test
    void productoTieneIngredientes_devuelveTrueSiExisteRelacion_yFalseSiNo() {
        Categoria cat = crearCategoria("BoolQ");
        em.persist(cat);

        Producto con = crearProducto("PR-080", "ConIng", "D", new BigDecimal("3.00"), cat);
        Producto sin = crearProducto("PR-081", "SinIng", "D", new BigDecimal("3.00"), cat);
        repo.save(con);
        repo.save(sin);

        Ingrediente ing = crearIngrediente("Pepinillo", BigDecimal.ZERO);
        em.persist(ing);
        em.persist(crearProductoIngrediente(con, ing, true, true, BigDecimal.ZERO));

        repo.flush();
        em.clear();

        assertThat(repo.productoTieneIngredientes("PR-080")).isTrue();
        assertThat(repo.productoTieneIngredientes("PR-081")).isFalse();
    }

    @Test
    void findByProductoCodigoFetchIngrediente_traeProductoIngrediente_conIngredienteCargado() {
        Categoria cat = crearCategoria("FetchIng");
        em.persist(cat);

        Producto p = crearProducto("PR-090", "Producto", "D", new BigDecimal("4.00"), cat);
        repo.save(p);

        Ingrediente i1 = crearIngrediente("Ketchup", new BigDecimal("0.10"));
        Ingrediente i2 = crearIngrediente("Mostaza", new BigDecimal("0.10"));
        em.persist(i1);
        em.persist(i2);

        em.persist(crearProductoIngrediente(p, i1, true, true, new BigDecimal("0.10")));
        em.persist(crearProductoIngrediente(p, i2, false, true, new BigDecimal("0.10")));

        repo.flush();
        em.clear();

        List<ProductoIngrediente> res = repo.findByProductoCodigoFetchIngrediente("PR-090");

        assertThat(res).hasSize(2);
        assertThat(res).allMatch(pi -> Hibernate.isInitialized(pi.getIngrediente()));
        assertThat(res).extracting(pi -> pi.getIngrediente().getNombre())
                .containsExactlyInAnyOrder("Ketchup", "Mostaza");
    }
}