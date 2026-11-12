package cl.gamehub.ordenes.dto;

/** precio es el que se cobraría hoy; mensaje explica por qué una línea no está disponible. */
public record LineaValidadaDto(Long productoId, String nombre, int precio, Integer precioNormal, int stock,
                               int cantidad, boolean disponible, String mensaje) {
}
