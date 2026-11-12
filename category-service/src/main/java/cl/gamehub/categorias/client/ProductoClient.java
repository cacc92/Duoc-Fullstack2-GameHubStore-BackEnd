package cl.gamehub.categorias.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente Feign hacia product-service. name es el nombre con que ese servicio se registra en Eureka;
 * Feign pregunta ahí su dirección. La cabecera X-Clave-Interna la agrega FeignInternoConfig (comun).
 */
@FeignClient(name = "product-service", contextId = "productoClient", path = "/internal/productos")
public interface ProductoClient {

    /** Cantidad de productos por id de categoría, por ejemplo { "1": 1, "4": 5 }. */
    @GetMapping("/conteo")
    Map<Long, Long> conteoPorCategoria(@RequestParam("soloActivos") boolean soloActivos);
}
