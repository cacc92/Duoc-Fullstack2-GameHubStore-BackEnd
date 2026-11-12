package cl.gamehub.despachos.controller;

import cl.gamehub.despachos.dto.DespachoDto;
import cl.gamehub.despachos.model.EstadoDespacho;
import cl.gamehub.despachos.service.DespachoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de despachos para el operador. La bandeja del frontend genera y avanza despachos a través de
 * order-service (que llama a las rutas internas). SecurityConfig solo deja entrar a OPERADOR y ADMIN.
 */
@RestController
@RequestMapping("/api/operador/despachos")
@SecurityRequirement(name = "bearer")
@Tag(name = "Bandeja de despachos", description = "Requiere rol OPERADOR o ADMIN")
public class OperadorDespachoController {

    private final DespachoService servicio;

    public OperadorDespachoController(DespachoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista los despachos", description = "Todos o los de un estado, del más nuevo al más antiguo.")
    @ApiResponse(responseCode = "400", description = "El estado no es PREPARANDO, DESPACHADO, EN_TRANSITO ni ENTREGADO")
    @ApiResponse(responseCode = "403", description = "El rol no es OPERADOR ni ADMIN")
    public ResponseEntity<List<DespachoDto>> listar(
            @Parameter(description = "Vacío para ver todos", example = "EN_TRANSITO")
            @RequestParam(required = false) EstadoDespacho estado) {
        return ResponseEntity.ok(servicio.listar(estado));
    }
}
