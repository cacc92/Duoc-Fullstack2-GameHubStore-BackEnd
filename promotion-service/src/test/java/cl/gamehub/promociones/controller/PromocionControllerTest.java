package cl.gamehub.promociones.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.promociones.repository.PromocionRepository;
import java.time.LocalDate;
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
 * El servicio completo con su base H2 y los 3 cupones de ejemplo. No hay dobles: este servicio no llama a otros.
 * @Transactional revierte lo que cada prueba guarda, así una no cambia lo que ve la siguiente.
 */
@SpringBootTest
@Transactional
class PromocionControllerTest {

    private static final String CLAVE_INTERNA = "clave-interna-de-desarrollo-gamehub";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JwtServicio jwt;

    @Autowired
    private PromocionRepository promociones;

    private MockMvc mvc;

    /** El mismo día desde el que DatosIniciales calculó las fechas de los cupones. */
    private final LocalDate hoy = DatosDeEjemplo.base().toLocalDate();

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    void validarEsPublicoYNoDistingueMayusculas() throws Exception {
        mvc.perform(get("/api/promociones/validar").param("codigo", "gamer10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("GAMER10"))
                .andExpect(jsonPath("$.porcentaje").value(DatosDeEjemplo.PORCENTAJE_GAMER10))
                .andExpect(jsonPath("$.tope").value(DatosDeEjemplo.TOPE_GAMER10))
                .andExpect(jsonPath("$.termino").value(hoy.plusDays(20).toString()));
    }

    @Test
    void cadaCuponQueNoSirveRespondeConSuMensaje() throws Exception {
        mvc.perform(get("/api/promociones/validar").param("codigo", "CYBER25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cupón CYBER25 venció el " + Formatos.fecha(hoy.minusDays(10)) + "."));
        mvc.perform(get("/api/promociones/validar").param("codigo", "SETUP15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cupón SETUP15 comienza a regir el " + Formatos.fecha(hoy.plusDays(5)) + "."));
        mvc.perform(get("/api/promociones/validar").param("codigo", "NOEXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El cupón «NOEXISTE» no existe. Revisa cómo lo escribiste."));
        mvc.perform(get("/api/promociones/validar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Escribe un código de cupón."));
    }

    @Test
    void elMantenedorExigeSesionYRolAdmin() throws Exception {
        mvc.perform(get("/api/admin/promociones"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/promociones").header("Authorization", token(Rol.OPERADOR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/promociones").header("Authorization", token(Rol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("SETUP15"))
                .andExpect(jsonPath("$[0].estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$[1].codigo").value("GAMER10"))
                .andExpect(jsonPath("$[1].estado").value("VIGENTE"))
                .andExpect(jsonPath("$[1].usos").value(34))
                .andExpect(jsonPath("$[2].codigo").value("CYBER25"))
                .andExpect(jsonPath("$[2].estado").value("VENCIDA"));
    }

    @Test
    void crearGuardaElCodigoEnMayusculasYNoAceptaRepetidos() throws Exception {
        String cuerpo = cuerpo("verano20", hoy, hoy.plusDays(30), 150);
        mvc.perform(post("/api/admin/promociones").header("Authorization", token(Rol.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/promociones").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("VERANO20"))
                .andExpect(jsonPath("$.usos").value(0))
                .andExpect(jsonPath("$.estado").value("VIGENTE"));
        mvc.perform(post("/api/admin/promociones").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un cupón con el código VERANO20."));
    }

    @Test
    void unCodigoConEspaciosResponde400ConElCampo() throws Exception {
        mvc.perform(post("/api/admin/promociones").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("VER 20", hoy, hoy.plusDays(30), 150)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.codigo").value("El código usa de 4 a 20 letras o números, sin espacios."));
    }

    @Test
    void editarYDesactivarUnCupon() throws Exception {
        Long id = promociones.findByCodigoIgnoreCase("SETUP15").orElseThrow().getId();

        mvc.perform(put("/api/admin/promociones/" + id).header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("SETUP18", hoy.plusDays(5), hoy.plusDays(35), 100)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("SETUP18"))
                .andExpect(jsonPath("$.porcentaje").value(20));
        mvc.perform(patch("/api/admin/promociones/" + id + "/activo").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVA"));
        mvc.perform(get("/api/promociones/validar").param("codigo", "setup18"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cupón SETUP18 no está disponible."));
    }

    @Test
    void lasRutasInternasExigenLaClaveInterna() throws Exception {
        mvc.perform(get("/internal/promociones/vigente").param("codigo", "GAMER10"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/internal/promociones/vigente").param("codigo", "GAMER10").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("GAMER10"))
                .andExpect(jsonPath("$.porcentaje").value(DatosDeEjemplo.PORCENTAJE_GAMER10));
    }

    @Test
    void registrarUnUsoLoSumaSoloSiElCuponSirve() throws Exception {
        mvc.perform(post("/internal/promociones/GAMER10/usos").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("GAMER10"));
        mvc.perform(get("/api/admin/promociones").header("Authorization", token(Rol.ADMIN)))
                .andExpect(jsonPath("$[1].codigo").value("GAMER10"))
                .andExpect(jsonPath("$[1].usos").value(35));
        // CYBER25 venció: order-service recibe el 400 con el mismo mensaje que vería el cliente
        mvc.perform(post("/internal/promociones/CYBER25/usos").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cupón CYBER25 venció el " + Formatos.fecha(hoy.minusDays(10)) + "."));
    }

    private static String cuerpo(String codigo, LocalDate inicio, LocalDate termino, int usosMaximos) {
        return """
                {"codigo":"%s","porcentaje":20,"tope":40000,"inicio":"%s","termino":"%s","usosMaximos":%d}
                """.formatted(codigo, inicio, termino, usosMaximos);
    }

    private String token(Rol rol) {
        return "Bearer " + jwt.generar(1L, "prueba@gamehub.test", "Prueba", rol);
    }
}
