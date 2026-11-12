package cl.gamehub.productos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lo que necesita order-service para armar una orden: el precio que se cobra hoy y si el producto sigue
 * a la venta. Los inactivos también vienen (con activo = false) para que order-service pueda avisarlo.
 */
public record ProductoInternoDto(
        @Schema(example = "2") Long id,
        @Schema(example = "GPU-VOL-X70") String sku,
        @Schema(example = "Tarjeta gráfica Voltra X70 OC 12 GB") String nombre,
        @Schema(example = "gpu") String imagen,
        @Schema(example = "649990", description = "Precio que se cobra hoy") int precio,
        @Schema(example = "729990", description = "Precio tachado; null si hoy no hay oferta") Integer precioNormal,
        @Schema(example = "true") boolean activo) {
}
