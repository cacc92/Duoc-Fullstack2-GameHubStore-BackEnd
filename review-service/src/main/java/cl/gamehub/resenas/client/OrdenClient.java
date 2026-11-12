package cl.gamehub.resenas.client;

import cl.gamehub.resenas.dto.OrdenInternaDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign hacia order-service. name es el nombre con que ese servicio se registra en Eureka;
 * Feign pregunta ahí su dirección. La cabecera X-Clave-Interna la agrega FeignInternoConfig (comun).
 */
@FeignClient(name = "order-service", contextId = "ordenClient", path = "/internal/ordenes")
public interface OrdenClient {

    /**
     * La orden con su dueño, su estado y sus productos. Si no existe, order-service responde 404
     * «La orden no existe.» y ese error llega aquí como ApiException con el mismo código y mensaje.
     */
    @GetMapping("/{id}")
    OrdenInternaDto buscar(@PathVariable("id") Long id);
}
