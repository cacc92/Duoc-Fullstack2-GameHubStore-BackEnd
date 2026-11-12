package cl.gamehub.productos.controller;

import cl.gamehub.productos.dto.ProductoInternoDto;
import cl.gamehub.productos.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rutas que usan order-service y category-service. El Gateway no las expone y exigen la cabecera X-Clave-Interna. */
@RestController
@RequestMapping("/internal/productos")
@Tag(name = "Interno", description = "Solo entre microservicios: requiere X-Clave-Interna y el Gateway no las expone")
public class InternoProductoController {

    private final ProductoService servicio;

    public InternoProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Varios productos por id, con el precio vigente hoy",
            description = "Lo usa order-service. Los id que no existen se omiten.")
    public ResponseEntity<List<ProductoInternoDto>> buscarVarios(
            @Parameter(description = "Ids separados por coma", example = "1,2,3") @RequestParam List<Long> ids) {
        return ResponseEntity.ok(servicio.buscarVarios(ids));
    }

    @GetMapping("/conteo")
    @Operation(summary = "Cantidad de productos por id de categoría",
            description = "Lo usa category-service, por ejemplo { \"1\": 1, \"4\": 4 }.")
    public ResponseEntity<Map<Long, Long>> conteo(@RequestParam(defaultValue = "true") boolean soloActivos) {
        return ResponseEntity.ok(servicio.conteoPorCategoria(soloActivos));
    }
}
