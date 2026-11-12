package cl.gamehub.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Lo que envía auth-service al registrar una cuenta: el id de la credencial y los datos del formulario.
 * auth-service ya los validó, pero esta ruta es una API propia y vuelve a revisarlos.
 */
public record NuevoPerfilSolicitud(
        @Schema(example = "18")
        @NotNull(message = "Indica el id del usuario.") Long id,
        @Schema(example = "Rocío Navarro")
        @NotBlank(message = "Escribe tu nombre.")
        @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres.") String nombre,
        @Schema(example = "rocio@gamehub.test")
        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "Escribe un correo válido.") String email,
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
