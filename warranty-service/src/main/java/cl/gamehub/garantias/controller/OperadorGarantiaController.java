package cl.gamehub.garantias.controller;

import cl.gamehub.garantias.dto.GarantiaDto;
import cl.gamehub.garantias.dto.ResolverGarantiaSolicitud;
import cl.gamehub.garantias.dto.ResumenGarantiasDto;
import cl.gamehub.garantias.service.GarantiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Bandeja de garantías del panel del operador. SecurityConfig solo deja entrar a OPERADOR y ADMIN. */
@RestController
@RequestMapping("/api/operador/garantias")
@SecurityRequirement(name = "bearer")
@Tag(name = "Garantías del operador", description = "Bandeja y resolución de solicitudes. Requiere rol OPERADOR o ADMIN")
public class OperadorGarantiaController {

    private final GarantiaService servicio;

    public OperadorGarantiaController(GarantiaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Solicitudes de garantía: primero las abiertas",
            description = "Primero ABIERTA y EN_REVISION, después las cerradas; cada grupo de la más nueva a la más antigua.")
    public ResponseEntity<List<GarantiaDto>> listar() {
        return ResponseEntity.ok(servicio.todas());
    }

    @GetMapping("/resumen")
    @Operation(summary = "Cantidad de garantías sin cerrar", description = "Cuenta ABIERTA y EN_REVISION para el contador del panel.")
    public ResponseEntity<ResumenGarantiasDto> resumen() {
        return ResponseEntity.ok(servicio.resumen());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cambia el estado de una garantía",
            description = "EN_REVISION, RESUELTA o RECHAZADA. Para cerrarla (RESUELTA o RECHAZADA) se exige la resolución.")
    @ApiResponse(responseCode = "200", description = "Garantía con su nuevo estado")
    @ApiResponse(responseCode = "400", description = "Ya está cerrada, se pidió volver a ABIERTA o falta la resolución")
    @ApiResponse(responseCode = "404", description = "La garantía no existe")
    public ResponseEntity<GarantiaDto> resolver(@PathVariable Long id, @Valid @RequestBody ResolverGarantiaSolicitud solicitud) {
        return ResponseEntity.ok(servicio.resolver(id, solicitud));
    }
}
