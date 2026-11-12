package cl.gamehub.productos.dto;

/**
 * Lo que se envía a inventory-service (POST /internal/inventario/productos) al crear un producto: el
 * stock vive allá, junto con una copia del SKU, el nombre y el estado para el panel del operador.
 */
public record StockNuevoSolicitud(Long productoId, String sku, String nombre, boolean activo, int stockInicial) {
}
