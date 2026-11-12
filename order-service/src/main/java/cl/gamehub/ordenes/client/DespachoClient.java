package cl.gamehub.ordenes.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** shipping-service: genera el despacho y lo hace avanzar hasta la entrega. */
@FeignClient(name = "shipping-service", contextId = "despachoClient", path = "/internal/despachos")
public interface DespachoClient {

    @PostMapping
    DespachoDto crear(@RequestBody NuevoDespachoSolicitud solicitud);

    @PutMapping("/orden/{ordenId}")
    DespachoDto actualizar(@PathVariable("ordenId") Long ordenId, @RequestBody CambioDespachoSolicitud solicitud);

    @GetMapping
    List<DespachoDto> buscar(@RequestParam("ordenes") List<Long> ordenes);
}
