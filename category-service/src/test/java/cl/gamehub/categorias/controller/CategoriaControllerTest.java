package cl.gamehub.categorias.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.gamehub.categorias.client.ProductoClient;
import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.Rol;
import java.util.Map;
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
 * El servicio completo con su base H2 y sus datos de ejemplo; solo product-service es un doble.
 * @Transactional revierte lo que cada prueba guarda, así una no cambia lo que ve la siguiente.
 */
@SpringBootTest
@Transactional
class CategoriaControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JwtServicio jwt;

    @MockitoBean
    private ProductoClient productos;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        when(productos.conteoPorCategoria(anyBoolean())).thenReturn(Map.of(4L, 5L));
    }

    @Test
    void laListaEsPublicaYTraeElConteo() throws Exception {
        mvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[3].slug").value("perifericos"))
                .andExpect(jsonPath("$[3].cantidadProductos").value(5));
    }

    @Test
    void crearExigeSesionYRolAdmin() throws Exception {
        String cuerpo = "{\"nombre\":\"Sillas gamer\"}";
        mvc.perform(post("/api/admin/categorias").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/categorias").header("Authorization", token(Rol.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/categorias").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("sillas-gamer"));
    }

    @Test
    void unNombreVacioResponde400ConElCampo() throws Exception {
        mvc.perform(post("/api/admin/categorias").header("Authorization", token(Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").value("Escribe el nombre de la categoría."));
    }

    @Test
    void lasRutasInternasExigenLaClaveInterna() throws Exception {
        mvc.perform(get("/internal/categorias/2"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/internal/categorias/2").header("X-Clave-Interna", "clave-interna-de-desarrollo-gamehub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tarjetas gráficas"));
    }

    private String token(Rol rol) {
        return "Bearer " + jwt.generar(1L, "prueba@gamehub.test", "Prueba", rol);
    }
}
