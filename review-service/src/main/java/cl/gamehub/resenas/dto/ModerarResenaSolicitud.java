package cl.gamehub.resenas.dto;

import cl.gamehub.resenas.model.EstadoResena;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** APROBADA publica la reseña; OCULTA la saca de la tienda. PENDIENTE no se acepta. */
public record ModerarResenaSolicitud(
        @Schema(example = "APROBADA")
        @NotNull(message = "Elige aprobar u ocultar.") EstadoResena estado) {
}
