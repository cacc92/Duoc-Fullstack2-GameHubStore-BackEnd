package cl.gamehub.resenas.dto;

/** Un producto de la orden que informa order-service: su id y el nombre con que se compró. */
public record LineaOrdenInternaDto(Long productoId, String nombre) {
}
