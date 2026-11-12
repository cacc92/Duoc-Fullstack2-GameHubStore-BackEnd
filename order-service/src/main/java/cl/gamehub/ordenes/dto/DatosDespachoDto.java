package cl.gamehub.ordenes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DatosDespachoDto(
        @Schema(example = "Camila Rojas")
        @NotBlank(message = "Escribe el nombre de quien recibe.")
        @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres.") String nombre,
        @Schema(example = "camila@gamehub.test")
        @NotBlank(message = "Escribe el correo.") @Email(message = "Escribe un correo válido.") String correo,
        @Schema(example = "912345678")
        @NotBlank(message = "Escribe el teléfono.")
        @Pattern(regexp = "^9\\d{8}$", message = "El teléfono debe tener 9 dígitos y empezar con 9.") String telefono,
        @Schema(example = "Región Metropolitana de Santiago") @NotBlank(message = "Elige una región.") String region,
        @Schema(example = "Ñuñoa") @NotBlank(message = "Elige una comuna.") String comuna,
        @Schema(example = "Av. Los Aromos 1450")
        @NotBlank(message = "Escribe la dirección de despacho.")
        @Size(min = 5, max = 150, message = "La dirección debe tener entre 5 y 150 caracteres.") String direccion,
        @Schema(example = "Depto. 32") @Size(max = 60) String depto,
        @Schema(example = "Dejar en conserjería") @Size(max = 200) String indicaciones) {
}
