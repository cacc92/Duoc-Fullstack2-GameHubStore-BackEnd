package cl.gamehub.auth.dto;

import cl.gamehub.comun.seguridad.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

/** Lo que el frontend guarda junto al token para mostrar el nombre y decidir qué menús ve. */
public record UsuarioDto(
        @Schema(example = "3") Long id,
        @Schema(example = "Camila Rojas") String nombre,
        @Schema(example = "camila@gamehub.test") String email,
        @Schema(example = "CLIENTE") Rol rol) {
}
