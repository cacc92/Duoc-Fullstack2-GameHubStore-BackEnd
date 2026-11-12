package cl.gamehub.auth.controller;

import cl.gamehub.auth.dto.UsuarioAdminDto;
import cl.gamehub.auth.service.AutenticacionService;
import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.comun.util.ActivoSolicitud;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cuentas de la tienda en el panel de administración. SecurityConfig solo deja entrar al rol ADMIN. */
@RestController
@RequestMapping("/api/admin/usuarios")
@SecurityRequirement(name = "bearer")
@Tag(name = "Administración de usuarios", description = "Requiere rol ADMIN")
public class AdminUsuarioController {

    private final AutenticacionService servicio;

    public AdminUsuarioController(AutenticacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista las cuentas con su rol y estado",
            description = "Ordenadas por rol (ADMIN, CLIENTE, OPERADOR) y, dentro de cada rol, por nombre.")
    @ApiResponse(responseCode = "200", description = "Todas las cuentas, activas y desactivadas")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN")
    public ResponseEntity<List<UsuarioAdminDto>> listar() {
        return ResponseEntity.ok(servicio.listar());
    }

    @PatchMapping("/{id}/activo")
    @Operation(summary = "Activa o desactiva una cuenta",
            description = "Una cuenta desactivada recibe 403 al iniciar sesión y al renovar el token. "
                    + "Los tokens que ya tenía siguen sirviendo hasta que vencen.")
    @ApiResponse(responseCode = "200", description = "La cuenta con su nuevo estado")
    @ApiResponse(responseCode = "400", description = "El administrador intentó desactivar su propia cuenta")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    public ResponseEntity<UsuarioAdminDto> cambiarActivo(@AuthenticationPrincipal UsuarioAutenticado yo,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody ActivoSolicitud solicitud) {
        return ResponseEntity.ok(servicio.cambiarActivo(id, solicitud.activo(), yo.id()));
    }
}
