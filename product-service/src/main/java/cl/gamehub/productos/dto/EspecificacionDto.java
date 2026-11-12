package cl.gamehub.productos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Una fila de la ficha técnica. Se usa tanto al recibir el formulario como al responder. */
public record EspecificacionDto(
        @Schema(example = "Memoria")
        @NotBlank(message = "Cada especificación necesita un nombre.")
        @Size(max = 60, message = "El nombre de cada especificación tiene hasta 60 caracteres.") String clave,
        @Schema(example = "12 GB GDDR6X · 192 bits")
        @NotBlank(message = "Cada especificación necesita un valor.")
        @Size(max = 160, message = "El valor de cada especificación tiene hasta 160 caracteres.") String valor) {
}
