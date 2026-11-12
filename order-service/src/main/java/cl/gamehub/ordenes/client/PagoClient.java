package cl.gamehub.ordenes.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** payment-service: registra el pago simulado y lo confirma, anula o revierte. */
@FeignClient(name = "payment-service", contextId = "pagoClient", path = "/internal/pagos")
public interface PagoClient {

    @PostMapping
    PagoDto crear(@RequestBody NuevoPagoSolicitud solicitud);

    @PostMapping("/orden/{ordenId}/confirmar")
    PagoDto confirmar(@PathVariable("ordenId") Long ordenId);

    @PostMapping("/orden/{ordenId}/anular")
    PagoDto anular(@PathVariable("ordenId") Long ordenId);

    /** Compensación: deshace un pago aprobado cuando la compra no se pudo completar. */
    @PostMapping("/orden/{ordenId}/revertir")
    PagoDto revertir(@PathVariable("ordenId") Long ordenId);

    @GetMapping
    List<PagoDto> buscar(@RequestParam("ordenes") List<Long> ordenes);
}
