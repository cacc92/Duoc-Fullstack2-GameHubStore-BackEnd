package cl.gamehub.comun.docs;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger de cada servicio en /swagger-ui.html. El Gateway junta todos en http://localhost:8080/swagger-ui.html.
 * El servidor "/" hace que «Try it out» llame al mismo origen desde el que se abrió la documentación:
 * a través del Gateway, o directo al servicio.
 */
@Configuration
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiComun {

    private static final String ESQUEMA_ERROR = "ErrorRespuesta";

    @Bean
    public OpenAPI documentacion(@Value("${spring.application.name}") String servicio,
                                 @Value("${app.docs.descripcion:}") String descripcion) {
        return new OpenAPI()
                .info(new Info()
                        .title("GameHub Store · " + servicio)
                        .version("1.0")
                        .description(descripcion + " Para probar una ruta protegida, inicia sesión en "
                                + "POST /api/auth/login (auth-service), copia el token y pégalo en Authorize."))
                .servers(List.of(new Server().url("/").description("Mismo origen: el Gateway o el servicio")));
    }

    /**
     * Todas las respuestas de error de la API tienen la misma forma (ManejadorErrores). Sin esto, Swagger
     * mostraría en los códigos 4xx y 5xx el mismo cuerpo que en la respuesta exitosa.
     */
    @Bean
    public OpenApiCustomizer erroresConFormatoComun() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            Schema<?> error = new ObjectSchema()
                    .description("Formato de todos los errores: un mensaje para el usuario y, si fallaron campos, el detalle de cada uno")
                    .addProperty("mensaje", new StringSchema().example("Revisa los datos enviados."))
                    .addProperty("errores", new MapSchema().additionalProperties(new StringSchema())
                            .example(Map.of("telefono", "El teléfono debe tener 9 dígitos y empezar con 9.")));
            openApi.getComponents().addSchemas(ESQUEMA_ERROR, error);
            Content contenido = new Content().addMediaType("application/json",
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ESQUEMA_ERROR)));
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(ruta -> ruta.readOperations().forEach(operacion -> {
                if (operacion.getResponses() != null) {
                    operacion.getResponses().forEach((codigo, respuesta) -> {
                        if (codigo.startsWith("4") || codigo.startsWith("5")) {
                            respuesta.setContent(contenido);
                        }
                    });
                }
            }));
        };
    }
}
