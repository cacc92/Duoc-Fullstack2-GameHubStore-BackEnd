package cl.gamehub.categorias.controller;

import cl.gamehub.categorias.dto.CategoriaDto;
import cl.gamehub.categorias.dto.CategoriaSolicitud;
import cl.gamehub.categorias.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mantenedor de categorías. SecurityConfig solo deja entrar al rol ADMIN. */
@RestController
@RequestMapping("/api/admin/categorias")
@SecurityRequirement(name = "bearer")
@Tag(name = "Administración de categorías", description = "Requiere rol ADMIN")
public class AdminCategoriaController {

    private final CategoriaService servicio;

    public AdminCategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista las categorías contando también los productos inactivos")
    public ResponseEntity<List<CategoriaDto>> listar() {
        return ResponseEntity.ok(servicio.listar(false));
    }

    @PostMapping
    @Operation(summary = "Crea una categoría")
    @ApiResponse(responseCode = "201", description = "Categoría creada")
    @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre")
    public ResponseEntity<CategoriaDto> crear(@Valid @RequestBody CategoriaSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cambia el nombre (y el slug) de una categoría")
    @ApiResponse(responseCode = "200", description = "Categoría renombrada")
    @ApiResponse(responseCode = "404", description = "La categoría no existe")
    public ResponseEntity<CategoriaDto> renombrar(@PathVariable Long id, @Valid @RequestBody CategoriaSolicitud solicitud) {
        return ResponseEntity.ok(servicio.renombrar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una categoría sin productos")
    @ApiResponse(responseCode = "204", description = "Categoría eliminada")
    @ApiResponse(responseCode = "409", description = "La categoría todavía tiene productos")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
