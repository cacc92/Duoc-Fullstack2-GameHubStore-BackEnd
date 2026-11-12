package cl.gamehub.productos.client;

import cl.gamehub.productos.dto.ProductoInventarioSolicitud;
import cl.gamehub.productos.dto.StockNuevoSolicitud;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign hacia inventory-service. El stock de cada producto vive allá; este servicio solo le avisa
 * cuando se crea un producto o cambian su SKU, su nombre o su estado. La respuesta no se usa (void).
 */
@FeignClient(name = "inventory-service", contextId = "inventarioClient", path = "/internal/inventario/productos")
public interface InventarioClient {

    /** Crea el registro de stock del producto nuevo. 409 si ya existía. */
    @PostMapping
    void crearStock(@RequestBody StockNuevoSolicitud solicitud);

    /** Actualiza la copia del SKU, el nombre y el estado que guarda inventory-service. */
    @PutMapping("/{productoId}")
    void actualizarProducto(@PathVariable("productoId") Long productoId, @RequestBody ProductoInventarioSolicitud solicitud);
}
