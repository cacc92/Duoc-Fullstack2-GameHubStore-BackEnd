package cl.gamehub.auth.dto;

import cl.gamehub.comun.seguridad.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Una fila de la tabla de usuarios del panel de administración. */
public record UsuarioAdminDto(
        @Schema(example = "4") Long id,
        @Schema(example = "Pedro Díaz") String nombre,
        @Schema(example = "pedro@gamehub.test") String email,
        @Schema(example = "CLIENTE") Rol rol,
        @Schema(example = "false") boolean activo,
        @Schema(example = "2026-03-06T11:00:00") LocalDateTime creadoEn) {
}
