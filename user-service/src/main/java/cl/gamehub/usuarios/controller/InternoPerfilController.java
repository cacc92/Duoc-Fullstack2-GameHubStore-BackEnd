package cl.gamehub.usuarios.controller;

import cl.gamehub.usuarios.dto.NuevoPerfilSolicitud;
import cl.gamehub.usuarios.dto.PerfilDto;
import cl.gamehub.usuarios.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rutas que usa auth-service. El Gateway no las expone y exigen la cabecera X-Clave-Interna. */
@RestController
@RequestMapping("/internal/perfiles")
@Tag(name = "Interno", description = "Solo entre microservicios: requiere X-Clave-Interna y el Gateway no las expone")
public class InternoPerfilController {

    private final PerfilService servicio;

    public InternoPerfilController(PerfilService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Crea el perfil de una cuenta nueva", description = "Lo llama auth-service al registrar: el perfil queda con el id de la credencial.")
    @ApiResponse(responseCode = "201", description = "Perfil creado (rol null)")
    @ApiResponse(responseCode = "409", description = "Ya existe un perfil para ese usuario")
    public ResponseEntity<PerfilDto> crear(@Valid @RequestBody NuevoPerfilSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Un perfil por el id de la cuenta (rol null)")
    @ApiResponse(responseCode = "200", description = "Perfil encontrado")
    @ApiResponse(responseCode = "404", description = "No encontramos el perfil")
    public ResponseEntity<PerfilDto> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.buscarInterno(id));
    }
}
