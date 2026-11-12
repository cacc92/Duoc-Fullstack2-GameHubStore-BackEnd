package cl.gamehub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginSolicitud(
        @Schema(example = "camila@gamehub.test")
        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "Escribe un correo válido.") String email,
        @Schema(example = "clave123")
        @NotBlank(message = "Escribe tu contraseña.") String clave) {
}
