package cl.gamehub.productos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Lo que muestra la tarjeta de un producto en el catálogo. precioNormal y ofertaHasta vienen solo si la
 * oferta sigue vigente. El frontend agrega el stock (inventory-service) y las reseñas (review-service).
 */
public record ProductoResumenDto(
        @Schema(example = "2") Long id,
        @Schema(example = "GPU-VOL-X70") String sku,
        @Schema(example = "Tarjeta gráfica Voltra X70 OC 12 GB") String nombre,
        @Schema(example = "Voltra") String marca,
        @Schema(example = "Tarjetas gráficas") String categoria,
        @Schema(example = "tarjetas-graficas") String categoriaSlug,
        @Schema(example = "649990", description = "Precio que se cobra hoy") int precio,
        @Schema(example = "729990", description = "Precio tachado; null si hoy no hay oferta") Integer precioNormal,
        @Schema(example = "2026-12-01", description = "Último día de la oferta; null si hoy no hay oferta") LocalDate ofertaHasta,
        @Schema(example = "gpu") String imagen,
        @Schema(example = "12 GB GDDR6X · PCIe 4.0") String resumen) {
}
