package cl.gamehub.resenas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Lo que muestra cada tarjeta del catálogo: estrellas promedio y cuántas reseñas aprobadas tiene. */
public record ResumenResenasDto(
        @Schema(example = "2") Long productoId,
        @Schema(example = "4.7", description = "Promedio de las reseñas aprobadas, redondeado a un decimal") double puntuacion,
        @Schema(example = "3") long cantidad) {
}
