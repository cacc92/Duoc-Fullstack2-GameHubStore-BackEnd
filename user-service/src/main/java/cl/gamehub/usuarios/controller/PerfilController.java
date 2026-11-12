package cl.gamehub.usuarios.controller;

import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.usuarios.dto.PerfilDto;
import cl.gamehub.usuarios.dto.PerfilSolicitud;
import cl.gamehub.usuarios.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cada usuario ve y cambia solo su propio perfil: el id sale del token. Sirve para cualquier rol. */
@RestController
@RequestMapping("/api/usuarios/perfil")
@SecurityRequirement(name = "bearer")
@Tag(name = "Perfil", description = "Datos de contacto y despacho del usuario con sesión iniciada")
public class PerfilController {

    private final PerfilService servicio;

    public PerfilController(PerfilService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Mi perfil", description = "El checkout lo usa para precargar los datos de despacho. El rol sale del token.")
    @ApiResponse(responseCode = "200", description = "Perfil del usuario con sesión iniciada")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "404", description = "No encontramos tu perfil")
    public ResponseEntity<PerfilDto> ver(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return ResponseEntity.ok(servicio.miPerfil(yo));
    }

    @PutMapping
    @Operation(summary = "Actualiza mi nombre, teléfono y dirección de despacho",
            description = "El correo no se cambia aquí: es el dato con que se inicia sesión en auth-service.")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado")
    @ApiResponse(responseCode = "400", description = "Algún campo no cumple las reglas")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "404", description = "No encontramos tu perfil")
    public ResponseEntity<PerfilDto> actualizar(@AuthenticationPrincipal UsuarioAutenticado yo,
                                                @Valid @RequestBody PerfilSolicitud solicitud) {
        return ResponseEntity.ok(servicio.actualizar(yo, solicitud));
    }
}
