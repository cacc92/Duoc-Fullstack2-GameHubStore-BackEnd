package cl.gamehub.ordenes.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.ordenes.client.DespachoClient;
import cl.gamehub.ordenes.client.DisponibilidadDto;
import cl.gamehub.ordenes.client.InventarioClient;
import cl.gamehub.ordenes.client.PagoClient;
import cl.gamehub.ordenes.client.ProductoClient;
import cl.gamehub.ordenes.client.ProductoInternoDto;
import cl.gamehub.ordenes.client.PromocionClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** order-service completo con sus 18 órdenes de ejemplo; los otros cinco servicios son dobles. */
@SpringBootTest
@Transactional
class OrdenControllerTest {

    private static final String CHECKOUT_INVALIDO = """
            {"lineas":[{"productoId":4,"cantidad":1}],"metodoPago":"DEBITO",
             "despacho":{"nombre":"Camila Rojas","correo":"camila@gamehub.test","telefono":"12345",
             "region":"Región Metropolitana de Santiago","comuna":"Ñuñoa","direccion":""}}
            """;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JwtServicio jwt;

    @MockitoBean
    private ProductoClient productos;

    @MockitoBean
    private InventarioClient inventario;

    @MockitoBean
    private PromocionClient promociones;

    @MockitoBean
    private PagoClient pagos;

    @MockitoBean
    private DespachoClient despachos;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    void elCheckoutValidaLosDatosDeDespacho() throws Exception {
        mvc.perform(post("/api/ordenes").header("Authorization", token(3L, Rol.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON).content(CHECKOUT_INVALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['despacho.telefono']").exists())
                .andExpect(jsonPath("$.errores['despacho.direccion']").exists());
    }

    @Test
    void comprarExigeSesionDeCliente() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(CHECKOUT_INVALIDO))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ordenes").header("Authorization", token(2L, Rol.OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(CHECKOUT_INVALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void noSeDespachaUnaOrdenNoPagada() throws Exception {
        // La orden 6 de los datos de ejemplo es una transferencia pendiente
        mvc.perform(post("/api/operador/ordenes/6/despacho").header("Authorization", token(2L, Rol.OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se despacha una orden no pagada."));
    }

    @Test
    void elResumenDelOperadorCuentaLasOrdenesDeEjemplo() throws Exception {
        mvc.perform(get("/api/operador/resumen").header("Authorization", token(2L, Rol.OPERADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagosPorConfirmar").value(2))
                .andExpect(jsonPath("$.despachosPorGenerar").value(1))
                .andExpect(jsonPath("$.enCurso").value(2));
    }

    @Test
    void misOrdenesSonLasCincoDeCamila() throws Exception {
        mvc.perform(get("/api/ordenes/mias").header("Authorization", token(3L, Rol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void laRutaInternaExigeLaClave() throws Exception {
        mvc.perform(get("/internal/ordenes/1")).andExpect(status().isForbidden());
        mvc.perform(get("/internal/ordenes/1").header("X-Clave-Interna", "clave-interna-de-desarrollo-gamehub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(3))
                .andExpect(jsonPath("$.estado").value("ENTREGADA"))
                .andExpect(jsonPath("$.entregadaEn").exists());
    }

    @Test
    void elCarritoSeValidaSinIniciarSesion() throws Exception {
        when(productos.buscar(anyList())).thenReturn(List.of(new ProductoInternoDto(4L, "KB-NIM-TKL", "Teclado mecánico Nimbo TKL", "teclado", 59990, null, true)));
        when(inventario.stock(anyList())).thenReturn(List.of(new DisponibilidadDto(4L, 24)));

        mvc.perform(post("/api/carrito/validar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lineas\":[{\"productoId\":4,\"cantidad\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.subtotal").value(119980));
    }

    private String token(Long id, Rol rol) {
        return "Bearer " + jwt.generar(id, "prueba@gamehub.test", "Prueba", rol);
    }
}
