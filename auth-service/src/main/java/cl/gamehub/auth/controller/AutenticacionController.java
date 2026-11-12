package cl.gamehub.auth.controller;

import cl.gamehub.auth.dto.LoginSolicitud;
import cl.gamehub.auth.dto.RegistroSolicitud;
import cl.gamehub.auth.dto.SesionRespuesta;
import cl.gamehub.auth.service.AutenticacionService;
import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login y registro son públicos; renovar el token exige una sesión vigente. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión, registro y renovación del token")
public class AutenticacionController {

    private final AutenticacionService servicio;

    public AutenticacionController(AutenticacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y entrega { token, usuario }",
            description = "El token se envía después en la cabecera Authorization: Bearer <token>. "
                    + "Lo validan todos los servicios con el mismo secreto, sin volver a preguntar aquí.")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada")
    @ApiResponse(responseCode = "400", description = "Falta el correo o la contraseña, o el correo no es válido")
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos")
    @ApiResponse(responseCode = "403", description = "La cuenta está desactivada")
    public ResponseEntity<SesionRespuesta> login(@Valid @RequestBody LoginSolicitud solicitud) {
        return ResponseEntity.ok(servicio.iniciarSesion(solicitud));
    }

    @PostMapping("/registro")
    @Operation(summary = "Crea una cuenta de cliente y deja la sesión iniciada",
            description = "La credencial queda en auth-service y el perfil (teléfono y dirección) en user-service, "
                    + "con el mismo id. Si user-service falla, la cuenta no se crea.")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "400", description = "Algún campo no cumple las reglas")
    @ApiResponse(responseCode = "409", description = "Ya existe una cuenta con ese correo")
    @ApiResponse(responseCode = "503", description = "user-service no respondió")
    public ResponseEntity<SesionRespuesta> registro(@Valid @RequestBody RegistroSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud));
    }

    @PostMapping("/renovar")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Entrega un token nuevo antes de que venza el actual",
            description = "Sirve para cualquier rol. Un token vencido ya no se renueva: hay que iniciar sesión otra vez.")
    @ApiResponse(responseCode = "200", description = "Token nuevo con los datos actuales de la cuenta")
    @ApiResponse(responseCode = "401", description = "Sin token o con un token vencido")
    @ApiResponse(responseCode = "403", description = "La cuenta quedó desactivada")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    public ResponseEntity<SesionRespuesta> renovar(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return ResponseEntity.ok(servicio.renovar(yo.id()));
    }
}
