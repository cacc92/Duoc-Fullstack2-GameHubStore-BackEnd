package cl.gamehub.ordenes.client;

import java.time.LocalDateTime;

/** estado: PENDIENTE, APROBADO, RECHAZADO o ANULADO (lo decide payment-service). */
public record PagoDto(Long ordenId, String metodo, String estado, LocalDateTime fecha, String detalle) {
}
