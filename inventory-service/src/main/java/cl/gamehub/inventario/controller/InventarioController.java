package cl.gamehub.inventario.controller;

import cl.gamehub.inventario.dto.DisponibilidadDto;
import cl.gamehub.inventario.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Ruta pública: el catálogo muestra cuántas unidades quedan sin pedir sesión. */
@RestController
@RequestMapping("/api/inventario")
@Tag(name = "Inventario", description = "Stock disponible de cada producto, para el catálogo")
public class InventarioController {

    private final InventarioService servicio;

    public InventarioController(InventarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/stock")
    @Operation(summary = "Stock disponible por producto",
            description = "Sin el parámetro productos entrega todos. El frontend lo junta con los datos de product-service.")
    @ApiResponse(responseCode = "200", description = "Stock ordenado por id de producto")
    public ResponseEntity<List<DisponibilidadDto>> stock(
            @Parameter(description = "Ids de producto separados por coma", example = "1,2,3")
            @RequestParam(required = false) List<Long> productos) {
        return ResponseEntity.ok(servicio.disponibilidad(productos));
    }
}
