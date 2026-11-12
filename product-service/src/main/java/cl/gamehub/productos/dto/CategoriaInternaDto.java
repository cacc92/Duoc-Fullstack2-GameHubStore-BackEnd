package cl.gamehub.productos.dto;

/**
 * Lo que responde category-service en /internal/categorias. Es una copia propia de este servicio: los
 * microservicios no comparten clases, solo el formato JSON.
 */
public record CategoriaInternaDto(Long id, String nombre, String slug) {
}
