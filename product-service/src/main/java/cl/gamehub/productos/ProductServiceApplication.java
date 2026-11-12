package cl.gamehub.productos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * product-service: productos, precios, ofertas y especificaciones técnicas. No guarda stock (eso es de
 * inventory-service): le pide a category-service el nombre de cada categoría y le avisa a inventory-service
 * cuando se crea o cambia un producto.
 * scanBasePackages = "cl.gamehub" incluye también las clases del módulo comun (errores, JWT, Swagger).
 */
@SpringBootApplication(scanBasePackages = "cl.gamehub")
@EnableFeignClients
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
