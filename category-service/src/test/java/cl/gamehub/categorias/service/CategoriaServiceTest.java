package cl.gamehub.categorias.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.gamehub.categorias.client.ProductoClient;
import cl.gamehub.categorias.dto.CategoriaDto;
import cl.gamehub.categorias.dto.CategoriaSolicitud;
import cl.gamehub.categorias.model.Categoria;
import cl.gamehub.categorias.repository.CategoriaRepository;
import cl.gamehub.comun.error.ApiException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/** Reglas de CategoriaService con dobles de prueba: el repositorio y product-service son mocks. */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categorias;

    @Mock
    private ProductoClient productos;

    @InjectMocks
    private CategoriaService servicio;

    @Test
    void listarJuntaLasCategoriasConElConteoDeProductService() {
        // Given: dos categorías y product-service dice que la 1 tiene 3 productos activos
        when(categorias.findAllByOrderByIdAsc()).thenReturn(List.of(categoria(1L, "Notebooks"), categoria(2L, "Consolas")));
        when(productos.conteoPorCategoria(true)).thenReturn(Map.of(1L, 3L));

        // When
        List<CategoriaDto> lista = servicio.listar(true);

        // Then: la que no aparece en el conteo queda con 0
        assertThat(lista).extracting(CategoriaDto::cantidadProductos).containsExactly(3L, 0L);
        assertThat(lista.getFirst().slug()).isEqualTo("notebooks");
    }

    @Test
    void crearRechazaUnNombreRepetido() {
        when(categorias.existsByNombreIgnoreCase("Consolas")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(new CategoriaSolicitud(" Consolas ")))
                .isInstanceOf(ApiException.class)
                .hasMessage("La categoría «Consolas» ya existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verify(categorias, never()).save(any());
    }

    @Test
    void crearGuardaLaCategoriaConSuSlug() {
        when(categorias.existsByNombreIgnoreCase("Sillas gamer")).thenReturn(false);
        when(categorias.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));

        CategoriaDto creada = servicio.crear(new CategoriaSolicitud("Sillas gamer"));

        assertThat(creada.slug()).isEqualTo("sillas-gamer");
        assertThat(creada.cantidadProductos()).isZero();
    }

    @Test
    void noSeEliminaUnaCategoriaConProductos() {
        when(categorias.findById(4L)).thenReturn(Optional.of(categoria(4L, "Periféricos")));
        when(productos.conteoPorCategoria(false)).thenReturn(Map.of(4L, 5L));

        assertThatThrownBy(() -> servicio.eliminar(4L))
                .isInstanceOf(ApiException.class)
                .hasMessage("No puedes eliminar una categoría que tiene productos.");
        verify(categorias, never()).delete(any());
    }

    @Test
    void seEliminaUnaCategoriaSinProductos() {
        Categoria vacia = categoria(8L, "Sillas gamer");
        when(categorias.findById(8L)).thenReturn(Optional.of(vacia));
        when(productos.conteoPorCategoria(false)).thenReturn(Map.of(4L, 5L));

        servicio.eliminar(8L);

        verify(categorias).delete(vacia);
    }

    @Test
    void renombrarActualizaElSlugYRechazaDuplicados() {
        Categoria accesorios = categoria(7L, "Accesorios");
        when(categorias.findById(7L)).thenReturn(Optional.of(accesorios));
        when(categorias.existsByNombreIgnoreCaseAndIdNot("Accesorios PC", 7L)).thenReturn(false);
        when(productos.conteoPorCategoria(false)).thenReturn(Map.of(7L, 2L));

        CategoriaDto renombrada = servicio.renombrar(7L, new CategoriaSolicitud("Accesorios PC"));

        assertThat(renombrada.slug()).isEqualTo("accesorios-pc");
        assertThat(renombrada.cantidadProductos()).isEqualTo(2L);

        when(categorias.existsByNombreIgnoreCaseAndIdNot("Consolas", 7L)).thenReturn(true);
        assertThatThrownBy(() -> servicio.renombrar(7L, new CategoriaSolicitud("Consolas")))
                .hasMessage("La categoría «Consolas» ya existe.");
    }

    @Test
    void buscarInternoResponde404SiNoExiste() {
        when(categorias.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscarInterno(99L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static Categoria categoria(Long id, String nombre) {
        Categoria c = new Categoria(nombre);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
