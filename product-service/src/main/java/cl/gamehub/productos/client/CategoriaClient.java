package cl.gamehub.productos.client;

import cl.gamehub.productos.dto.CategoriaInternaDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign hacia category-service. name es el nombre con que ese servicio se registra en Eureka;
 * Feign pregunta ahí su dirección. La cabecera X-Clave-Interna la agrega FeignInternoConfig (comun).
 */
@FeignClient(name = "category-service", contextId = "categoriaClient", path = "/internal/categorias")
public interface CategoriaClient {

    /** Todas las categorías: con una sola llamada se obtiene el nombre y el slug de cada una. */
    @GetMapping
    List<CategoriaInternaDto> listar();

    /** Una categoría. Si no existe, category-service responde 404 y aquí llega como ApiException. */
    @GetMapping("/{id}")
    CategoriaInternaDto buscar(@PathVariable("id") Long id);
}
