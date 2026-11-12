package cl.gamehub.pagos.controller;

import cl.gamehub.pagos.dto.PagoDto;
import cl.gamehub.pagos.model.EstadoPago;
import cl.gamehub.pagos.service.PagoService;
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
 * Consulta de pagos para el operador. La bandeja del frontend confirma y anula pagos a través de order-service
 * (que llama a las rutas internas); esta ruta queda en Swagger. SecurityConfig solo deja entrar a OPERADOR y ADMIN.
 */
@RestController
@RequestMapping("/api/operador/pagos")
@SecurityRequirement(name = "bearer")
@Tag(name = "Bandeja de pagos", description = "Requiere rol OPERADOR o ADMIN")
public class OperadorPagoController {

    private final PagoService servicio;

    public OperadorPagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista los pagos", description = "Todos o los de un estado, del más reciente al más antiguo.")
    @ApiResponse(responseCode = "400", description = "El estado no es PENDIENTE, APROBADO, RECHAZADO ni ANULADO")
    @ApiResponse(responseCode = "403", description = "El rol no es OPERADOR ni ADMIN")
    public ResponseEntity<List<PagoDto>> listar(
            @Parameter(description = "Vacío para ver todos", example = "PENDIENTE")
            @RequestParam(required = false) EstadoPago estado) {
        return ResponseEntity.ok(servicio.listar(estado));
    }
}
