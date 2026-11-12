package cl.gamehub.despachos.controller;

import cl.gamehub.despachos.dto.ActualizarDespachoSolicitud;
import cl.gamehub.despachos.dto.DespachoDto;
import cl.gamehub.despachos.dto.NuevoDespachoSolicitud;
import cl.gamehub.despachos.service.DespachoService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rutas que usa order-service. El Gateway no las expone y exigen la cabecera X-Clave-Interna. */
@RestController
@RequestMapping("/internal/despachos")
@Tag(name = "Interno", description = "Solo entre microservicios: requiere X-Clave-Interna y el Gateway no las expone")
public class InternoDespachoController {

    private final DespachoService servicio;

    public InternoDespachoController(DespachoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Genera el despacho de una orden pagada",
            description = "Nace en PREPARANDO. Sin seguimiento usa GHX- y 70000 + id de la orden; sin fecha estimada, hoy + 3 días.")
    @ApiResponse(responseCode = "201", description = "Despacho generado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos (por ejemplo, una fecha estimada pasada)")
    @ApiResponse(responseCode = "409", description = "La orden ya tiene despacho o el seguimiento ya está en uso")
    public ResponseEntity<DespachoDto> generar(@Valid @RequestBody NuevoDespachoSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.generar(solicitud));
    }

    @PutMapping("/orden/{ordenId}")
    @Operation(summary = "Avanza el despacho: DESPACHADO, EN_TRANSITO o ENTREGADO",
            description = "Solo hacia un estado posterior. ENTREGADO fija la fecha de entrega.")
    @ApiResponse(responseCode = "400", description = "La orden no tiene despacho o el estado no es posterior al actual")
    @ApiResponse(responseCode = "409", description = "El nuevo seguimiento ya lo usa otro despacho")
    public ResponseEntity<DespachoDto> actualizar(@PathVariable Long ordenId,
                                                  @Valid @RequestBody ActualizarDespachoSolicitud solicitud) {
        return ResponseEntity.ok(servicio.actualizar(ordenId, solicitud));
    }

    @GetMapping
    @Operation(summary = "Los despachos de varias órdenes, por id de orden", description = "Las órdenes sin despacho se omiten.")
    public ResponseEntity<List<DespachoDto>> buscarPorOrdenes(
            @Parameter(description = "Ids de orden separados por coma", example = "1,2")
            @RequestParam(required = false) List<Long> ordenes) {
        return ResponseEntity.ok(servicio.buscarPorOrdenes(ordenes));
    }
}
