package cl.gamehub.ordenes.dto;

import cl.gamehub.ordenes.client.DespachoDto;
import cl.gamehub.ordenes.client.PagoDto;
import cl.gamehub.ordenes.model.EstadoOrden;
import java.time.LocalDateTime;
import java.util.List;

/** Detalle de una orden: lo propio de order-service más el pago y el despacho que informan sus servicios. */
public record OrdenDetalleDto(Long id, String numero, LocalDateTime creadaEn, EstadoOrden estado, String cliente,
                              int subtotal, int descuento, int total, String cupon, DatosDespachoDto despachoDatos,
                              List<LineaOrdenDto> lineas, PagoDto pago, DespachoDto despacho,
                              LocalDateTime entregadaEn) {
}
