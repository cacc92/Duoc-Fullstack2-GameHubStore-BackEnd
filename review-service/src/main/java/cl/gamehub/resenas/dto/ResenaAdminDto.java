package cl.gamehub.resenas.dto;

import cl.gamehub.resenas.model.EstadoResena;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Reseña en el panel de moderación. alerta avisa al administrador si el texto trae un teléfono o un correo. */
public record ResenaAdminDto(
        @Schema(example = "15") Long id,
        @Schema(example = "7") Long productoId,
        @Schema(example = "Monitor Kairo 27\" QHD 170 Hz") String producto,
        @Schema(example = "Paula R.") String autor,
        @Schema(example = "2") int puntuacion,
        @Schema(example = "Llegó con un píxel muerto. Llámenme al 9 8765 4321 para coordinar el cambio.") String comentario,
        @Schema(example = "2026-11-09T14:00:00") LocalDateTime fecha,
        @Schema(example = "PENDIENTE") EstadoResena estado,
        @Schema(example = "Contiene datos de contacto", description = "null si el texto no trae teléfonos ni correos") String alerta) {
}
