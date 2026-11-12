package cl.gamehub.resenas.repository;

/** Una fila del resumen que calcula la base: el promedio sin redondear y cuántas reseñas lo forman. */
public record PromedioProducto(Long productoId, Double promedio, Long cantidad) {
}
