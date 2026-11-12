package cl.gamehub.inventario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * inventory-service: stock de cada producto y registro de sus movimientos. No llama a otros servicios
 * (por eso no tiene @EnableFeignClients): product-service le avisa cuando se crea o cambia un producto y
 * order-service le pide descontar el stock de cada compra.
 * scanBasePackages = "cl.gamehub" incluye también las clases del módulo comun (errores, JWT, Swagger).
 */
@SpringBootApplication(scanBasePackages = "cl.gamehub")
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
