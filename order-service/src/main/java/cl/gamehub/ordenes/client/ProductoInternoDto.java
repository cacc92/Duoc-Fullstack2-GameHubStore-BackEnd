package cl.gamehub.ordenes.client;

/** precio y precioNormal son los vigentes hoy, ya calculados por product-service. */
public record ProductoInternoDto(Long id, String sku, String nombre, String imagen, int precio, Integer precioNormal,
                                 boolean activo) {
}
