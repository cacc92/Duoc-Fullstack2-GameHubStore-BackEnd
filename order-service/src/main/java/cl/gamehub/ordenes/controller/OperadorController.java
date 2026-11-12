package cl.gamehub.ordenes.controller;

import cl.gamehub.ordenes.dto.ActualizarDespachoSolicitud;
import cl.gamehub.ordenes.dto.GenerarDespachoSolicitud;
import cl.gamehub.ordenes.dto.OrdenDetalleDto;
import cl.gamehub.ordenes.dto.OrdenOperadorDto;
import cl.gamehub.ordenes.dto.ResumenOperadorDto;
import cl.gamehub.ordenes.service.OperacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bandeja del operador. SecurityConfig solo deja entrar a OPERADOR y ADMIN. */
@RestController
@RequestMapping("/api/operador")
@SecurityRequirement(name = "bearer")
@Tag(name = "Panel del operador: órdenes", description = "Pagos y despachos. Requiere rol OPERADOR o ADMIN")
public class OperadorController {

    private final OperacionService servicio;

    public OperadorController(OperacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/resumen")
    @Operation(summary = "Pagos por confirmar, despachos por generar y órdenes en curso")
    public ResponseEntity<ResumenOperadorDto> resumen() {
        return ResponseEntity.ok(servicio.resumen());
    }

    @GetMapping("/ordenes")
    @Operation(summary = "Bandeja de órdenes", description = "estado: PENDIENTE_PAGO, POR_DESPACHAR, EN_CURSO, ENTREGADAS o CERRADAS")
    public ResponseEntity<List<OrdenOperadorDto>> ordenes(@RequestParam(required = false) String estado) {
        return ResponseEntity.ok(servicio.listar(estado));
    }

    @GetMapping("/ordenes/{id}")
    @Operation(summary = "Detalle completo de cualquier orden")
    public ResponseEntity<OrdenDetalleDto> orden(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.detalle(id));
    }

    @PostMapping("/ordenes/{id}/pago/confirmar")
    @Operation(summary = "Confirma una transferencia pendiente y descuenta el stock")
    @ApiResponse(responseCode = "200", description = "Pago confirmado y stock descontado")
    @ApiResponse(responseCode = "400", description = "El pago no está pendiente")
    @ApiResponse(responseCode = "409", description = "No hay stock suficiente para confirmar")
    public ResponseEntity<OrdenOperadorDto> confirmarPago(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.confirmarPago(id));
    }

    @PostMapping("/ordenes/{id}/pago/anular")
    @Operation(summary = "Anula un pago pendiente")
    public ResponseEntity<OrdenOperadorDto> anularPago(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.anularPago(id));
    }

    @PostMapping("/ordenes/{id}/despacho")
    @Operation(summary = "Genera el despacho en shipping-service")
    @ApiResponse(responseCode = "201", description = "Despacho generado")
    @ApiResponse(responseCode = "409", description = "La orden no está pagada o ya tiene despacho")
    public ResponseEntity<OrdenOperadorDto> generarDespacho(@PathVariable Long id,
                                                            @Valid @RequestBody GenerarDespachoSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.generarDespacho(id, solicitud));
    }

    @PutMapping("/ordenes/{id}/despacho")
    @Operation(summary = "Avanza el despacho: DESPACHADO, EN_TRANSITO o ENTREGADO")
    @ApiResponse(responseCode = "200", description = "Despacho actualizado")
    @ApiResponse(responseCode = "400", description = "El estado no es posterior al actual o la orden no tiene despacho")
    public ResponseEntity<OrdenOperadorDto> actualizarDespacho(@PathVariable Long id,
                                                               @Valid @RequestBody ActualizarDespachoSolicitud solicitud) {
        return ResponseEntity.ok(servicio.actualizarDespacho(id, solicitud));
    }
}
