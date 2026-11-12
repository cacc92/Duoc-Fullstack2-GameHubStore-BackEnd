package cl.gamehub.pagos.controller;

import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.pagos.dto.PagoDto;
import cl.gamehub.pagos.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ruta del cliente. El frontend no la usa (muestra el pago dentro de cada orden, que arma order-service),
 * pero queda en Swagger para probar el servicio por separado. SecurityConfig solo deja entrar al rol CLIENTE.
 */
@RestController
@RequestMapping("/api/pagos")
@SecurityRequirement(name = "bearer")
@Tag(name = "Pagos", description = "Pagos del cliente que inició sesión. Requiere rol CLIENTE")
public class PagoController {

    private final PagoService servicio;

    public PagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/mios")
    @Operation(summary = "Mis pagos", description = "Los pagos del usuario del token, del más reciente al más antiguo.")
    @ApiResponse(responseCode = "401", description = "Falta el token o venció")
    @ApiResponse(responseCode = "403", description = "El rol no es CLIENTE")
    public ResponseEntity<List<PagoDto>> mios(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return ResponseEntity.ok(servicio.mios(yo.id()));
    }
}
