package cl.gamehub.despachos.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.OrdenEjemplo;
import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.Rol;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * El servicio completo con su base H2 y los 13 despachos de ejemplo. No hay dobles: este servicio no llama a otros.
 * @Transactional revierte lo que cada prueba guarda, así una no cambia lo que ve la siguiente.
 */
@SpringBootTest
@Transactional
class DespachoControllerTest {

    private static final String CLAVE_INTERNA = "clave-interna-de-desarrollo-gamehub";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JwtServicio jwt;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    void elSeguimientoEsPublicoYNoMuestraDatosPersonales() throws Exception {
        // La orden 1 de ejemplo se entregó: 4 eventos con las fechas de DatosDeEjemplo
        OrdenEjemplo primera = DatosDeEjemplo.orden(DatosDeEjemplo.base(), 1);
        mvc.perform(get("/api/despachos/seguimiento/ghx-61873"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordenId").value(1))
                .andExpect(jsonPath("$.ordenNumero").value(primera.numero()))
                .andExpect(jsonPath("$.estado").value("ENTREGADO"))
                .andExpect(jsonPath("$.fechaEstimada").value(primera.fechaEstimada().toString()))
                .andExpect(jsonPath("$.entregadoEn").value(iso(primera.entregadaEn())))
                .andExpect(jsonPath("$.eventos.length()").value(4))
                .andExpect(jsonPath("$.eventos[0].estado").value("PREPARANDO"))
                .andExpect(jsonPath("$.eventos[0].fecha").value(iso(primera.eventos().getFirst().fecha())))
                .andExpect(jsonPath("$.destinatario").doesNotExist())
                .andExpect(jsonPath("$.direccion").doesNotExist());
        mvc.perform(get("/api/despachos/seguimiento/GHX-00000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos un despacho con el seguimiento GHX-00000."));
    }

    @Test
    void laBandejaDelOperadorExigeSesionYRol() throws Exception {
        mvc.perform(get("/api/operador/despachos"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/operador/despachos").header("Authorization", token(Rol.CLIENTE)))
                .andExpect(status().isForbidden());
        // Los 13 despachos, del más nuevo (orden 8, en preparación desde ayer) al más antiguo
        mvc.perform(get("/api/operador/despachos").header("Authorization", token(Rol.OPERADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[0].ordenId").value(8))
                .andExpect(jsonPath("$[0].estado").value("PREPARANDO"));
        mvc.perform(get("/api/operador/despachos").param("estado", "EN_TRANSITO").header("Authorization", token(Rol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].seguimiento").value("GHX-70214"));
    }

    @Test
    void lasRutasInternasExigenLaClaveInterna() throws Exception {
        String cuerpo = """
                {"ordenId":5,"ordenNumero":"GH-2026-000005","destinatario":"Camila Rojas",
                 "direccion":"Av. Los Aromos 1450, depto. 32","comuna":"Ñuñoa","region":"Región Metropolitana de Santiago"}
                """;
        mvc.perform(post("/internal/despachos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/internal/despachos").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seguimiento").value("GHX-70005"))
                .andExpect(jsonPath("$.estado").value("PREPARANDO"))
                .andExpect(jsonPath("$.fechaEstimada").value(LocalDate.now().plusDays(3).toString()))
                .andExpect(jsonPath("$.eventos.length()").value(1));
        mvc.perform(post("/internal/despachos").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Esta orden ya tiene un despacho."));
    }

    @Test
    void unaFechaEstimadaPasadaResponde400ConElCampo() throws Exception {
        String cuerpo = """
                {"ordenId":5,"ordenNumero":"GH-2026-000005","fechaEstimada":"%s"}
                """.formatted(LocalDate.now().minusDays(1));
        mvc.perform(post("/internal/despachos").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaEstimada").value("La fecha estimada no puede estar en el pasado."));
    }

    @Test
    void elDespachoSoloAvanzaYAlEntregarseFijaLaFecha() throws Exception {
        // La orden 2 de ejemplo va en tránsito: no puede volver a DESPACHADO
        mvc.perform(put("/internal/despachos/orden/2").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"DESPACHADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El despacho solo avanza: elige un estado posterior al actual."));
        mvc.perform(put("/internal/despachos/orden/2").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENTREGADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"))
                .andExpect(jsonPath("$.entregadoEn").isNotEmpty())
                .andExpect(jsonPath("$.eventos.length()").value(4))
                .andExpect(jsonPath("$.eventos[3].estado").value("ENTREGADO"));
        // La orden 3 no tiene despacho (su pago fue rechazado)
        mvc.perform(put("/internal/despachos/orden/3").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"DESPACHADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Primero genera el despacho de esta orden."));
        mvc.perform(put("/internal/despachos/orden/8").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.estado").value("Elige el nuevo estado del despacho."));
    }

    @Test
    void orderServicePideLosDespachosDeVariasOrdenes() throws Exception {
        mvc.perform(get("/internal/despachos").param("ordenes", "1,2,3").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].seguimiento").value("GHX-61873"))
                .andExpect(jsonPath("$[1].estado").value("EN_TRANSITO"))
                .andExpect(jsonPath("$[1].entregadoEn").isEmpty());
    }

    /** Jackson escribe las fechas con segundos: 2026-11-11T14:05:00. */
    private static String iso(LocalDateTime fecha) {
        return fecha.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String token(Rol rol) {
        return "Bearer " + jwt.generar(1L, "prueba@gamehub.test", "Prueba", rol);
    }
}
