package cl.gamehub.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * payment-service: pagos simulados de las órdenes. No llama a otros servicios, por eso no lleva
 * @EnableFeignClients: order-service le pide registrar el pago de cada orden y el operador (a través de
 * order-service) confirma o anula las transferencias. scanBasePackages = "cl.gamehub" incluye también las
 * clases del módulo comun (errores, JWT, Swagger).
 */
@SpringBootApplication(scanBasePackages = "cl.gamehub")
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
