package cl.gamehub.ordenes.dto;

import cl.gamehub.ordenes.model.EstadoOrden;
import java.time.LocalDateTime;

public record OrdenResumenDto(Long id, String numero, LocalDateTime creadaEn, EstadoOrden estado, int total, int unidades) {
}
