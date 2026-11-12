package cl.gamehub.resenas.dto;

import cl.gamehub.resenas.model.EstadoResena;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Reseña propia del cliente: Mis órdenes la usa para saber qué productos de cada orden ya reseñó. */
public record MiResenaDto(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long ordenId,
        @Schema(example = "3") Long productoId,
        @Schema(example = "Procesador Tectra R7 de 8 núcleos") String producto,
        @Schema(example = "5") int puntuacion,
        @Schema(example = "Instalación simple y temperaturas bajas con un disipador de torre.") String comentario,
        @Schema(example = "2026-10-25T14:00:00") LocalDateTime fecha,
        @Schema(example = "PENDIENTE") EstadoResena estado) {
}
