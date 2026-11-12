package cl.gamehub.categorias.controller;

import cl.gamehub.categorias.dto.CategoriaInternaDto;
import cl.gamehub.categorias.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rutas que usa product-service. El Gateway no las expone y exigen la cabecera X-Clave-Interna. */
@RestController
@RequestMapping("/internal/categorias")
@Tag(name = "Interno", description = "Solo entre microservicios: requiere X-Clave-Interna y el Gateway no las expone")
public class InternoCategoriaController {

    private final CategoriaService servicio;

    public InternoCategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Todas las categorías, sin contadores")
    public ResponseEntity<List<CategoriaInternaDto>> listar() {
        return ResponseEntity.ok(servicio.listarInterno());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Una categoría: 404 si no existe")
    public ResponseEntity<CategoriaInternaDto> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.buscarInterno(id));
    }
}
