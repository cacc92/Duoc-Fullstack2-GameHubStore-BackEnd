package cl.gamehub.categorias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * category-service: categorías del catálogo. Pregunta a product-service cuántos productos tiene cada
 * una (para los contadores del catálogo y para impedir borrar una categoría con productos).
 * scanBasePackages = "cl.gamehub" incluye también las clases del módulo comun (errores, JWT, Swagger).
 */
@SpringBootApplication(scanBasePackages = "cl.gamehub")
@EnableFeignClients
public class CategoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CategoryServiceApplication.class, args);
    }
}
