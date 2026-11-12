package cl.gamehub.ordenes.dto;

import cl.gamehub.ordenes.model.EstadoOrden;
import java.time.LocalDateTime;
import java.util.List;

/** Lo que necesitan review-service y warranty-service para validar una reseña o una garantía. */
public record OrdenInternaDto(Long id, String numero, Long usuarioId, String cliente, EstadoOrden estado,
                              LocalDateTime entregadaEn, List<LineaInternaDto> lineas) {

    public record LineaInternaDto(Long productoId, String nombre) {
    }
}
