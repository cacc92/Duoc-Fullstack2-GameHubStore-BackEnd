package cl.gamehub.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Unidades disponibles de un producto: lo que el catálogo del frontend junta con los datos de product-service. */
public record DisponibilidadDto(
        @Schema(example = "2") Long productoId,
        @Schema(example = "12") int stock) {
}
