package com.serveat.service.menu.impl;

import com.serveat.domain.menu.Categoria;
import com.serveat.repository.menu.CategoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceImplTest {

    @Mock
    private CategoriaRepository categoriaRepo;

    @InjectMocks
    private CategoriaServiceImpl service;

    @Test
    void crearCategoria_guarda_y_devuelve_categoria_con_nombre() {
        Categoria guardada = new Categoria();
        guardada.setNombre("Bebidas");

        when(categoriaRepo.save(any(Categoria.class))).thenReturn(guardada);

        Categoria res = service.crearCategoria("Bebidas");

        assertThat(res).isNotNull();
        assertThat(res.getNombre()).isEqualTo("Bebidas");

        verify(categoriaRepo).save(any(Categoria.class));
        verifyNoMoreInteractions(categoriaRepo);
    }

    @Test
    void obtenerPorNombre_si_existe_devuelve_categoria() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Postres");

        when(categoriaRepo.findByNombre("Postres"))
                .thenReturn(Optional.of(categoria));

        Categoria res = service.obtenerPorNombre("Postres");

        assertThat(res).isNotNull();
        assertThat(res.getNombre()).isEqualTo("Postres");

        verify(categoriaRepo).findByNombre("Postres");
        verifyNoMoreInteractions(categoriaRepo);
    }

    @Test
    void obtenerPorNombre_si_no_existe_lanza_illegal_argument() {
        when(categoriaRepo.findByNombre("Inexistente"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorNombre("Inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Categoría no encontrada");

        verify(categoriaRepo).findByNombre("Inexistente");
        verifyNoMoreInteractions(categoriaRepo);
    }

    @Test
    void listarCategorias_devuelve_lista_del_repositorio() {
        Categoria c1 = new Categoria();
        c1.setNombre("Entrantes");

        Categoria c2 = new Categoria();
        c2.setNombre("Platos principales");

        when(categoriaRepo.findAll()).thenReturn(List.of(c1, c2));

        List<Categoria> res = service.listarCategorias();

        assertThat(res)
                .hasSize(2)
                .extracting(Categoria::getNombre)
                .containsExactly("Entrantes", "Platos principales");

        verify(categoriaRepo).findAll();
        verifyNoMoreInteractions(categoriaRepo);
    }

    @Test
    void listarCategorias_si_no_hay_categorias_devuelve_lista_vacia() {
        when(categoriaRepo.findAll()).thenReturn(List.of());

        List<Categoria> res = service.listarCategorias();

        assertThat(res).isEmpty();

        verify(categoriaRepo).findAll();
        verifyNoMoreInteractions(categoriaRepo);
    }
}