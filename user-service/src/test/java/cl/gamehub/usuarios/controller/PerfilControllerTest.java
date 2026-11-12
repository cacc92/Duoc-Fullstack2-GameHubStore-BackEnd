package cl.gamehub.usuarios.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.comun.semilla.DatosDeEjemplo;
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
 * El servicio completo con su base H2 y los 17 perfiles de ejemplo (user-service no llama a otros servicios,
 * así que no hay dobles). @Transactional revierte lo que cada prueba guarda.
 */
@SpringBootTest
@Transactional
class PerfilControllerTest {

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
    void elPerfilExigeSesionYTomaElRolDelToken() throws Exception {
        mvc.perform(get("/api/usuarios/perfil"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/perfil").header("Authorization", token(3L, Rol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.nombre").value("Camila Rojas"))
                .andExpect(jsonPath("$.comuna").value("Ñuñoa"))
                .andExpect(jsonPath("$.direccion").value("Av. Los Aromos 1450, depto. 32"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
        // Cualquier rol tiene perfil: Ana es la administradora
        mvc.perform(get("/api/usuarios/perfil").header("Authorization", token(1L, Rol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@gamehub.test"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void unaCuentaSinPerfilResponde404() throws Exception {
        mvc.perform(get("/api/usuarios/perfil").header("Authorization", token(99L, Rol.CLIENTE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos tu perfil."));
    }

    @Test
    void actualizarGuardaLosNuevosDatosDeDespacho() throws Exception {
        String cuerpo = perfil("Camila Rojas", "998877665", "Av. Italia 1180, depto. 401");
        mvc.perform(put("/api/usuarios/perfil").header("Authorization", token(3L, Rol.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("998877665"))
                .andExpect(jsonPath("$.comuna").value("Providencia"));
        // El checkout ve los datos nuevos; el correo sigue siendo el de la cuenta
        mvc.perform(get("/api/usuarios/perfil").header("Authorization", token(3L, Rol.CLIENTE)))
                .andExpect(jsonPath("$.direccion").value("Av. Italia 1180, depto. 401"))
                .andExpect(jsonPath("$.email").value("camila@gamehub.test"));
    }

    @Test
    void unTelefonoInvalidoResponde400ConElCampo() throws Exception {
        mvc.perform(put("/api/usuarios/perfil").header("Authorization", token(3L, Rol.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfil("Camila Rojas", "12345", "Av. Italia 1180, depto. 401")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.telefono").value("El teléfono debe tener 9 dígitos y empezar con 9."));
    }

    @Test
    void lasRutasInternasExigenLaClaveInterna() throws Exception {
        mvc.perform(get("/internal/perfiles/3"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/internal/perfiles/3").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Camila Rojas"))
                .andExpect(jsonPath("$.rol").value(nullValue()));
        mvc.perform(get("/internal/perfiles/99").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos el perfil."));
    }

    @Test
    void authServiceCreaElPerfilDeUnaCuentaNueva() throws Exception {
        mvc.perform(post("/internal/perfiles").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevoPerfil("18")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(18))
                .andExpect(jsonPath("$.nombre").value("Rocío Navarro"))
                .andExpect(jsonPath("$.rol").value(nullValue()));
        // La cuenta nueva ya ve su perfil con su propio token
        mvc.perform(get("/api/usuarios/perfil").header("Authorization", token(18L, Rol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comuna").value("Ñuñoa"));
        // Camila (id 3) ya tiene perfil
        mvc.perform(post("/internal/perfiles").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevoPerfil("3")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un perfil para ese usuario."));
        // Sin id no se sabe de qué cuenta es
        mvc.perform(post("/internal/perfiles").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevoPerfil("null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.id").value("Indica el id del usuario."));
    }

    private String token(Long id, Rol rol) {
        return "Bearer " + jwt.generar(id, "prueba@gamehub.test", "Prueba", rol);
    }

    private static String perfil(String nombre, String telefono, String direccion) {
        return """
                {"nombre":"%s","telefono":"%s","region":"%s","comuna":"Providencia","direccion":"%s"}
                """.formatted(nombre, telefono, DatosDeEjemplo.RM, direccion);
    }

    private static String nuevoPerfil(String id) {
        return """
                {"id":%s,"nombre":"Rocío Navarro","email":"rocio@gamehub.test","telefono":"987654321",
                 "region":"%s","comuna":"Ñuñoa","direccion":"Av. Irarrázaval 3400"}
                """.formatted(id, DatosDeEjemplo.RM);
    }
}
