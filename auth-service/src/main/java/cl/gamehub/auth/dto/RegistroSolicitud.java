package cl.gamehub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Formulario de registro. Nombre, correo y contraseña quedan en auth-service; teléfono, región, comuna y
 * dirección van al perfil en user-service.
 */
public record RegistroSolicitud(
        @Schema(example = "Rocío Navarro")
        @NotBlank(message = "Escribe tu nombre.")
        @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres.") String nombre,
        @Schema(example = "rocio@gamehub.test")
        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "Escribe un correo válido.") String email,
        @Schema(example = "clave1234")
        @NotBlank(message = "Escribe una contraseña.")
        @Size(min = 8, max = 60, message = "La contraseña debe tener al menos 8 caracteres.")
        @Pattern(regexp = ".*\\d.*", message = "La contraseña debe incluir al menos un número.") String clave,
        @Schema(example = "987654321")
        @NotBlank(message = "Escribe tu teléfono.")
        @Pattern(regexp = "^9\\d{8}$", message = "El teléfono debe tener 9 dígitos y empezar con 9.") String telefono,
        @Schema(example = "Región Metropolitana de Santiago")
        @NotBlank(message = "Elige una región.") String region,
        @Schema(example = "Ñuñoa")
        @NotBlank(message = "Elige una comuna.") String comuna,
        @Schema(example = "Av. Irarrázaval 3400, depto. 1203")
        @NotBlank(message = "Escribe tu dirección de despacho.")
        @Size(min = 5, max = 150, message = "La dirección debe tener entre 5 y 150 caracteres.") String direccion) {
}
