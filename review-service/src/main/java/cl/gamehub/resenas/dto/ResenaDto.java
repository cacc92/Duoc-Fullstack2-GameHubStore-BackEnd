package cl.gamehub.resenas.dto;

import cl.gamehub.resenas.model.EstadoResena;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Reseña que ve la tienda. autor va abreviado ("Martina C.") para no exponer datos personales. */
public record ResenaDto(
        @Schema(example = "4") Long id,
        @Schema(example = "Martina C.") String autor,
        @Schema(example = "5") int puntuacion,
        @Schema(example = "Llegó en dos días y bien embalada.") String comentario,
        @Schema(example = "2026-11-06T14:00:00") LocalDateTime fecha,
        @Schema(example = "APROBADA") EstadoResena estado) {
}
