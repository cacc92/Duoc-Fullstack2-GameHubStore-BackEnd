package cl.gamehub.gateway;

import java.util.List;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rutas de la documentación, escritas con el DSL de Java del Gateway (las de la API están en
 * application.yml). Cada servicio publica su OpenAPI en /v3/api-docs; el Gateway lo expone en
 * /docs/<servicio>/v3/api-docs y el Swagger UI del Gateway los lista todos.
 */
@Configuration
public class DocumentacionConfig {

    static final List<String> SERVICIOS = List.of(
            "auth-service", "user-service", "category-service", "product-service", "inventory-service",
            "promotion-service", "order-service", "payment-service", "shipping-service", "review-service",
            "warranty-service");

    @Bean
    public RouteLocator rutasDocumentacion(RouteLocatorBuilder constructor) {
        RouteLocatorBuilder.Builder rutas = constructor.routes();
        for (String servicio : SERVICIOS) {
            rutas.route("docs-" + servicio, r -> r
                    .path("/docs/" + servicio + "/v3/api-docs")
                    .filters(f -> f.setPath("/v3/api-docs"))
                    .uri("lb://" + servicio));
        }
        return rutas.build();
    }
}
