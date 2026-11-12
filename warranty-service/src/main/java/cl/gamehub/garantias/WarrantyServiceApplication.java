package cl.gamehub.garantias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * warranty-service: solicitudes de garantía de productos entregados. Antes de aceptar una pregunta a
 * order-service por la orden (que sea del cliente, que traiga el producto y cuándo se entregó).
 * scanBasePackages = "cl.gamehub" incluye también las clases del módulo comun (errores, JWT, Swagger).
 */
@SpringBootApplication(scanBasePackages = "cl.gamehub")
@EnableFeignClients
public class WarrantyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarrantyServiceApplication.class, args);
    }
}
