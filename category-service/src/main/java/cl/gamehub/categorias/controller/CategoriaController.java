package cl.gamehub.categorias.controller;

import cl.gamehub.categorias.dto.CategoriaDto;
import cl.gamehub.categorias.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ruta pública: el visitante ve las categorías sin iniciar sesión. */
@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorías", description = "Categorías del catálogo con su cantidad de productos activos")
public class CategoriaController {

    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista las categorías", description = "Cuenta solo los productos activos; los datos vienen de product-service.")
    @ApiResponse(responseCode = "200", description = "Categorías ordenadas por id")
    @ApiResponse(responseCode = "503", description = "product-service no respondió")
    public ResponseEntity<List<CategoriaDto>> listar() {
        return ResponseEntity.ok(servicio.listar(true));
    }
}
