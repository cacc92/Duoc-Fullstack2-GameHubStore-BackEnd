package cl.gamehub.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Lo que el usuario puede cambiar de su perfil, con las mismas reglas del registro. El correo no se cambia aquí. */
public record PerfilSolicitud(
        @Schema(example = "Camila Rojas")
        @NotBlank(message = "Escribe tu nombre.")
        @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres.") String nombre,
        @Schema(example = "912345678")
        @NotBlank(message = "Escribe tu teléfono.")
        @Pattern(regexp = "^9\\d{8}$", message = "El teléfono debe tener 9 dígitos y empezar con 9.") String telefono,
        @Schema(example = "Región Metropolitana de Santiago")
        @NotBlank(message = "Elige una región.") String region,
        @Schema(example = "Providencia")
        @NotBlank(message = "Elige una comuna.") String comuna,
        @Schema(example = "Av. Italia 1180, depto. 401")
        @NotBlank(message = "Escribe tu dirección de despacho.")
        @Size(min = 5, max = 150, message = "La dirección debe tener entre 5 y 150 caracteres.") String direccion) {
}
