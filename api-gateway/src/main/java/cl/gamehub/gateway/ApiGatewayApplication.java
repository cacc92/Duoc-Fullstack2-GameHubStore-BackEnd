package cl.gamehub.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Única puerta de entrada del frontend (http://localhost:8080). Las rutas están en application.yml:
 * cada prefijo /api/... va al microservicio que lo atiende, que se busca en Eureka por su nombre.
 * Aquí también se resuelve el CORS, así los servicios no necesitan configurarlo.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
