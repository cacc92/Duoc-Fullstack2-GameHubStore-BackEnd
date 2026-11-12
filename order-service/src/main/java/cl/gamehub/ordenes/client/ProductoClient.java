package cl.gamehub.ordenes.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** product-service: nombre, imagen y precio vigente de los productos del carrito. */
@FeignClient(name = "product-service", contextId = "productoClient", path = "/internal/productos")
public interface ProductoClient {

    @GetMapping
    List<ProductoInternoDto> buscar(@RequestParam("ids") List<Long> ids);
}
