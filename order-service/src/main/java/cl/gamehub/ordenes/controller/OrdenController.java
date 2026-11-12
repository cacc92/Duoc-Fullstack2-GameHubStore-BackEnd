package cl.gamehub.ordenes.controller;

import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.ordenes.dto.CrearOrdenSolicitud;
import cl.gamehub.ordenes.dto.OrdenDetalleDto;
import cl.gamehub.ordenes.dto.OrdenResumenDto;
import cl.gamehub.ordenes.service.OrdenService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordenes")
@SecurityRequirement(name = "bearer")
@Tag(name = "Órdenes del cliente", description = "Checkout y seguimiento. Requiere rol CLIENTE")
public class OrdenController {

    private final OrdenService servicio;

    public OrdenController(OrdenService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Crea la orden y registra el pago simulado",
            description = "Débito aprueba, crédito rechaza sobre $1.000.000 y transferencia queda pendiente. "
                    + "Con el pago aprobado se descuenta el stock en inventory-service.")
    @ApiResponse(responseCode = "201", description = "Orden creada (revisa pago.estado)")
    @ApiResponse(responseCode = "400", description = "Datos de despacho inválidos, cupón que no sirve o producto no disponible")
    @ApiResponse(responseCode = "409", description = "No hay stock suficiente")
    public ResponseEntity<OrdenDetalleDto> crear(@AuthenticationPrincipal UsuarioAutenticado yo,
                                                 @Valid @RequestBody CrearOrdenSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(yo, solicitud));
    }

    @GetMapping("/mias")
    @Operation(summary = "Órdenes del cliente, de la más reciente a la más antigua")
    public ResponseEntity<List<OrdenResumenDto>> mias(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return ResponseEntity.ok(servicio.mias(yo.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle con el pago y el seguimiento del despacho")
    @ApiResponse(responseCode = "200", description = "Detalle de la orden")
    @ApiResponse(responseCode = "403", description = "La orden es de otro cliente")
    @ApiResponse(responseCode = "404", description = "La orden no existe")
    public ResponseEntity<OrdenDetalleDto> detalle(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.detalle(id, yo));
    }
}
