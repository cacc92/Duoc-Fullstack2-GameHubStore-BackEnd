package cl.gamehub.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Registro de servicios. Cada microservicio se anota aquí al arrancar con su nombre (product-service,
 * order-service...) y su dirección. El Gateway y los clientes Feign preguntan aquí dónde está cada uno,
 * así nadie escribe puertos a mano. Panel: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
