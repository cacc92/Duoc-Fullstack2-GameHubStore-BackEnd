package cl.gamehub.inventario.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.Rol;
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
 * El servicio completo con su base H2, el stock de los 13 productos de ejemplo y sus 5 movimientos.
 * inventory-service no llama a otros servicios, así que aquí no hay dobles de Feign.
 * @Transactional revierte lo que cada prueba guarda, así una no cambia lo que ve la siguiente.
 */
@SpringBootTest
@Transactional
class InventarioControllerTest {

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
    void laDisponibilidadEsPublica() throws Exception {
        mvc.perform(get("/api/inventario/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[0].productoId").value(1))
                .andExpect(jsonPath("$[0].stock").value(7));
        mvc.perform(get("/api/inventario/stock").param("productos", "5,1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].productoId").value(5))
                .andExpect(jsonPath("$[1].stock").value(0));
    }

    @Test
    void elPanelExigeSesionYRolOperadorOAdmin() throws Exception {
        mvc.perform(get("/api/operador/inventario"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/operador/inventario").header("Authorization", token(Rol.CLIENTE)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/operador/inventario").header("Authorization", token(Rol.ADMIN)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/operador/inventario").header("Authorization", token(Rol.OPERADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[12].sku").value("CTL-AUS-PAD25"))
                .andExpect(jsonPath("$[12].activo").value(false));
    }

    @Test
    void losMovimientosMasRecientesVanPrimero() throws Exception {
        mvc.perform(get("/api/operador/inventario/movimientos").header("Authorization", token(Rol.OPERADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].producto").value("Mouse inalámbrico Nimbo Aero"))
                .andExpect(jsonPath("$[0].tipo").value("SALIDA"))
                .andExpect(jsonPath("$[0].cantidad").value(-1))
                .andExpect(jsonPath("$[0].stockResultante").value(0))
                .andExpect(jsonPath("$[1].producto").value("SSD NVMe Tectra Flux 1 TB"))
                .andExpect(jsonPath("$[1].fecha").value(endsWith("T17:30:00")))
                .andExpect(jsonPath("$[4].producto").value("Teclado mecánico Nimbo TKL"))
                .andExpect(jsonPath("$[4].usuario").value("Luis Soto"));
    }

    @Test
    void registrarUnMovimientoGuardaElNombreDelToken() throws Exception {
        String cuerpo = """
                {"productoId": 4, "tipo": "INGRESO", "cantidad": 6, "motivo": "Recepción de proveedor"}""";
        mvc.perform(post("/api/operador/inventario/movimientos").header("Authorization", token(Rol.OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value("Luis Soto"))
                .andExpect(jsonPath("$.fecha").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
                .andExpect(jsonPath("$.cantidad").value(6))
                .andExpect(jsonPath("$.stockResultante").value(30));
        mvc.perform(get("/api/inventario/stock").param("productos", "4"))
                .andExpect(jsonPath("$[0].stock").value(30));
        mvc.perform(get("/api/operador/inventario/movimientos").header("Authorization", token(Rol.ADMIN)))
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].producto").value("Teclado mecánico Nimbo TKL"));
    }

    @Test
    void unaSolicitudInvalidaResponde400ConElCampo() throws Exception {
        String cuerpo = """
                {"productoId": 4, "tipo": "SALIDA", "cantidad": -1, "motivo": "Merma"}""";
        mvc.perform(post("/api/operador/inventario/movimientos").header("Authorization", token(Rol.OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").value("La cantidad no puede ser negativa."));
    }

    @Test
    void unaSalidaQueDejaElStockNegativoResponde400() throws Exception {
        String cuerpo = """
                {"productoId": 5, "tipo": "SALIDA", "cantidad": 1, "motivo": "Venta en tienda"}""";
        mvc.perform(post("/api/operador/inventario/movimientos").header("Authorization", token(Rol.OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La salida deja el stock en -1. El stock no puede quedar negativo."));
    }

    @Test
    void lasRutasInternasExigenLaClaveInterna() throws Exception {
        String cuerpo = """
                {"referencia": "Venta GH-2026-000019",
                 "lineas": [{"productoId": 8, "cantidad": 2}, {"productoId": 4, "cantidad": 1}]}""";
        mvc.perform(post("/internal/inventario/descontar").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/internal/inventario/descontar").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoId").value(8))
                .andExpect(jsonPath("$[0].stock").value(0))
                .andExpect(jsonPath("$[1].productoId").value(4))
                .andExpect(jsonPath("$[1].stock").value(23));
        mvc.perform(get("/internal/inventario/stock").param("ids", "4,8").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stock").value(23))
                .andExpect(jsonPath("$[1].stock").value(0));
    }

    @Test
    void sinStockSuficienteNoSeDescuentaNada() throws Exception {
        String cuerpo = """
                {"referencia": "Venta GH-2026-000020",
                 "lineas": [{"productoId": 4, "cantidad": 1}, {"productoId": 8, "cantidad": 3}]}""";
        mvc.perform(post("/internal/inventario/descontar").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No hay stock suficiente de Consola portátil Austral Go 512 GB."));
        mvc.perform(get("/internal/inventario/stock").param("ids", "4,8").header("X-Clave-Interna", CLAVE_INTERNA))
                .andExpect(jsonPath("$[0].stock").value(24))
                .andExpect(jsonPath("$[1].stock").value(2));
    }

    @Test
    void unDescuentoSinLineasResponde400ConElCampo() throws Exception {
        mvc.perform(post("/internal/inventario/descontar").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"referencia\": \"Venta GH-2026-000021\", \"lineas\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.lineas").value("Indica al menos una línea."));
    }

    @Test
    void productServiceCreaYActualizaElStockDeUnProductoNuevo() throws Exception {
        String nuevo = """
                {"productoId": 14, "sku": "KB-NIM-MINI", "nombre": "Teclado Nimbo Mini", "activo": true, "stockInicial": 5}""";
        mvc.perform(post("/internal/inventario/productos").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(14))
                .andExpect(jsonPath("$.stock").value(5));
        mvc.perform(put("/internal/inventario/productos/14").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\": \"KB-NIM-MINI\", \"nombre\": \"Teclado Nimbo Mini 60 %\", \"activo\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Teclado Nimbo Mini 60 %"))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.stock").value(5));
        mvc.perform(get("/api/operador/inventario/movimientos").header("Authorization", token(Rol.OPERADOR)))
                .andExpect(jsonPath("$[0].motivo").value("Stock inicial del producto"))
                .andExpect(jsonPath("$[0].usuario").value("Sistema"))
                .andExpect(jsonPath("$[0].cantidad").value(5));
    }

    @Test
    void crearDosVecesElStockDeUnProductoResponde409() throws Exception {
        String repetido = """
                {"productoId": 1, "sku": "NB-KAI-B15", "nombre": "Notebook gamer Kairo Blade 15", "activo": true, "stockInicial": 3}""";
        mvc.perform(post("/internal/inventario/productos").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON).content(repetido))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe stock para el producto 1."));
    }

    @Test
    void actualizarUnProductoSinStockResponde404() throws Exception {
        mvc.perform(put("/internal/inventario/productos/99").header("X-Clave-Interna", CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\": \"X-99\", \"nombre\": \"No existe\", \"activo\": true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El producto no existe."));
    }

    /** Token de Luis Soto con el rol pedido: el usuario de los movimientos sale del nombre del token. */
    private String token(Rol rol) {
        return "Bearer " + jwt.generar(2L, "luis@gamehub.test", "Luis Soto", rol);
    }
}
