package cl.gamehub.productos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** Lo que ve el mantenedor de productos: los precios tal como están guardados, no los vigentes de hoy. */
public record ProductoAdminDto(
        @Schema(example = "2") Long id,
        @Schema(example = "GPU-VOL-X70") String sku,
        @Schema(example = "Tarjeta gráfica Voltra X70 OC 12 GB") String nombre,
        @Schema(example = "Voltra") String marca,
        @Schema(example = "2") Long categoriaId,
        @Schema(example = "Tarjetas gráficas") String categoria,
        @Schema(example = "649990") int precio,
        @Schema(example = "729990") Integer precioNormal,
        @Schema(example = "2026-12-01") LocalDate ofertaHasta,
        @Schema(example = "true") boolean activo,
        @Schema(example = "gpu") String imagen,
        @Schema(example = "12 GB GDDR6X · PCIe 4.0") String resumen,
        @Schema(example = "Tarjeta gráfica de 12 GB con overclock de fábrica y doble ventilador.") String descripcion,
        List<EspecificacionDto> especificaciones) {
}
