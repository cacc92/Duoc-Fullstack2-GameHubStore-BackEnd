package cl.gamehub.pagos.controller;

import cl.gamehub.pagos.dto.NuevoPagoSolicitud;
import cl.gamehub.pagos.dto.PagoDto;
import cl.gamehub.pagos.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rutas que usa order-service. El Gateway no las expone y exigen la cabecera X-Clave-Interna. */
@RestController
@RequestMapping("/internal/pagos")
@Tag(name = "Interno", description = "Solo entre microservicios: requiere X-Clave-Interna y el Gateway no las expone")
public class InternoPagoController {

    private final PagoService servicio;

    public InternoPagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Registra el pago simulado de una orden nueva",
            description = "DEBITO queda APROBADO; CREDITO se RECHAZA sobre $1.000.000; TRANSFERENCIA queda PENDIENTE.")
    @ApiResponse(responseCode = "201", description = "Pago registrado (revisar su estado)")
    @ApiResponse(responseCode = "409", description = "La orden ya tiene un pago")
    public ResponseEntity<PagoDto> registrar(@Valid @RequestBody NuevoPagoSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud));
    }

    @PostMapping("/orden/{ordenId}/confirmar")
    @Operation(summary = "Confirma una transferencia pendiente")
    @ApiResponse(responseCode = "400", description = "El pago no está pendiente")
    @ApiResponse(responseCode = "404", description = "La orden no tiene pago")
    public ResponseEntity<PagoDto> confirmar(@PathVariable Long ordenId) {
        return ResponseEntity.ok(servicio.confirmar(ordenId));
    }

    @PostMapping("/orden/{ordenId}/anular")
    @Operation(summary = "Anula un pago pendiente")
    @ApiResponse(responseCode = "400", description = "El pago no está pendiente")
    @ApiResponse(responseCode = "404", description = "La orden no tiene pago")
    public ResponseEntity<PagoDto> anular(@PathVariable Long ordenId) {
        return ResponseEntity.ok(servicio.anular(ordenId));
    }

    @PostMapping("/orden/{ordenId}/revertir")
    @Operation(summary = "Compensación: anula el pago desde cualquier estado",
            description = "order-service la usa cuando aprobó el pago pero no pudo descontar el stock.")
    @ApiResponse(responseCode = "404", description = "La orden no tiene pago")
    public ResponseEntity<PagoDto> revertir(@PathVariable Long ordenId) {
        return ResponseEntity.ok(servicio.revertir(ordenId));
    }

    @GetMapping
    @Operation(summary = "Los pagos de varias órdenes, por id de orden", description = "Las órdenes sin pago se omiten.")
    public ResponseEntity<List<PagoDto>> buscarPorOrdenes(
            @Parameter(description = "Ids de orden separados por coma", example = "1,2,3")
            @RequestParam(required = false) List<Long> ordenes) {
        return ResponseEntity.ok(servicio.buscarPorOrdenes(ordenes));
    }
}
