package cl.gamehub.resenas.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.resenas.client.OrdenClient;
import cl.gamehub.resenas.dto.LineaOrdenInternaDto;
import cl.gamehub.resenas.dto.OrdenInternaDto;
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

/**
 * El servicio completo con su base H2 y sus 16 reseñas de ejemplo; solo order-service es un doble.
 * @Transactional revierte lo que cada prueba guarda, así una no cambia lo que ve la siguiente.
 */
@SpringBootTest
@Transactional
class ResenaControllerTest {

    private static final String RESENA_DE_CAMILA =
            "{\"ordenId\":1,\"productoId\":6,\"puntuacion\":4,\"comentario\":\"Cómodos y con buen sonido.\"}";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JwtServicio jwt;

    @MockitoBean
    private OrdenClient ordenes;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    void lasResenasDeUnProductoSonPublicasYSoloLasAprobadas() throws Exception {
        // La tarjeta gráfica (producto 2) tiene tres reseñas aprobadas: primero la más reciente, de Martina
        mvc.perform(get("/api/productos/2/resenas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].autor").value("Martina C."))
                .andExpect(jsonPath("$[2].autor").value("Fernanda P."));
        // El monitor (producto 7) tiene una aprobada y una pendiente: la pendiente no se ve
        mvc.perform(get("/api/productos/7/resenas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estado").value("APROBADA"));
    }

    @Test
    void elResumenEsPublicoYOmiteLosProductosSinResenasAprobadas() throws Exception {
        // El mouse (producto 5) no tiene reseñas, así que no aparece
        mvc.perform(get("/api/resenas/resumen").param("productos", "2,5,7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productoId").value(2))
                .andExpect(jsonPath("$[0].puntuacion").value(4.7))
                .andExpect(jsonPath("$[0].cantidad").value(3))
                .andExpect(jsonPath("$[1].productoId").value(7))
                .andExpect(jsonPath("$[1].cantidad").value(1));
        // Sin el parámetro: los 11 productos que tienen reseñas aprobadas
        mvc.perform(get("/api/resenas/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11));
    }

    @Test
    void crearExigeSesionYRolCliente() throws Exception {
        // Camila (usuario 3) reseña los audífonos de su orden 1, que ya recibió
        when(ordenes.buscar(1L)).thenReturn(ordenDeEjemplo(1));

        mvc.perform(post("/api/resenas").contentType(MediaType.APPLICATION_JSON).content(RESENA_DE_CAMILA))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/resenas").header("Authorization", tokenDe(1))
                        .contentType(MediaType.APPLICATION_JSON).content(RESENA_DE_CAMILA))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/resenas").header("Authorization", tokenDe(3))
                        .contentType(MediaType.APPLICATION_JSON).content(RESENA_DE_CAMILA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor").value("Camila R."))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        // La misma reseña otra vez choca con la regla de una por producto y orden
        mvc.perform(post("/api/resenas").header("Authorization", tokenDe(3))
                        .contentType(MediaType.APPLICATION_JSON).content(RESENA_DE_CAMILA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya reseñaste este producto en esta orden."));
    }

    @Test
    void unaPuntuacionFueraDeRangoResponde400ConElCampo() throws Exception {
        mvc.perform(post("/api/resenas").header("Authorization", tokenDe(3)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordenId\":1,\"productoId\":6,\"puntuacion\":6,\"comentario\":\"Cómodos y con buen sonido.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.puntuacion").value("La puntuación va de 1 a 5."));
        // La validación corta antes: ni siquiera se le pregunta a order-service
        verifyNoInteractions(ordenes);
    }

    @Test
    void unErrorDeOrderServiceLlegaConSuMensaje() throws Exception {
        // order-service respondió 404: FeignInternoConfig lo entrega como ApiException con el mismo mensaje
        when(ordenes.buscar(99L)).thenThrow(ApiException.noEncontrado("La orden no existe."));

        mvc.perform(post("/api/resenas").header("Authorization", tokenDe(3)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordenId\":99,\"productoId\":6,\"puntuacion\":4,\"comentario\":\"Cómodos y con buen sonido.\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La orden no existe."));
    }

    @Test
    void misResenasSonLasDelClienteDelToken() throws Exception {
        // Camila tiene tres: la del procesador (hace 17 días) y las dos de su compra antigua
        mvc.perform(get("/api/resenas/mias").header("Authorization", tokenDe(3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].ordenId").value(1))
                .andExpect(jsonPath("$[0].productoId").value(3))
                .andExpect(jsonPath("$[0].producto").value("Procesador Tectra R7 de 8 núcleos"));
    }

    @Test
    void laModeracionEsSoloParaAdmin() throws Exception {
        mvc.perform(get("/api/admin/resenas").header("Authorization", tokenDe(3)))
                .andExpect(status().isForbidden());
        // Las tres pendientes, de la más reciente a la más antigua; la de Paula (id 15) trae un teléfono
        mvc.perform(get("/api/admin/resenas").param("estado", "PENDIENTE").header("Authorization", tokenDe(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].id").value(15))
                .andExpect(jsonPath("$[1].autor").value("Paula R."))
                .andExpect(jsonPath("$[1].alerta").value("Contiene datos de contacto"));
        mvc.perform(put("/api/admin/resenas/15").header("Authorization", tokenDe(1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"OCULTA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OCULTA"));
        mvc.perform(put("/api/admin/resenas/15").header("Authorization", tokenDe(1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"PENDIENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Elige aprobar u ocultar la reseña."));
    }

    @Test
    void lasRutasInternasExigenLaClaveInterna() throws Exception {
        // Este servicio no publica rutas internas, pero el filtro de comun igual protege /internal/**
        mvc.perform(get("/internal/resenas"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Ruta interna: solo otros microservicios de la tienda pueden usarla."));
        mvc.perform(get("/internal/resenas").header("X-Clave-Interna", "clave-interna-de-desarrollo-gamehub"))
                .andExpect(status().isNotFound());
    }

    /** Token de un usuario de ejemplo, el mismo que entregaría auth-service: 1 es Ana (ADMIN), 3 es Camila (CLIENTE). */
    private String tokenDe(long usuarioId) {
        DatosDeEjemplo.UsuarioEjemplo u = DatosDeEjemplo.usuario(usuarioId);
        return "Bearer " + jwt.generar(u.id(), u.email(), u.nombre(), Rol.valueOf(u.rol()));
    }

    /** Lo que respondería order-service por una orden de ejemplo: se arma con DatosDeEjemplo, igual que sus datos. */
    private static OrdenInternaDto ordenDeEjemplo(long id) {
        DatosDeEjemplo.OrdenEjemplo o = DatosDeEjemplo.orden(DatosDeEjemplo.base(), id);
        List<LineaOrdenInternaDto> lineas = o.lineas().stream()
                .map(l -> new LineaOrdenInternaDto(l.productoId(), DatosDeEjemplo.producto(l.productoId()).nombre()))
                .toList();
        return new OrdenInternaDto(o.id(), o.numero(), o.usuarioId(), o.cliente().nombre(), o.estadoOrden(),
                o.entregadaEn(), lineas);
    }
}
