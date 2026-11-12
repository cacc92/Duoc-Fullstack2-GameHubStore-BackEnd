package cl.gamehub.categorias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaSolicitud(
        @Schema(example = "Sillas gamer")
        @NotBlank(message = "Escribe el nombre de la categoría.")
        @Size(max = 60, message = "El nombre tiene hasta 60 caracteres.") String nombre) {
}
