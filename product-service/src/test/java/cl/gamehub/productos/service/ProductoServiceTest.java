package cl.gamehub.productos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.productos.client.CategoriaClient;
import cl.gamehub.productos.client.InventarioClient;
import cl.gamehub.productos.dto.CategoriaInternaDto;
import cl.gamehub.productos.dto.EspecificacionDto;
import cl.gamehub.productos.dto.ProductoAdminDto;
import cl.gamehub.productos.dto.ProductoDetalleDto;
import cl.gamehub.productos.dto.ProductoInternoDto;
import cl.gamehub.productos.dto.ProductoInventarioSolicitud;
import cl.gamehub.productos.dto.ProductoResumenDto;
import cl.gamehub.productos.dto.ProductoSolicitud;
import cl.gamehub.productos.dto.StockNuevoSolicitud;
import cl.gamehub.productos.model.Especificacion;
import cl.gamehub.productos.model.Producto;
import cl.gamehub.productos.repository.ProductoRepository;
import cl.gamehub.productos.repository.ProductoRepository.ConteoCategoria;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/** Reglas de ProductoService con dobles de prueba: el repositorio, category-service e inventory-service son mocks. */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final CategoriaInternaDto NOTEBOOKS = new CategoriaInternaDto(1L, "Notebooks", "notebooks");
    private static final CategoriaInternaDto PERIFERICOS = new CategoriaInternaDto(4L, "Periféricos", "perifericos");

    @Mock
    private ProductoRepository productos;

    @Mock
    private CategoriaClient categorias;

    @Mock
    private InventarioClient inventario;

    @InjectMocks
    private ProductoService servicio;

    // ---------- Catálogo público ----------

    @Test
    void listarMuestraElPrecioVigenteYLaCategoriaConUnaSolaLlamada() {
        // Given: un notebook en oferta y un teclado sin oferta
        when(categorias.listar()).thenReturn(List.of(NOTEBOOKS, PERIFERICOS));
        when(productos.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(notebook(), teclado()));

        // When
        List<ProductoResumenDto> lista = servicio.listar(null);

        // Then: la oferta vigente trae el precio tachado y su último día; el teclado no
        assertThat(lista).extracting(ProductoResumenDto::categoria).containsExactly("Notebooks", "Periféricos");
        assertThat(lista.getFirst().precio()).isEqualTo(1_149_990);
        assertThat(lista.getFirst().precioNormal()).isEqualTo(1_299_990);
        assertThat(lista.getFirst().ofertaHasta()).isEqualTo(HOY.plusDays(20));
        assertThat(lista.get(1).precioNormal()).isNull();
        assertThat(lista.get(1).ofertaHasta()).isNull();
        verify(categorias, times(1)).listar();
    }

    @Test
    void unaOfertaVencidaVuelveAlPrecioNormal() {
        // Given: la oferta del notebook terminó ayer
        Producto vencida = notebook();
        vencida.setOfertaHasta(HOY.minusDays(1));
        when(categorias.listar()).thenReturn(List.of(NOTEBOOKS));
        when(productos.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(vencida));

        // When: un filtro en blanco equivale a no filtrar
        ProductoResumenDto dto = servicio.listar(" ").getFirst();

        // Then: se cobra el precio normal y ya no se muestra como oferta
        assertThat(dto.precio()).isEqualTo(1_299_990);
        assertThat(dto.precioNormal()).isNull();
        assertThat(dto.ofertaHasta()).isNull();
    }

    @Test
    void listarPorCategoriaBuscaElIdQueCorrespondeAlSlug() {
        // Given
        when(categorias.listar()).thenReturn(List.of(NOTEBOOKS, PERIFERICOS));
        when(productos.findByActivoTrueAndCategoriaIdOrderByIdAsc(4L)).thenReturn(List.of(teclado()));

        // When
        List<ProductoResumenDto> lista = servicio.listar("perifericos");

        // Then
        assertThat(lista).extracting(ProductoResumenDto::sku).containsExactly("KB-NIM-TKL");
        assertThat(lista.getFirst().categoriaSlug()).isEqualTo("perifericos");
    }

    @Test
    void unSlugDesconocidoDevuelveUnaListaVacia() {
        // Given
        when(categorias.listar()).thenReturn(List.of(NOTEBOOKS, PERIFERICOS));

        // When / Then: ni siquiera se consulta la base
        assertThat(servicio.listar("sillas-gamer")).isEmpty();
        verify(productos, never()).findByActivoTrueAndCategoriaIdOrderByIdAsc(anyLong());
    }

    @Test
    void unProductoCuyaCategoriaNoLlegaDeCategoryServiceSeMuestraIgual() {
        // Given: category-service no trae la categoría 1
        when(categorias.listar()).thenReturn(List.of(PERIFERICOS));
        when(productos.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(notebook()));

        // When
        ProductoResumenDto dto = servicio.listar(null).getFirst();

        // Then
        assertThat(dto.sku()).isEqualTo("NB-KAI-B15");
        assertThat(dto.categoria()).isNull();
    }

    @Test
    void elDetalleDeUnProductoInactivoOInexistenteResponde404() {
        // Given
        Producto inactivo = teclado();
        inactivo.setActivo(false);
        when(productos.findById(4L)).thenReturn(Optional.of(inactivo));
        when(productos.findById(99L)).thenReturn(Optional.empty());

        // When / Then: el visitante recibe el mismo mensaje en los dos casos
        for (long id : new long[] {4L, 99L}) {
            assertThatThrownBy(() -> servicio.detalle(id))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("El producto no existe o ya no está disponible.")
                    .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
        }
        verifyNoInteractions(categorias);
    }

    @Test
    void elDetalleTraeLaDescripcionYLaFichaTecnica() {
        // Given
        Producto notebook = notebook();
        notebook.setDescripcion("Notebook de 15,6 pulgadas.");
        notebook.getEspecificaciones().add(new Especificacion("Memoria", "16 GB DDR5"));
        when(productos.findById(1L)).thenReturn(Optional.of(notebook));
        when(categorias.listar()).thenReturn(List.of(NOTEBOOKS));

        // When
        ProductoDetalleDto detalle = servicio.detalle(1L);

        // Then
        assertThat(detalle.categoria()).isEqualTo("Notebooks");
        assertThat(detalle.categoriaSlug()).isEqualTo("notebooks");
        assertThat(detalle.descripcion()).isEqualTo("Notebook de 15,6 pulgadas.");
        assertThat(detalle.especificaciones()).containsExactly(new EspecificacionDto("Memoria", "16 GB DDR5"));
        assertThat(detalle.precioNormal()).isEqualTo(1_299_990);
    }

    // ---------- Administración ----------

    @Test
    void elMantenedorMuestraLosPreciosGuardadosAunqueLaOfertaHayaVencido() {
        // Given
        Producto vencida = notebook();
        vencida.setOfertaHasta(HOY.minusDays(1));
        when(categorias.listar()).thenReturn(List.of(NOTEBOOKS));
        when(productos.findAllByOrderByIdAsc()).thenReturn(List.of(vencida));

        // When
        ProductoAdminDto dto = servicio.listarTodos().getFirst();

        // Then
        assertThat(dto.precio()).isEqualTo(1_149_990);
        assertThat(dto.precioNormal()).isEqualTo(1_299_990);
        assertThat(dto.ofertaHasta()).isEqualTo(HOY.minusDays(1));
        assertThat(dto.categoriaId()).isEqualTo(1L);
        assertThat(dto.categoria()).isEqualTo("Notebooks");
    }

    @Test
    void crearRechazaUnSkuRepetidoAunqueVengaEnMinusculas() {
        // Given
        when(productos.existsBySkuIgnoreCase("KB-NIM-TKL")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> servicio.crear(solicitud(" kb-nim-tkl ", 4L, 59_990, null, 5)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya existe un producto con el SKU KB-NIM-TKL.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verify(productos, never()).save(any());
        verifyNoInteractions(inventario);
    }

    @Test
    void crearConvierteEl404DeCategoryServiceEnUn400() {
        // Given: category-service responde 404 y Feign lo entrega como ApiException
        when(productos.existsBySkuIgnoreCase("KB-NIM-MINI")).thenReturn(false);
        when(categorias.buscar(99L)).thenThrow(ApiException.noEncontrado("La categoría no existe."));

        // When / Then: para el formulario es un dato inválido
        assertThatThrownBy(() -> servicio.crear(solicitud("KB-NIM-MINI", 99L, 49_990, null, 5)))
                .isInstanceOf(ApiException.class)
                .hasMessage("La categoría elegida no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(productos, never()).save(any());
    }

    @Test
    void siCategoryServiceNoRespondeElErrorLlegaTalCual() {
        // Given
        when(productos.existsBySkuIgnoreCase("KB-NIM-MINI")).thenReturn(false);
        when(categorias.buscar(4L)).thenThrow(ApiException.noDisponible("category-service no responde."));

        // When / Then
        assertThatThrownBy(() -> servicio.crear(solicitud("KB-NIM-MINI", 4L, 49_990, null, 5)))
                .hasMessage("category-service no responde.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void elPrecioNormalDebeSerMayorQueElDeOferta() {
        // Given
        when(productos.existsBySkuIgnoreCase("KB-NIM-MINI")).thenReturn(false);
        when(categorias.buscar(4L)).thenReturn(PERIFERICOS);

        // When / Then
        assertThatThrownBy(() -> servicio.crear(solicitud("KB-NIM-MINI", 4L, 49_990, 49_990, 5)))
                .isInstanceOf(ApiException.class)
                .hasMessage("El precio normal debe ser mayor que el precio de oferta.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(productos, never()).save(any());
    }

    @Test
    void crearGuardaElProductoYPideAInventarioSuStockInicial() {
        // Given
        when(productos.existsBySkuIgnoreCase("KB-NIM-MINI")).thenReturn(false);
        when(categorias.buscar(4L)).thenReturn(PERIFERICOS);
        when(productos.save(any(Producto.class))).thenAnswer(i -> conId(i.getArgument(0), 14L));

        // When: SKU en minúsculas, sin imagen, resumen en blanco y en oferta
        ProductoAdminDto creado = servicio.crear(solicitud(" kb-nim-mini ", 4L, 49_990, 54_990, 5));

        // Then
        assertThat(creado.id()).isEqualTo(14L);
        assertThat(creado.sku()).isEqualTo("KB-NIM-MINI");
        assertThat(creado.nombre()).isEqualTo("Teclado Nimbo Mini");
        assertThat(creado.marca()).isEqualTo("Nimbo");
        assertThat(creado.categoria()).isEqualTo("Periféricos");
        assertThat(creado.imagen()).isEqualTo("teclado");
        assertThat(creado.resumen()).isNull();
        assertThat(creado.ofertaHasta()).isEqualTo(HOY.plusDays(10));
        assertThat(creado.especificaciones()).containsExactly(new EspecificacionDto("Formato", "60 %"));
        ArgumentCaptor<StockNuevoSolicitud> aviso = ArgumentCaptor.forClass(StockNuevoSolicitud.class);
        verify(inventario).crearStock(aviso.capture());
        assertThat(aviso.getValue()).isEqualTo(new StockNuevoSolicitud(14L, "KB-NIM-MINI", "Teclado Nimbo Mini", true, 5));
    }

    @Test
    void sinPrecioNormalNoHayOfertaYSinStockSeInformaCero() {
        // Given
        when(productos.existsBySkuIgnoreCase("KB-NIM-MINI")).thenReturn(false);
        when(categorias.buscar(4L)).thenReturn(PERIFERICOS);
        when(productos.save(any(Producto.class))).thenAnswer(i -> conId(i.getArgument(0), 14L));

        // When: trae fecha de oferta, pero no precio normal
        ProductoAdminDto creado = servicio.crear(solicitud("KB-NIM-MINI", 4L, 49_990, null, null));

        // Then
        assertThat(creado.precioNormal()).isNull();
        assertThat(creado.ofertaHasta()).isNull();
        verify(inventario).crearStock(new StockNuevoSolicitud(14L, "KB-NIM-MINI", "Teclado Nimbo Mini", true, 0));
    }

    @Test
    void siInventarioFallaLaCreacionTerminaConError() {
        // Given: inventory-service no responde; la excepción hace que @Transactional revierta el producto
        when(productos.existsBySkuIgnoreCase("KB-NIM-MINI")).thenReturn(false);
        when(categorias.buscar(4L)).thenReturn(PERIFERICOS);
        when(productos.save(any(Producto.class))).thenAnswer(i -> conId(i.getArgument(0), 14L));
        doThrow(ApiException.noDisponible("inventory-service no responde.")).when(inventario).crearStock(any());

        // When / Then
        assertThatThrownBy(() -> servicio.crear(solicitud("KB-NIM-MINI", 4L, 49_990, null, 5)))
                .isInstanceOf(ApiException.class)
                .hasMessage("inventory-service no responde.");
    }

    @ParameterizedTest
    @CsvSource({
        "notebooks, notebook", "procesadores, cpu", "perifericos, teclado", "consolas, consola",
        "monitores, monitor", "accesorios, ssd", "tarjetas-graficas, gpu", "sillas-gamer, gpu"
    })
    void sinImagenSeUsaLaIlustracionDeLaCategoria(String slug, String imagen) {
        // Given
        when(productos.existsBySkuIgnoreCase("NUEVO-1")).thenReturn(false);
        when(categorias.buscar(9L)).thenReturn(new CategoriaInternaDto(9L, "Categoría", slug));
        when(productos.save(any(Producto.class))).thenAnswer(i -> conId(i.getArgument(0), 14L));

        // When
        ProductoAdminDto creado = servicio.crear(solicitud("NUEVO-1", 9L, 10_000, null, 0));

        // Then
        assertThat(creado.imagen()).isEqualTo(imagen);
    }

    @Test
    void actualizarResponde404SiElProductoNoExiste() {
        // Given
        when(productos.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> servicio.actualizar(99L, solicitud("KB-NIM-TKL", 4L, 59_990, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("El producto no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void actualizarRechazaElSkuDeOtroProducto() {
        // Given
        when(productos.findById(4L)).thenReturn(Optional.of(teclado()));
        when(productos.existsBySkuIgnoreCaseAndIdNot("MS-NIM-AERO", 4L)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> servicio.actualizar(4L, solicitud("ms-nim-aero", 4L, 59_990, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya existe otro producto con el SKU MS-NIM-AERO.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verifyNoInteractions(inventario);
    }

    @Test
    void actualizarCambiaLosDatosYAvisaAInventarioSinTocarElStock() {
        // Given: nuevo SKU, foto propia, sin precio normal y sin especificaciones
        when(productos.findById(4L)).thenReturn(Optional.of(teclado()));
        when(productos.existsBySkuIgnoreCaseAndIdNot("KB-NIM-TKL2", 4L)).thenReturn(false);
        when(categorias.buscar(4L)).thenReturn(PERIFERICOS);
        ProductoSolicitud cambios = new ProductoSolicitud("kb-nim-tkl2", "Teclado Nimbo TKL v2", "Nimbo", 4L, 64_990,
                null, HOY.plusDays(5), 99, " Switches marrones ", null, " https://cdn.gamehub.test/tkl2.png ", null);

        // When
        ProductoAdminDto dto = servicio.actualizar(4L, cambios);

        // Then: el stock que trae el formulario no viaja a inventory-service
        assertThat(dto.sku()).isEqualTo("KB-NIM-TKL2");
        assertThat(dto.precio()).isEqualTo(64_990);
        assertThat(dto.ofertaHasta()).isNull();
        assertThat(dto.resumen()).isEqualTo("Switches marrones");
        assertThat(dto.descripcion()).isNull();
        assertThat(dto.imagen()).isEqualTo("https://cdn.gamehub.test/tkl2.png");
        assertThat(dto.especificaciones()).isEmpty();
        verify(inventario).actualizarProducto(4L, new ProductoInventarioSolicitud("KB-NIM-TKL2", "Teclado Nimbo TKL v2", true));
        verify(inventario, never()).crearStock(any());
    }

    @Test
    void cambiarActivoAvisaAInventario() {
        // Given
        when(productos.findById(4L)).thenReturn(Optional.of(teclado()));
        when(categorias.listar()).thenReturn(List.of(PERIFERICOS));

        // When
        ProductoAdminDto dto = servicio.cambiarActivo(4L, false);

        // Then
        assertThat(dto.activo()).isFalse();
        assertThat(dto.categoria()).isEqualTo("Periféricos");
        verify(inventario).actualizarProducto(4L,
                new ProductoInventarioSolicitud("KB-NIM-TKL", "Teclado mecánico Nimbo TKL", false));
    }

    // ---------- Para otros microservicios ----------

    @Test
    void buscarVariosEntregaElPrecioVigenteYOmiteLosQueNoExisten() {
        // Given: se piden el 1, el 4 y el 99; el 99 no existe y el 4 está inactivo
        Producto inactivo = teclado();
        inactivo.setActivo(false);
        when(productos.findByIdInOrderByIdAsc(List.of(1L, 4L, 99L))).thenReturn(List.of(notebook(), inactivo));

        // When
        List<ProductoInternoDto> lista = servicio.buscarVarios(List.of(1L, 4L, 99L));

        // Then
        assertThat(lista).containsExactly(
                new ProductoInternoDto(1L, "NB-KAI-B15", "Notebook gamer Kairo Blade 15", "notebook", 1_149_990, 1_299_990, true),
                new ProductoInternoDto(4L, "KB-NIM-TKL", "Teclado mecánico Nimbo TKL", "teclado", 59_990, null, false));
    }

    @Test
    void buscarVariosSinIdsNoConsultaLaBase() {
        assertThat(servicio.buscarVarios(List.of())).isEmpty();
        verifyNoInteractions(productos);
    }

    @Test
    void elConteoPorCategoriaQuedaOrdenadoPorId() {
        // Given
        when(productos.contarPorCategoria(true)).thenReturn(List.of(fila(4L, 4L), fila(1L, 1L)));

        // When
        Map<Long, Long> conteo = servicio.conteoPorCategoria(true);

        // Then
        assertThat(conteo).containsExactly(entry(1L, 1L), entry(4L, 4L));
    }

    // ---------- Datos de prueba ----------

    /** Notebook en oferta: $1.149.990 en vez de $1.299.990 hasta dentro de 20 días. */
    private static Producto notebook() {
        Producto p = new Producto("NB-KAI-B15", "Notebook gamer Kairo Blade 15", "Kairo", 1L, 1_149_990);
        p.setPrecioNormal(1_299_990);
        p.setOfertaHasta(HOY.plusDays(20));
        p.setImagen("notebook");
        return conId(p, 1L);
    }

    private static Producto teclado() {
        Producto p = new Producto("KB-NIM-TKL", "Teclado mecánico Nimbo TKL", "Nimbo", 4L, 59_990);
        p.setImagen("teclado");
        p.getEspecificaciones().add(new Especificacion("Formato", "TKL (87 teclas)"));
        return conId(p, 4L);
    }

    private static Producto conId(Producto p, Long id) {
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    /** Formulario con espacios de más, sin imagen, con el resumen en blanco y oferta hasta dentro de 10 días. */
    private static ProductoSolicitud solicitud(String sku, Long categoriaId, Integer precio, Integer precioNormal,
                                               Integer stock) {
        return new ProductoSolicitud(sku, " Teclado Nimbo Mini ", " Nimbo ", categoriaId, precio, precioNormal,
                HOY.plusDays(10), stock, "  ", "Teclado compacto de 61 teclas.", null,
                List.of(new EspecificacionDto(" Formato ", " 60 % ")));
    }

    private static ConteoCategoria fila(Long categoriaId, Long cantidad) {
        return new ConteoCategoria() {
            @Override
            public Long getCategoriaId() {
                return categoriaId;
            }

            @Override
            public Long getCantidad() {
                return cantidad;
            }
        };
    }
}
