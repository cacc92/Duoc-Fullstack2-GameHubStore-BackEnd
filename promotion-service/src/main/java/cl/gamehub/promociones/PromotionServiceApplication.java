package cl.gamehub.promociones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * promotion-service: cupones de descuento con vigencia y tope de usos. No llama a otros servicios, por eso
 * no lleva @EnableFeignClients: el carrito valida aquí el cupón y order-service lo vuelve a validar al comprar
 * y le suma un uso. scanBasePackages = "cl.gamehub" incluye también las clases del módulo comun (errores, JWT, Swagger).
 */
@SpringBootApplication(scanBasePackages = "cl.gamehub")
public class PromotionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromotionServiceApplication.class, args);
    }
}
