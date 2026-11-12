package cl.gamehub.ordenes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * promotion-service: valida el cupón y cuenta su uso. Si el cupón no sirve, responde 400 o 404 con un
 * mensaje como «El cupón CYBER25 venció el 01-11-2026.», que llega aquí como ApiException.
 */
@FeignClient(name = "promotion-service", contextId = "promocionClient", path = "/internal/promociones")
public interface PromocionClient {

    @GetMapping("/vigente")
    CuponDto vigente(@RequestParam("codigo") String codigo);

    @PostMapping("/{codigo}/usos")
    CuponDto registrarUso(@PathVariable("codigo") String codigo);
}
