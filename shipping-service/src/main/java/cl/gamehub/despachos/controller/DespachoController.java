package cl.gamehub.despachos.controller;

import cl.gamehub.despachos.dto.DespachoDto;
import cl.gamehub.despachos.service.DespachoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ruta pública: con el código de seguimiento cualquiera ve en qué va el despacho, sin iniciar sesión. */
@RestController
@RequestMapping("/api/despachos")
@Tag(name = "Seguimiento", description = "Estado de un despacho a partir de su código de seguimiento")
public class DespachoController {

    private final DespachoService servicio;

    public DespachoController(DespachoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/seguimiento/{codigo}")
    @Operation(summary = "Sigue un despacho", description = "No distingue mayúsculas. No muestra la dirección ni el destinatario.")
    @ApiResponse(responseCode = "200", description = "Estado del despacho y su línea de eventos")
    @ApiResponse(responseCode = "404", description = "No hay un despacho con ese código")
    public ResponseEntity<DespachoDto> seguimiento(@Parameter(example = "GHX-61873") @PathVariable String codigo) {
        return ResponseEntity.ok(servicio.seguimiento(codigo));
    }
}
