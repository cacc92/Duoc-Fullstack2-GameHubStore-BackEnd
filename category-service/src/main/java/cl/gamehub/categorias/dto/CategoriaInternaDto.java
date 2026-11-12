package cl.gamehub.categorias.dto;

/** Lo que necesita product-service para mostrar el nombre de la categoría y validar que exista. */
public record CategoriaInternaDto(Long id, String nombre, String slug) {
}
