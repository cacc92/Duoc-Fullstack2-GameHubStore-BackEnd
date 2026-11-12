package cl.gamehub.ordenes.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** inventory-service: consulta el stock y lo descuenta cuando el pago queda aprobado. */
@FeignClient(name = "inventory-service", contextId = "inventarioClient", path = "/internal/inventario")
public interface InventarioClient {

    @GetMapping("/stock")
    List<DisponibilidadDto> stock(@RequestParam("ids") List<Long> ids);

    /** Todo o nada: si una línea no alcanza, responde 409 y no descuenta ninguna. */
    @PostMapping("/descontar")
    List<DisponibilidadDto> descontar(@RequestBody DescuentoStockSolicitud solicitud);
}
