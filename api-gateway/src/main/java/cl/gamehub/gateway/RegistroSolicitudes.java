package cl.gamehub.gateway;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtro global: a cada petición le asigna un identificador X-Request-Id (que viaja al microservicio)
 * y deja en el log el método, la ruta, el servicio que respondió, el código y cuánto tardó.
 * Ejemplo: «GET /api/productos → product-service 200 (38 ms) [a1b2c3d4]».
 */
@Component
public class RegistroSolicitudes implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RegistroSolicitudes.class);
    public static final String CABECERA_ID = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange intercambio, GatewayFilterChain cadena) {
        long inicio = System.currentTimeMillis();
        String recibido = intercambio.getRequest().getHeaders().getFirst(CABECERA_ID);
        String id = recibido != null ? recibido : UUID.randomUUID().toString().substring(0, 8);
        ServerHttpRequest solicitud = intercambio.getRequest().mutate().header(CABECERA_ID, id).build();
        intercambio.getResponse().getHeaders().set(CABECERA_ID, id);

        return cadena.filter(intercambio.mutate().request(solicitud).build())
                .doFinally(senal -> {
                    Route ruta = intercambio.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                    var codigo = intercambio.getResponse().getStatusCode();
                    log.info("{} {} → {} {} ({} ms) [{}]",
                            solicitud.getMethod(), solicitud.getURI().getRawPath(),
                            ruta == null ? "sin ruta" : ruta.getId(),
                            codigo == null ? "-" : codigo.value(),
                            System.currentTimeMillis() - inicio, id);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
