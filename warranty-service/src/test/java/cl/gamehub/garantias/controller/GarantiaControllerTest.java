package cl.gamehub.garantias.controller;

import static org.hamcrest.Matchers.matchesPattern;
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
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.garantias.client.OrdenClient;
import cl.gamehub.garantias.dto.LineaOrdenInternaDto;
import cl.gamehub.garantias.dto.OrdenInternaDto;
import cl.gamehub.garantias.model.Garantia;
import java.time.LocalDate;
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
 * El servicio completo con su base H2 y sus 3 garantías de ejemplo; solo order-service es un doble.
 * @Transactional revierte lo que cada prueba guarda, así una no cambia lo que ve la siguiente.
 */
@SpringBootTest
@Transactional
class GarantiaControllerTest {

    /** Martina pide garantía de la tarjeta gráfica de su orden 10 (los audífonos ya tienen una abierta). */
    private static final String SOLICITUD_DE_MARTINA = "{\"ordenId\":10,\"productoId\":2,\"motivo\":\"FALLA\","
            + "\"descripcion\":\"La tarjeta se apaga sola a los pocos minutos de jugar.\"}";

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
    void crearExigeSesionYRolCliente() throws Exception {
        when(ordenes.buscar(10L)).thenReturn(ordenDeEjemplo(10));
        DatosDeEjemplo.OrdenEjemplo orden = DatosDeEjemplo.orden(DatosDeEjemplo.base(), 10);

        mvc.perform(post("/api/garantias").contentType(MediaType.APPLICATION_JSON).content(SOLICITUD_DE_MARTINA))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/garantias").header("Authorization", tokenDe(2))
                        .contentType(MediaType.APPLICATION_JSON).content(SOLICITUD_DE_MARTINA))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/garantias").header("Authorization", tokenDe(5))
                        .contentType(MediaType.APPLICATION_JSON).content(SOLICITUD_DE_MARTINA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(matchesPattern("GAR-\\d{4}")))
                .andExpect(jsonPath("$.ordenNumero").value(orden.numero()))
                .andExpect(jsonPath("$.cliente").value("Martina Castro"))
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.plazo").value(Garantia.plazoDesde(orden.entregadaEn()).toString()));
    }

    @Test
    void unaDescripcionCortaResponde400ConElCampo() throws Exception {
        mvc.perform(post("/api/garantias").header("Authorization", tokenDe(5)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordenId\":10,\"productoId\":2,\"motivo\":\"FALLA\",\"descripcion\":\"No funciona.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").value("Describe el problema con al menos 20 caracteres."));
        // La validación corta antes: ni siquiera se le pregunta a order-service
        verifyNoInteractions(ordenes);
    }

    @Test
    void unaSegundaSolicitudAbiertaResponde409() throws Exception {
        // Los audífonos (producto 6) de la orden 10 ya tienen la garantía GAR-0001 abierta
        when(ordenes.buscar(10L)).thenReturn(ordenDeEjemplo(10));

        mvc.perform(post("/api/garantias").header("Authorization", tokenDe(5)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordenId\":10,\"productoId\":6,\"motivo\":\"FALLA\","
                                + "\"descripcion\":\"Ahora también falla el lado derecho.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya hay una solicitud abierta para este producto."));
    }

    @Test
    void unPlazoVencidoRespondeConLaFecha() throws Exception {
        // La compra antigua de Camila (orden 4) se entregó hace más de 6 meses
        when(ordenes.buscar(4L)).thenReturn(ordenDeEjemplo(4));
        LocalDate plazo = Garantia.plazoDesde(DatosDeEjemplo.orden(DatosDeEjemplo.base(), 4).entregadaEn());

        mvc.perform(post("/api/garantias").header("Authorization", tokenDe(3)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordenId\":4,\"productoId\":11,\"motivo\":\"FALLA\","
                                + "\"descripcion\":\"El micrófono dejó de captar la voz.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El plazo de garantía venció el " + Formatos.fecha(plazo) + "."));
    }

    @Test
    void unErrorDeOrderServiceLlegaConSuMensaje() throws Exception {
        // order-service respondió 404: FeignInternoConfig lo entrega como ApiException con el mismo mensaje
        when(ordenes.buscar(10L)).thenThrow(ApiException.noEncontrado("La orden no existe."));

        mvc.perform(post("/api/garantias").header("Authorization", tokenDe(5))
                        .contentType(MediaType.APPLICATION_JSON).content(SOLICITUD_DE_MARTINA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La orden no existe."));
    }

    @Test
    void misGarantiasSonLasDelClienteDelToken() throws Exception {
        // Sofía (usuario 17) tiene la del monitor, ya resuelta
        mvc.perform(get("/api/garantias/mias").header("Authorization", tokenDe(17)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("GAR-0002"))
                .andExpect(jsonPath("$[0].estado").value("RESUELTA"))
                .andExpect(jsonPath("$[0].resolucion")
                        .value("Se reemplazó por una unidad nueva y se envió un cable DisplayPort certificado."));
    }

    @Test
    void laBandejaDelOperadorMuestraPrimeroLasAbiertas() throws Exception {
        mvc.perform(get("/api/operador/garantias").header("Authorization", tokenDe(5)))
                .andExpect(status().isForbidden());
        // GAR-0001 (abierta, hace 2 días), GAR-0003 (en revisión, hace 4) y al final GAR-0002 (resuelta)
        mvc.perform(get("/api/operador/garantias").header("Authorization", tokenDe(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("GAR-0001"))
                .andExpect(jsonPath("$[1].codigo").value("GAR-0003"))
                .andExpect(jsonPath("$[2].codigo").value("GAR-0002"));
        // El administrador también entra al panel del operador
        mvc.perform(get("/api/operador/garantias/resumen").header("Authorization", tokenDe(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abiertas").value(2));
    }

    @Test
    void resolverExigeResolucionParaCerrar() throws Exception {
        // El operador resuelve la de Martina (GAR-0001) explicando qué se hizo
        mvc.perform(put("/api/operador/garantias/1").header("Authorization", tokenDe(2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RESUELTA\",\"resolucion\":\"Se cambiaron los audífonos por unos nuevos.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTA"))
                .andExpect(jsonPath("$.actualizadaEn").isNotEmpty());
        // Rechazar la de Diego (GAR-0003) sin escribir la resolución no se permite
        mvc.perform(put("/api/operador/garantias/3").header("Authorization", tokenDe(2))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"RECHAZADA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Una garantía no se cierra sin resolución. Escribe qué se hizo."));
        // La de Sofía (GAR-0002) ya está cerrada
        mvc.perform(put("/api/operador/garantias/2").header("Authorization", tokenDe(2))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_REVISION\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Esta garantía ya está cerrada."));
    }

    @Test
    void lasRutasInternasExigenLaClaveInterna() throws Exception {
        // Este servicio no publica rutas internas, pero el filtro de comun igual protege /internal/**
        mvc.perform(get("/internal/garantias"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Ruta interna: solo otros microservicios de la tienda pueden usarla."));
        mvc.perform(get("/internal/garantias").header("X-Clave-Interna", "clave-interna-de-desarrollo-gamehub"))
                .andExpect(status().isNotFound());
    }

    /** Token de un usuario de ejemplo: 1 es Ana (ADMIN), 2 es Luis (OPERADOR) y del 3 en adelante son clientes. */
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
