package cl.gamehub.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.gamehub.auth.client.NuevoPerfilSolicitud;
import cl.gamehub.auth.client.UserClient;
import cl.gamehub.auth.config.DatosIniciales;
import cl.gamehub.auth.repository.CredencialRepository;
import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.comun.semilla.DatosDeEjemplo;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * El servicio completo con su base H2 y las 17 cuentas de ejemplo; solo user-service es un doble.
 * @Transactional revierte lo que cada prueba guarda, así una no cambia lo que ve la siguiente.
 */
@SpringBootTest
@Transactional
class AutenticacionControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JwtServicio jwt;

    @Autowired
    private CredencialRepository credenciales;

    @MockitoBean
    private UserClient perfiles;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    // ---------- Login ----------

    @Test
    void elLoginEntregaUnTokenQueLosDemasServiciosPuedenValidar() throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("camila@gamehub.test", DatosIniciales.CLAVE_PRUEBA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.id").value(3))
                .andExpect(jsonPath("$.usuario.nombre").value("Camila Rojas"))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // El token lleva el id y el rol: con el mismo secreto, cualquier servicio sabe quién llama
        UsuarioAutenticado usuario = jwt.validar(JsonPath.read(json, "$.token"));
        assertThat(usuario.id()).isEqualTo(3L);
        assertThat(usuario.nombre()).isEqualTo("Camila Rojas");
        assertThat(usuario.rol()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    void elLoginRechazaUnaClaveIncorrectaYUnaCuentaDesactivada() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("camila@gamehub.test", "otra-clave1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
        // Pedro (id 4) está desactivado en los datos de ejemplo
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("pedro@gamehub.test", DatosIniciales.CLAVE_PRUEBA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Tu cuenta está desactivada. Escríbenos desde Ayuda para reactivarla."));
    }

    @Test
    void unCorreoInvalidoResponde400ConElCampo() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("camila", DatosIniciales.CLAVE_PRUEBA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").value("Escribe un correo válido."));
    }

    // ---------- Registro ----------

    @Test
    void elRegistroCreaUnClienteYLePideElPerfilAUserService() throws Exception {
        String json = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Rocio@GameHub.test", "987654321")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value("rocio@gamehub.test"))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // user-service recibe el perfil con el mismo id que la credencial nueva
        Number id = JsonPath.read(json, "$.usuario.id");
        verify(perfiles).crearPerfil(new NuevoPerfilSolicitud(id.longValue(), "Rocío Navarro", "rocio@gamehub.test",
                "987654321", DatosDeEjemplo.RM, "Ñuñoa", "Av. Irarrázaval 3400"));
        // y la cuenta nueva ya puede iniciar sesión
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("rocio@gamehub.test", "clave1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.id").value(id.intValue()));
    }

    @Test
    void unCorreoConCuentaResponde409SinLlamarAUserService() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("CAMILA@gamehub.test", "987654321")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una cuenta con ese correo. Inicia sesión o usa otro."));
        verifyNoInteractions(perfiles);
    }

    @Test
    void unTelefonoInvalidoResponde400ConElCampo() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("rocio@gamehub.test", "12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.telefono").value("El teléfono debe tener 9 dígitos y empezar con 9."));
        verifyNoInteractions(perfiles);
    }

    /**
     * Esta prueba va sin la transacción de la clase: así se ve el rollback real del servicio. La credencial
     * se inserta, user-service falla y la excepción deshace el INSERT: la cuenta no queda a medias.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void siUserServiceFallaLaCredencialNoQuedaGuardada() throws Exception {
        doThrow(ApiException.noDisponible("Uno de los servicios de la tienda no responde. Intenta de nuevo en unos minutos."))
                .when(perfiles).crearPerfil(any());

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("sinperfil@gamehub.test", "987654321")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Uno de los servicios de la tienda no responde. Intenta de nuevo en unos minutos."));

        assertThat(credenciales.existsByEmailIgnoreCase("sinperfil@gamehub.test")).isFalse();
        assertThat(credenciales.count()).isEqualTo(17);
    }

    // ---------- Renovar ----------

    @Test
    void renovarExigeSesionYUnaCuentaActiva() throws Exception {
        mvc.perform(post("/api/auth/renovar"))
                .andExpect(status().isUnauthorized());
        // El token nuevo sale con los datos guardados, no con los del token anterior
        mvc.perform(post("/api/auth/renovar").header("Authorization", token(3L, Rol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value("camila@gamehub.test"));
        mvc.perform(post("/api/auth/renovar").header("Authorization", token(4L, Rol.CLIENTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Tu cuenta está desactivada. Escríbenos desde Ayuda para reactivarla."));
        mvc.perform(post("/api/auth/renovar").header("Authorization", token(99L, Rol.CLIENTE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El usuario no existe."));
    }

    // ---------- Administración ----------

    @Test
    void laListaDeUsuariosEsSoloParaAdmin() throws Exception {
        mvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/usuarios").header("Authorization", token(2L, Rol.OPERADOR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/usuarios").header("Authorization", token(1L, Rol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(17))
                // Por rol (ADMIN, CLIENTE, OPERADOR) y dentro de cada rol por nombre
                .andExpect(jsonPath("$[0].nombre").value("Ana Torres"))
                .andExpect(jsonPath("$[0].rol").value("ADMIN"))
                .andExpect(jsonPath("$[1].nombre").value("Andrés Vidal"))
                .andExpect(jsonPath("$[16].nombre").value("Luis Soto"))
                .andExpect(jsonPath("$[16].rol").value("OPERADOR"))
                // Fechas en formato ISO, como en el resto de la API
                .andExpect(jsonPath("$[0].creadoEn").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")));
    }

    @Test
    void elAdminReactivaUnaCuentaPeroNoPuedeDesactivarLaSuya() throws Exception {
        String admin = token(1L, Rol.ADMIN);
        // Reactiva a Pedro (id 4) y Pedro vuelve a entrar
        mvc.perform(patch("/api/admin/usuarios/4/activo").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pedro@gamehub.test"))
                .andExpect(jsonPath("$.activo").value(true));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("pedro@gamehub.test", DatosIniciales.CLAVE_PRUEBA)))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/admin/usuarios/1/activo").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No puedes desactivar tu propia cuenta."));
        mvc.perform(patch("/api/admin/usuarios/99/activo").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El usuario no existe."));
        mvc.perform(patch("/api/admin/usuarios/4/activo").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.activo").value("Indica si queda activo."));
    }

    private String token(Long id, Rol rol) {
        return "Bearer " + jwt.generar(id, "prueba@gamehub.test", "Prueba", rol);
    }

    private static String login(String email, String clave) {
        return "{\"email\":\"" + email + "\",\"clave\":\"" + clave + "\"}";
    }

    private static String registro(String email, String telefono) {
        return """
                {"nombre":" Rocío Navarro ","email":"%s","clave":"clave1234","telefono":"%s",
                 "region":"%s","comuna":"Ñuñoa","direccion":"Av. Irarrázaval 3400"}
                """.formatted(email, telefono, DatosDeEjemplo.RM);
    }
}
