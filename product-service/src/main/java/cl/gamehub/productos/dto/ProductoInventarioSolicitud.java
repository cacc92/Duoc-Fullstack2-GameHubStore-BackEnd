package cl.gamehub.productos.dto;

/**
 * Lo que se envía a inventory-service (PUT /internal/inventario/productos/{productoId}) cuando cambian el
 * SKU, el nombre o el estado de un producto. El stock no viaja: ese dato es de inventory-service.
 */
public record ProductoInventarioSolicitud(String sku, String nombre, boolean activo) {
}
