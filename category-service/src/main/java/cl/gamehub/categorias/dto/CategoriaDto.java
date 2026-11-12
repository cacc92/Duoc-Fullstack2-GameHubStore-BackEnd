package cl.gamehub.categorias.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Lo que muestra el catálogo: la categoría y cuántos productos tiene. */
public record CategoriaDto(
        @Schema(example = "2") Long id,
        @Schema(example = "Tarjetas gráficas") String nombre,
        @Schema(example = "tarjetas-graficas") String slug,
        @Schema(example = "1", description = "En la ruta pública cuenta solo productos activos") long cantidadProductos) {
}
