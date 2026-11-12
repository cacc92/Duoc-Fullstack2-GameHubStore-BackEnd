package cl.gamehub.garantias.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que responde order-service en GET /internal/ordenes/{id}. Es un record propio de este servicio: los
 * microservicios no comparten clases, solo el formato JSON. estado llega como texto (PENDIENTE_PAGO,
 * PAGADA, ..., ENTREGADA) y entregadaEn solo viene si la orden ya fue entregada.
 */
public record OrdenInternaDto(Long id, String numero, Long usuarioId, String cliente, String estado,
                              LocalDateTime entregadaEn, List<LineaOrdenInternaDto> lineas) {
}
