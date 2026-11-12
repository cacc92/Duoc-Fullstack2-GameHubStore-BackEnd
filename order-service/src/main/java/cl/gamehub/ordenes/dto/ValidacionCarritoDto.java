package cl.gamehub.ordenes.dto;

import cl.gamehub.ordenes.client.CuponDto;
import java.util.List;

/** valido es false si alguna línea supera el stock: el frontend no deja avanzar al checkout. */
public record ValidacionCarritoDto(List<LineaValidadaDto> lineas, CuponDto cupon, String mensajeCupon,
                                   int subtotal, int descuento, int total, boolean valido) {
}
