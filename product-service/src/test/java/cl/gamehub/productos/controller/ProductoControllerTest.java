package cl.gamehub.productos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.productos.client.CategoriaClient;
import cl.gamehub.productos.client.InventarioClient;
import cl.gamehub.productos.dto.CategoriaInternaDto;
import cl.gamehub.productos.dto.ProductoInventarioSolicitud;
import cl.gamehub.productos.dto.StockNuevoSolicitud;
import cl.gamehub.productos.repository.ProductoRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * El servicio completo con su base H2 y sus 13 productos de ejemplo; category-service e inventory-service
 * son dobles. @Transactional revierte lo que cada prueba guarda, así una no cambia lo que ve la siguiente.
 */
@SpringBootTest
@Transactional
class ProductoControllerTest {

    private static final String CLAVE_INTERNA = "clave-interna-de-desarrollo-gamehub";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JwtServicio jwt;

    @Autowired
    private ProductoRepository productos;

    @MockitoBean
    private CategoriaClient categorias;

    @MockitoBean
    private InventarioClient inventario;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        // El doble de category-service responde con las 7 categorías de ejemplo; la 99 no existe
        List<CategoriaInternaDto> lista = DatosDeEjemplo.CATEGORIAS.stream()
                .map(c -> new CategoriaInternaDto(c.id(), c.nombre(), Formatos.slug(c.nombre())))
                .toList();
        when(categorias.listar()).thenReturn(lista);
        lista.forEach(c -> when(categorias.buscar(c.id())).thenReturn(c));
        when(categorias.buscar(99L)).thenThrow(ApiException.noEncontrado("La categoría no existe."));
    }

    @Test
    void elCatalogoEsPublicoYSoloMuestraLosProductosActivos() throws Exception {
        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].sku").value("NB-KAI-B15"))
                .andExpect(jsonPath("$[0].categoria").value("Notebooks"))
                .andExpect(jsonPath("$[0].precio").value(1_149_990))
                .andExpect(jsonPath("$[0].precioNormal").value(1_299_990))
                .andExpect(jsonPath("$[0].ofertaHasta").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}")))
                .andExpect(jsonPath("$[2].precioNormal").value(nullValue()))
                .andExpect(jsonPath("$[2].ofertaHasta").value(nullValue()));
        mvc.perform(get("/api/productos").param("categoria", "perifericos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].marca").value("Nimbo"));
        mvc.perform(get("/api/productos").param("categoria", "sillas-gamer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void elDetalleTraeLaFichaTecnicaYUnProductoInactivoResponde404() throws Exception {
        mvc.perform(get("/api/productos/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marca").value("Voltra"))
                .andExpect(jsonPath("$.categoriaSlug").value("tarjetas-graficas"))
                .andExpect(jsonPath("$.especificaciones.length()").value(9))
                .andExpect(jsonPath("$.especificaciones[0].clave").value("Memoria"));
        mvc.perform(get("/api/productos/13"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El producto no existe o ya no está disponible."));
    }

    @Test
    void crearExigeSesionYRolAdminYCreaElStockEnInventario() throws Exception {
        String cuerpo = """
                {"sku": "kb-nim-mini", "nombre": "Teclado Nimbo Mini", "marca": "Nimbo", "categoriaId": 4,
                 "precio": 49990, "stock": 5, "especificaciones": [{"clave": "Formato", "valor": "60 %"}]}""";
        mvc.perform(post("/api/admin/productos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/productos").header("Authorization", token(Rol.OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/productos").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("KB-NIM-MINI"))
                .andExpect(jsonPath("$.categoria").value("Periféricos"))
                .andExpect(jsonPath("$.imagen").value("teclado"));

        ArgumentCaptor<StockNuevoSolicitud> aviso = ArgumentCaptor.forClass(StockNuevoSolicitud.class);
        verify(inventario).crearStock(aviso.capture());
        assertThat(aviso.getValue().sku()).isEqualTo("KB-NIM-MINI");
        assertThat(aviso.getValue().stockInicial()).isEqualTo(5);
        assertThat(productos.existsBySkuIgnoreCase("KB-NIM-MINI")).isTrue();
    }

    @Test
    void unaSolicitudInvalidaResponde400ConElCampo() throws Exception {
        String cuerpo = """
                {"sku": "KB-NIM-MINI", "nombre": "Teclado Nimbo Mini", "marca": "Nimbo", "categoriaId": 4, "precio": 0}""";
        mvc.perform(post("/api/admin/productos").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").value("El precio debe ser mayor que cero."));
    }

    @Test
    void unaCategoriaQueNoExisteResponde400() throws Exception {
        String cuerpo = """
                {"sku": "KB-NIM-MINI", "nombre": "Teclado Nimbo Mini", "marca": "Nimbo", "categoriaId": 99, "precio": 49990}""";
        mvc.perform(post("/api/admin/productos").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La categoría elegida no existe."));
    }

    @Test
    void editarUnProductoAvisaAInventarioYNoTocaElStock() throws Exception {
        String cuerpo = """
                {"sku": "kb-nim-tkl", "nombre": "Teclado mecánico Nimbo TKL v2", "marca": "Nimbo", "categoriaId": 4,
                 "precio": 54990, "precioNormal": 59990, "ofertaHasta": "2099-12-31", "stock": 999}""";
        mvc.perform(put("/api/admin/productos/4").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Teclado mecánico Nimbo TKL v2"))
                .andExpect(jsonPath("$.precioNormal").value(59990))
                .andExpect(jsonPath("$.ofertaHasta").value("2099-12-31"));
        verify(inventario).actualizarProducto(4L,
                new ProductoInventarioSolicitud("KB-NIM-TKL", "Teclado mecánico Nimbo TKL v2", true));
        verify(inventario, never()).crearStock(any());
    }

    @Test
    void elAdministradorVeLosInactivosYPuedeDesactivarUnProducto() throws Exception {
        mvc.perform(get("/api/admin/productos").header("Authorization", token(Rol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[12].sku").value("CTL-AUS-PAD25"))
                .andExpect(jsonPath("$[12].activo").value(false));
        mvc.perform(patch("/api/admin/productos/5/activo").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        verify(inventario).actualizarProducto(5L,
                new ProductoInventarioSolicitud("MS-NIM-AERO", "Mouse inalámbrico Nimbo Aero", false));
        mvc.perform(get("/api/productos/5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void lasRutasInternasExigenLaClaveInterna() throws Exception {
        mvc.perform(get("/internal/productos").param("ids", "1,13,99"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/internal/productos").param("ids", "1,13,99").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].precio").value(1_149_990))
                .andExpect(jsonPath("$[0].precioNormal").value(1_299_990))
                .andExpect(jsonPath("$[1].activo").value(false));
        mvc.perform(get("/internal/productos/conteo").param("soloActivos", "true").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['4']").value(4))
                .andExpect(jsonPath("$['5']").value(2));
        mvc.perform(get("/internal/productos/conteo").param("soloActivos", "false").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['5']").value(3));
    }

    /** Sin la transacción de la prueba, para ver el rollback real que hace el servicio. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void siInventarioFallaElProductoNoQuedaGuardado() throws Exception {
        doThrow(ApiException.noDisponible("Uno de los servicios de la tienda no responde."))
                .when(inventario).crearStock(any());
        String cuerpo = """
                {"sku": "SIN-STOCK-1", "nombre": "Producto sin stock", "marca": "Nimbo", "categoriaId": 4, "precio": 9990}""";

        mvc.perform(post("/api/admin/productos").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isServiceUnavailable());

        assertThat(productos.existsBySkuIgnoreCase("SIN-STOCK-1")).isFalse();
    }

    private String token(Rol rol) {
        return "Bearer " + jwt.generar(1L, "prueba@gamehub.test", "Prueba", rol);
    }
}
