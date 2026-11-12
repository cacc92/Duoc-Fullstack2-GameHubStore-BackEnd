package cl.gamehub.ordenes.dto;

/** Línea comprada. Si se puede reseñar o pedir garantía lo resuelve el frontend con review-service y warranty-service. */
public record LineaOrdenDto(Long productoId, String nombre, String imagen, int precioUnitario, int cantidad, int subtotal) {
}
