package cl.gamehub.promociones.controller;

import cl.gamehub.comun.util.ActivoSolicitud;
import cl.gamehub.promociones.dto.PromocionDto;
import cl.gamehub.promociones.dto.PromocionSolicitud;
import cl.gamehub.promociones.service.PromocionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mantenedor de cupones. SecurityConfig solo deja entrar al rol ADMIN. */
@RestController
@RequestMapping("/api/admin/promociones")
@SecurityRequirement(name = "bearer")
@Tag(name = "Administración de promociones", description = "Requiere rol ADMIN")
public class AdminPromocionController {

    private final PromocionService servicio;

    public AdminPromocionController(PromocionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista los cupones",
            description = "Del inicio más reciente al más antiguo. estado: VIGENTE, PROGRAMADA, VENCIDA, AGOTADA o INACTIVA.")
    public ResponseEntity<List<PromocionDto>> listar() {
        return ResponseEntity.ok(servicio.listar());
    }

    @PostMapping
    @Operation(summary = "Crea un cupón con vigencia y tope de usos", description = "El código se guarda en mayúsculas.")
    @ApiResponse(responseCode = "201", description = "Cupón creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o término anterior al inicio")
    @ApiResponse(responseCode = "409", description = "Ya existe un cupón con ese código")
    public ResponseEntity<PromocionDto> crear(@Valid @RequestBody PromocionSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita un cupón", description = "Los usos que ya tiene no cambian.")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, término anterior al inicio o tope de usos menor que los usos")
    @ApiResponse(responseCode = "404", description = "La promoción no existe")
    @ApiResponse(responseCode = "409", description = "Otro cupón ya usa ese código")
    public ResponseEntity<PromocionDto> actualizar(@PathVariable Long id, @Valid @RequestBody PromocionSolicitud solicitud) {
        return ResponseEntity.ok(servicio.actualizar(id, solicitud));
    }

    @PatchMapping("/{id}/activo")
    @Operation(summary = "Activa o desactiva un cupón", description = "Un cupón desactivado responde «no está disponible».")
    @ApiResponse(responseCode = "404", description = "La promoción no existe")
    public ResponseEntity<PromocionDto> cambiarActivo(@PathVariable Long id, @Valid @RequestBody ActivoSolicitud solicitud) {
        return ResponseEntity.ok(servicio.cambiarActivo(id, solicitud.activo()));
    }
}
