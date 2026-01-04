package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
import com.serveat.repository.menu.CategoriaRepository;
import com.serveat.repository.menu.IngredienteRepository;
import com.serveat.repository.menu.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepo;

    @Mock
    private CategoriaRepository categoriaRepo;

    @Mock
    private IngredienteRepository ingredienteRepo;

    @InjectMocks
    private ProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        // Evita tocar filesystem en tests
        ReflectionTestUtils.setField(service, "uploadPath", "target/test-uploads/productos");
        ReflectionTestUtils.setField(service, "publicUrl", "/images/productos");
    }

    @Test
    void crearProducto_si_categoria_no_existe_lanza_y_no_guarda() {
        when(categoriaRepo.findByNombre("Bebidas")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearProducto(
                "Coca Cola",
                "Refresco",
                new BigDecimal("2.50"),
                "Bebidas"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La categoría no existe");

        verify(categoriaRepo).findByNombre("Bebidas");
        verifyNoInteractions(productoRepo);
        verifyNoMoreInteractions(categoriaRepo);
        verifyNoInteractions(ingredienteRepo);
    }

    @Test
    void crearProducto_si_categoria_existe_guarda_con_codigo_y_categoria() {
        Categoria cat = new Categoria();
        cat.setNombre("Bebidas");

        when(categoriaRepo.findByNombre("Bebidas")).thenReturn(Optional.of(cat));
        when(productoRepo.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto res = service.crearProducto(
                "Coca Cola",
                "Refresco",
                new BigDecimal("2.50"),
                "Bebidas"
        );

        assertThat(res).isNotNull();
        assertThat(res.getCodigo()).isNotBlank();
        assertThat(res.getCodigo()).startsWith("PROD-");
        assertThat(res.getNombre()).isEqualTo("Coca Cola");
        assertThat(res.getDescripcion()).isEqualTo("Refresco");
        assertThat(res.getPrecio()).isEqualByComparingTo("2.50");
        assertThat(res.getCategoria()).isSameAs(cat);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepo).save(captor.capture());
        assertThat(captor.getValue().getCodigo()).isNotBlank();

        verify(categoriaRepo).findByNombre("Bebidas");
        verifyNoMoreInteractions(categoriaRepo, productoRepo);
        verifyNoInteractions(ingredienteRepo);
    }

    @Test
    void crearProductoConIngredientes_si_ingrediente_no_existe_lanza_y_no_guarda() {
        Categoria cat = new Categoria();
        cat.setNombre("Pizzas");

        when(categoriaRepo.findByNombre("Pizzas")).thenReturn(Optional.of(cat));
        when(ingredienteRepo.findByNombre("Queso")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearProductoConIngredientes(
                "Pizza",
                "Margarita",
                new BigDecimal("10.00"),
                "Pizzas",
                new LinkedHashSet<>(List.of("Queso")),
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ingrediente no encontrado: Queso");

        verify(categoriaRepo).findByNombre("Pizzas");
        verify(ingredienteRepo).findByNombre("Queso");
        verifyNoInteractions(productoRepo);
        verifyNoMoreInteractions(categoriaRepo, ingredienteRepo);
    }

    @Test
    void crearProductoConIngredientes_con_ingredientes_aplica_receta_y_guarda() {
        Categoria cat = new Categoria();
        cat.setNombre("Pizzas");

        Ingrediente queso = new Ingrediente();
        queso.setNombre("Queso");
        queso.setPrecioExtra(new BigDecimal("0.50"));

        Ingrediente jamon = new Ingrediente();
        jamon.setNombre("Jamón");
        jamon.setPrecioExtra(new BigDecimal("1.00"));

        when(categoriaRepo.findByNombre("Pizzas")).thenReturn(Optional.of(cat));
        when(ingredienteRepo.findByNombre("Queso")).thenReturn(Optional.of(queso));
        when(ingredienteRepo.findByNombre("Jamón")).thenReturn(Optional.of(jamon));
        when(productoRepo.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto res = service.crearProductoConIngredientes(
                "Pizza",
                "Margarita",
                new BigDecimal("10.00"),
                "Pizzas",
                new LinkedHashSet<>(List.of("Queso", "Jamón")),
                null,
                null
        );

        assertThat(res.getCodigo()).startsWith("PROD-");
        assertThat(res.getIngredientes()).hasSize(2);

        // Verifica que se han creado relaciones ProductoIngrediente con precioExtra del ingrediente
        List<ProductoIngrediente> rels = res.getIngredientes();
        assertThat(rels)
                .extracting(pi -> pi.getIngrediente().getNombre())
                .containsExactlyInAnyOrder("Queso", "Jamón");

        Map<String, BigDecimal> precioExtraPorNombre = new HashMap<>();
        for (ProductoIngrediente pi : rels) {
            precioExtraPorNombre.put(pi.getIngrediente().getNombre(), pi.getPrecioExtra());
            assertThat(pi.getProducto()).isSameAs(res);
        }

        assertThat(precioExtraPorNombre.get("Queso")).isEqualByComparingTo("0.50");
        assertThat(precioExtraPorNombre.get("Jamón")).isEqualByComparingTo("1.00");

        verify(categoriaRepo).findByNombre("Pizzas");
        verify(ingredienteRepo).findByNombre("Queso");
        verify(ingredienteRepo).findByNombre("Jamón");
        verify(productoRepo).save(any(Producto.class));
        verifyNoMoreInteractions(categoriaRepo, ingredienteRepo, productoRepo);
    }

    @Test
    void actualizarProductoConIngredientes_si_producto_no_existe_lanza() {
        when(productoRepo.findWithIngredientesByCodigo("PROD-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarProductoConIngredientes(
                "PROD-123",
                "Nuevo",
                "Desc",
                new BigDecimal("5.00"),
                "Cat",
                Set.of(),
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Producto no encontrado");

        verify(productoRepo).findWithIngredientesByCodigo("PROD-123");
        verifyNoMoreInteractions(productoRepo);
        verifyNoInteractions(categoriaRepo, ingredienteRepo);
    }

    @Test
    void actualizarProductoConIngredientes_si_categoria_no_existe_lanza_y_no_guarda() {
        Producto existente = new Producto();
        existente.setCodigo("PROD-123");
        existente.setIngredientes(new ArrayList<>(List.of())); // evita null

        when(productoRepo.findWithIngredientesByCodigo("PROD-123")).thenReturn(Optional.of(existente));
        when(categoriaRepo.findByNombre("Cat")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarProductoConIngredientes(
                "PROD-123",
                "Nuevo",
                "Desc",
                new BigDecimal("5.00"),
                "Cat",
                Set.of(),
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La categoría no existe");

        verify(productoRepo).findWithIngredientesByCodigo("PROD-123");
        verify(categoriaRepo).findByNombre("Cat");
        verifyNoMoreInteractions(productoRepo, categoriaRepo);
        verifyNoInteractions(ingredienteRepo);
    }

    @Test
    void actualizarProductoConIngredientes_reemplaza_ingredientes_y_guarda() {
        Producto existente = new Producto();
        existente.setCodigo("PROD-123");

        Ingrediente viejoIng = new Ingrediente();
        viejoIng.setNombre("Viejo");
        viejoIng.setPrecioExtra(new BigDecimal("0.10"));

        // Ingrediente antiguo en el producto
        existente.setIngredientes(new ArrayList<>());
        existente.getIngredientes().add(new ProductoIngrediente(
                existente, viejoIng, true, true, viejoIng.getPrecioExtra()
        ));

        Categoria nuevaCat = new Categoria();
        nuevaCat.setNombre("NuevaCat");

        Ingrediente nuevoIng = new Ingrediente();
        nuevoIng.setNombre("NuevoIng");
        nuevoIng.setPrecioExtra(new BigDecimal("0.30"));

        when(productoRepo.findWithIngredientesByCodigo("PROD-123")).thenReturn(Optional.of(existente));
        when(categoriaRepo.findByNombre("NuevaCat")).thenReturn(Optional.of(nuevaCat));
        when(ingredienteRepo.findByNombre("NuevoIng")).thenReturn(Optional.of(nuevoIng));
        when(productoRepo.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto res = service.actualizarProductoConIngredientes(
                "PROD-123",
                "NombreNuevo",
                "DescNueva",
                new BigDecimal("8.00"),
                "NuevaCat",
                new LinkedHashSet<>(List.of("NuevoIng")),
                null,
                null
        );

        assertThat(res.getNombre()).isEqualTo("NombreNuevo");
        assertThat(res.getDescripcion()).isEqualTo("DescNueva");
        assertThat(res.getPrecio()).isEqualByComparingTo("8.00");
        assertThat(res.getCategoria()).isSameAs(nuevaCat);

        assertThat(res.getIngredientes()).hasSize(1);
        assertThat(res.getIngredientes().get(0).getIngrediente().getNombre()).isEqualTo("NuevoIng");

        verify(productoRepo).findWithIngredientesByCodigo("PROD-123");
        verify(categoriaRepo).findByNombre("NuevaCat");
        verify(ingredienteRepo).findByNombre("NuevoIng");
        verify(productoRepo).save(existente);
        verifyNoMoreInteractions(productoRepo, categoriaRepo, ingredienteRepo);
    }

    @Test
    void obtenerPorCodigo_si_no_existe_lanza() {
        when(productoRepo.findByCodigo("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorCodigo("X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Producto no encontrado");

        verify(productoRepo).findByCodigo("X");
        verifyNoMoreInteractions(productoRepo);
        verifyNoInteractions(categoriaRepo, ingredienteRepo);
    }

    @Test
    void obtenerConIngredientesPorCodigo_si_no_existe_lanza() {
        when(productoRepo.findWithIngredientesByCodigo("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerConIngredientesPorCodigo("X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Producto no encontrado");

        verify(productoRepo).findWithIngredientesByCodigo("X");
        verifyNoMoreInteractions(productoRepo);
        verifyNoInteractions(categoriaRepo, ingredienteRepo);
    }

    @Test
    void listarProductos_devuelve_findAll() {
        Producto p1 = new Producto();
        p1.setCodigo("A");
        Producto p2 = new Producto();
        p2.setCodigo("B");

        when(productoRepo.findAll()).thenReturn(List.of(p1, p2));

        List<Producto> res = service.listarProductos();

        assertThat(res).containsExactly(p1, p2);

        verify(productoRepo).findAll();
        verifyNoMoreInteractions(productoRepo);
        verifyNoInteractions(categoriaRepo, ingredienteRepo);
    }

    @Test
    void buscarPorCategoria_si_categoria_no_existe_lanza() {
        when(categoriaRepo.findByNombre("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorCategoria("X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Categoría no encontrada");

        verify(categoriaRepo).findByNombre("X");
        verifyNoMoreInteractions(categoriaRepo);
        verifyNoInteractions(productoRepo, ingredienteRepo);
    }

    @Test
    void buscarPorCategoria_si_categoria_existe_devuelve_lista_del_repo() {
        Categoria cat = new Categoria();
        cat.setNombre("Bebidas");

        Producto p1 = new Producto();
        p1.setCodigo("A");

        when(categoriaRepo.findByNombre("Bebidas")).thenReturn(Optional.of(cat));
        when(productoRepo.findByCategoria(cat)).thenReturn(List.of(p1));

        List<Producto> res = service.buscarPorCategoria("Bebidas");

        assertThat(res).containsExactly(p1);

        verify(categoriaRepo).findByNombre("Bebidas");
        verify(productoRepo).findByCategoria(cat);
        verifyNoMoreInteractions(categoriaRepo, productoRepo);
        verifyNoInteractions(ingredienteRepo);
    }

    @Test
    void buscarPorNombreParcial_usa_like_con_wildcards() {
        when(productoRepo.findByNombreLike("%bur%")).thenReturn(List.of());

        List<Producto> res = service.buscarPorNombreParcial("bur");

        assertThat(res).isEmpty();

        verify(productoRepo).findByNombreLike("%bur%");
        verifyNoMoreInteractions(productoRepo);
        verifyNoInteractions(categoriaRepo, ingredienteRepo);
    }

    @Test
    void buscarPorDescripcionParcial_usa_like_con_wildcards() {
        when(productoRepo.findByDescripcionLike("%queso%")).thenReturn(List.of());

        List<Producto> res = service.buscarPorDescripcionParcial("queso");

        assertThat(res).isEmpty();

        verify(productoRepo).findByDescripcionLike("%queso%");
        verifyNoMoreInteractions(productoRepo);
        verifyNoInteractions(categoriaRepo, ingredienteRepo);
    }

    @Test
    void eliminarProducto_si_no_existe_lanza_y_no_borra() {
        when(productoRepo.findByCodigo("PROD-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminarProducto("PROD-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Producto no encontrado");

        verify(productoRepo).findByCodigo("PROD-1");
        verifyNoMoreInteractions(productoRepo);
        verifyNoInteractions(categoriaRepo, ingredienteRepo);
    }

    @Test
    void eliminarProducto_si_existe_borra() {
        Producto p = new Producto();
        p.setCodigo("PROD-1");

        when(productoRepo.findByCodigo("PROD-1")).thenReturn(Optional.of(p));

        service.eliminarProducto("PROD-1");

        verify(productoRepo).findByCodigo("PROD-1");
        verify(productoRepo).delete(p);
        verifyNoMoreInteractions(productoRepo);
        verifyNoInteractions(categoriaRepo, ingredienteRepo);
    }

    @Test
    void listarNombresIngredientes_filtra_null_y_ordena() {
        Ingrediente i1 = new Ingrediente();
        i1.setNombre("Zanahoria");

        Ingrediente i2 = new Ingrediente();
        i2.setNombre(null);

        Ingrediente i3 = new Ingrediente();
        i3.setNombre("Ajo");

        when(ingredienteRepo.findAll()).thenReturn(List.of(i1, i2, i3));

        List<String> res = service.listarNombresIngredientes();

        assertThat(res).containsExactly("Ajo", "Zanahoria");

        verify(ingredienteRepo).findAll();
        verifyNoMoreInteractions(ingredienteRepo);
        verifyNoInteractions(productoRepo, categoriaRepo);
    }

    @Test
    void productoTieneIngredientes_si_codigo_null_o_blank_devuelve_false_y_no_llama_repo() {
        assertThat(service.productoTieneIngredientes(null)).isFalse();
        assertThat(service.productoTieneIngredientes(" ")).isFalse();

        verifyNoInteractions(productoRepo, categoriaRepo, ingredienteRepo);
    }

    @Test
    void productoTieneIngredientes_si_codigo_valido_delega_en_repo() {
        when(productoRepo.productoTieneIngredientes("PROD-1")).thenReturn(true);

        boolean res = service.productoTieneIngredientes("PROD-1");

        assertThat(res).isTrue();

        verify(productoRepo).productoTieneIngredientes("PROD-1");
        verifyNoMoreInteractions(productoRepo);
        verifyNoInteractions(categoriaRepo, ingredienteRepo);
    }

    @Test
    void crearProductoConIngredientes_con_imagen_invalida_lanza_illegalState_y_no_guarda() {
        Categoria cat = new Categoria();
        cat.setNombre("Bebidas");

        when(categoriaRepo.findByNombre("Bebidas")).thenReturn(Optional.of(cat));

        // Provoca IOException real usando un path inválido en Unix, para entrar en el catch (IOException)
        // y que el servicio lance IllegalStateException.
        ReflectionTestUtils.setField(service, "uploadPath", "/dev/null/no-es-un-directorio");

        byte[] bytes = "x".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.crearProductoConIngredientes(
                "Agua",
                "Desc",
                new BigDecimal("1.00"),
                "Bebidas",
                Set.of(),
                bytes,
                "foto.png"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo guardar la imagen del producto");

        verify(categoriaRepo).findByNombre("Bebidas");
        verifyNoInteractions(productoRepo);
        verifyNoInteractions(ingredienteRepo);
        verifyNoMoreInteractions(categoriaRepo);
    }
}