package cl.gamehub.despachos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * shipping-service: despachos de las órdenes pagadas y su seguimiento. No llama a otros servicios, por eso no
 * lleva @EnableFeignClients: order-service le pide generar y avanzar cada despacho, y el cliente lo sigue con su
 * código. scanBasePackages = "cl.gamehub" incluye también las clases del módulo comun (errores, JWT, Swagger).
 */
@SpringBootApplication(scanBasePackages = "cl.gamehub")
public class ShippingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShippingServiceApplication.class, args);
    }
}
