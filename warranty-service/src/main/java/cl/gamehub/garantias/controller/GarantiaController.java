package cl.gamehub.garantias.controller;

import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.garantias.dto.CrearGarantiaSolicitud;
import cl.gamehub.garantias.dto.GarantiaDto;
import cl.gamehub.garantias.service.GarantiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** El cliente pide garantía de lo que recibió. SecurityConfig solo deja entrar al rol CLIENTE. */
@RestController
@RequestMapping("/api/garantias")
@SecurityRequirement(name = "bearer")
@Tag(name = "Garantías del cliente", description = "Solicitudes de garantía. Requiere rol CLIENTE")
public class GarantiaController {

    private final GarantiaService servicio;

    public GarantiaController(GarantiaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Pide garantía de un producto entregado",
            description = "Pregunta a order-service por la orden: debe ser del cliente, traer el producto y estar entregada. "
                    + "El plazo es de 6 meses desde la entrega y no puede haber otra solicitud abierta del mismo producto.")
    @ApiResponse(responseCode = "201", description = "Solicitud creada en estado ABIERTA, con su código GAR-0000")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, producto fuera de la orden, orden sin entregar o plazo vencido")
    @ApiResponse(responseCode = "403", description = "La orden es de otro cliente")
    @ApiResponse(responseCode = "404", description = "La orden no existe (respuesta de order-service)")
    @ApiResponse(responseCode = "409", description = "Ya hay una solicitud abierta para ese producto")
    @ApiResponse(responseCode = "503", description = "order-service no respondió")
    public ResponseEntity<GarantiaDto> crear(@AuthenticationPrincipal UsuarioAutenticado yo,
                                             @Valid @RequestBody CrearGarantiaSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(yo, solicitud));
    }

    @GetMapping("/mias")
    @Operation(summary = "Solicitudes de garantía del cliente", description = "De la más nueva a la más antigua.")
    public ResponseEntity<List<GarantiaDto>> mias(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return ResponseEntity.ok(servicio.mias(yo.id()));
    }
}
