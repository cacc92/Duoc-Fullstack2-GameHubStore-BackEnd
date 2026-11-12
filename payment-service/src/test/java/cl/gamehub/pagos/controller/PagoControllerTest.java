package cl.gamehub.pagos.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.OrdenEjemplo;
import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.Rol;
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
 * El servicio completo con su base H2 y los 18 pagos de ejemplo. No hay dobles: este servicio no llama a otros.
 * @Transactional revierte lo que cada prueba guarda, así una no cambia lo que ve la siguiente.
 */
@SpringBootTest
@Transactional
class PagoControllerTest {

    private static final String CLAVE_INTERNA = "clave-interna-de-desarrollo-gamehub";
    private static final String NUEVO_PAGO = """
            {"ordenId":19,"ordenNumero":"GH-2026-000019","usuarioId":3,"metodo":"DEBITO","monto":59990}
            """;

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
    void lasRutasInternasExigenLaClaveInterna() throws Exception {
        mvc.perform(post("/internal/pagos").contentType(MediaType.APPLICATION_JSON).content(NUEVO_PAGO))
                .andExpect(status().isForbidden());
        mvc.perform(post("/internal/pagos").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(NUEVO_PAGO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ordenId").value(19))
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.detalle").value("Pago aprobado (simulado)."));
        // La misma orden no se paga dos veces
        mvc.perform(post("/internal/pagos").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(NUEVO_PAGO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La orden ya tiene un pago registrado."));
    }

    @Test
    void unPagoSinMetodoResponde400ConElCampo() throws Exception {
        mvc.perform(post("/internal/pagos").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordenId\":19,\"ordenNumero\":\"GH-2026-000019\",\"usuarioId\":3,\"monto\":59990}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.metodo").value("Elige un método de pago."));
    }

    @Test
    void misPagosSonLosDelUsuarioDelToken() throws Exception {
        mvc.perform(get("/api/pagos/mios"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/pagos/mios").header("Authorization", token(2L, Rol.OPERADOR)))
                .andExpect(status().isForbidden());
        // Camila (usuario 3) compró las órdenes 1 a 5; la más reciente es la 5, con el cupón GAMER10
        mvc.perform(get("/api/pagos/mios").header("Authorization", token(3L, Rol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].ordenId").value(5))
                .andExpect(jsonPath("$[0].monto").value(orden(5).total()));
    }

    @Test
    void elOperadorVeTodosLosPagosOLosDeUnEstado() throws Exception {
        mvc.perform(get("/api/operador/pagos").header("Authorization", token(3L, Rol.CLIENTE)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/operador/pagos").header("Authorization", token(1L, Rol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(18));
        // Las dos transferencias por confirmar, de la más reciente a la más antigua
        mvc.perform(get("/api/operador/pagos").param("estado", "PENDIENTE").header("Authorization", token(2L, Rol.OPERADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ordenId").value(6))
                .andExpect(jsonPath("$[1].ordenId").value(7));
    }

    @Test
    void soloSeConfirmanOAnulanTransferenciasPendientes() throws Exception {
        mvc.perform(post("/internal/pagos/orden/6/confirmar").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.detalle").value("Transferencia confirmada por el operador."));
        mvc.perform(post("/internal/pagos/orden/7/anular").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"))
                .andExpect(jsonPath("$.detalle").value("Anulado por el operador."));
        mvc.perform(post("/internal/pagos/orden/6/anular").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Solo se pueden anular pagos pendientes."));
        mvc.perform(post("/internal/pagos/orden/99/confirmar").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El pago de la orden no existe."));
    }

    @Test
    void revertirAnulaUnPagoAprobado() throws Exception {
        mvc.perform(post("/internal/pagos/orden/5/revertir").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"))
                .andExpect(jsonPath("$.detalle").value("Anulado: no había stock para completar la compra."));
    }

    @Test
    void losPagosDeEjemploCalzanConLasOrdenesDeEjemplo() throws Exception {
        // La orden 1 se pagó por transferencia y el operador la confirmó 3 horas después; la 99 no existe
        OrdenEjemplo primera = orden(1);
        mvc.perform(get("/internal/pagos").param("ordenes", "1,3,99").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ordenNumero").value(primera.numero()))
                .andExpect(jsonPath("$[0].metodo").value("TRANSFERENCIA"))
                .andExpect(jsonPath("$[0].monto").value(primera.total()))
                .andExpect(jsonPath("$[0].fecha").value(primera.fechaPago().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .andExpect(jsonPath("$[0].detalle").value(primera.detallePago()))
                .andExpect(jsonPath("$[1].estado").value("RECHAZADO"));
    }

    private static OrdenEjemplo orden(long id) {
        return DatosDeEjemplo.orden(DatosDeEjemplo.base(), id);
    }

    private String token(Long id, Rol rol) {
        return "Bearer " + jwt.generar(id, "prueba@gamehub.test", "Prueba", rol);
    }
}
