package cl.gamehub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta del login, del registro y de la renovación: { token, usuario }. */
public record SesionRespuesta(
        @Schema(description = "JWT que se envía en la cabecera Authorization: Bearer <token>",
                example = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJjYW1pbGFAZ2FtZWh1Yi50ZXN0In0.firma") String token,
        UsuarioDto usuario) {
}
