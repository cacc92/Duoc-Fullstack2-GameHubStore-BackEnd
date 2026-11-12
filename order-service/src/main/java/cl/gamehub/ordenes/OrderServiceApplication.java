package cl.gamehub.ordenes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * order-service: carrito, órdenes y bandeja del operador. Es el servicio que coordina la compra:
 * pregunta precios a product-service, stock a inventory-service y cupones a promotion-service, cobra en
 * payment-service, descuenta el stock y pide el despacho a shipping-service.
 */
@SpringBootApplication(scanBasePackages = "cl.gamehub")
@EnableFeignClients
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
