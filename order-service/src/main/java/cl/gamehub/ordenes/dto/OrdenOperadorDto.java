package cl.gamehub.ordenes.dto;

import cl.gamehub.ordenes.model.EstadoOrden;
import cl.gamehub.ordenes.model.MetodoPago;
import java.time.LocalDateTime;

/** Fila de la bandeja del operador. estadoPago y estadoDespacho vienen de payment-service y shipping-service. */
public record OrdenOperadorDto(Long id, String numero, String cliente, LocalDateTime creadaEn, int total, int unidades,
                               EstadoOrden estado, MetodoPago metodoPago, String estadoPago, String estadoDespacho,
                               String seguimiento) {
}
