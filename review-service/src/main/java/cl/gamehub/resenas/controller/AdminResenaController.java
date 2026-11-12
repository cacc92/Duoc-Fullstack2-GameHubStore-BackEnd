package cl.gamehub.resenas.controller;

import cl.gamehub.resenas.dto.ModerarResenaSolicitud;
import cl.gamehub.resenas.dto.ResenaAdminDto;
import cl.gamehub.resenas.model.EstadoResena;
import cl.gamehub.resenas.service.ResenaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Moderación de reseñas. SecurityConfig solo deja entrar al rol ADMIN. */
@RestController
@RequestMapping("/api/admin/resenas")
@SecurityRequirement(name = "bearer")
@Tag(name = "Moderación de reseñas", description = "Requiere rol ADMIN")
public class AdminResenaController {

    private final ResenaService servicio;

    public AdminResenaController(ResenaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Reseñas para moderar",
            description = "estado: PENDIENTE, APROBADA u OCULTA (vacío para todas), de la más reciente a la más antigua. "
                    + "alerta avisa si el comentario trae un teléfono o un correo.")
    @ApiResponse(responseCode = "200", description = "Reseñas con su alerta de datos de contacto")
    @ApiResponse(responseCode = "400", description = "El estado no es PENDIENTE, APROBADA ni OCULTA")
    public ResponseEntity<List<ResenaAdminDto>> listar(@RequestParam(required = false) EstadoResena estado) {
        return ResponseEntity.ok(servicio.listar(estado));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aprueba u oculta una reseña", description = "Solo las aprobadas se muestran en la tienda.")
    @ApiResponse(responseCode = "200", description = "Reseña con su nuevo estado")
    @ApiResponse(responseCode = "400", description = "Se pidió dejarla PENDIENTE")
    @ApiResponse(responseCode = "404", description = "La reseña no existe")
    public ResponseEntity<ResenaAdminDto> moderar(@PathVariable Long id,
                                                  @Valid @RequestBody ModerarResenaSolicitud solicitud) {
        return ResponseEntity.ok(servicio.moderar(id, solicitud.estado()));
    }
}
