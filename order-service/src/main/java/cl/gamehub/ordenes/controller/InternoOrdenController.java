package cl.gamehub.ordenes.controller;

import cl.gamehub.ordenes.dto.OrdenInternaDto;
import cl.gamehub.ordenes.service.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ruta que usan review-service y warranty-service para validar reseñas y garantías. */
@RestController
@RequestMapping("/internal/ordenes")
@Tag(name = "Interno", description = "Solo entre microservicios: requiere X-Clave-Interna y el Gateway no las expone")
public class InternoOrdenController {

    private final OrdenService servicio;

    public InternoOrdenController(OrdenService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dueño, estado, fecha de entrega y productos de una orden: 404 si no existe")
    public ResponseEntity<OrdenInternaDto> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.interno(id));
    }
}
