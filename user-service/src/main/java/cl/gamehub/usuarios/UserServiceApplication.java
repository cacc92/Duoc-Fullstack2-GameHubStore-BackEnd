package cl.gamehub.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * user-service: el perfil de cada cuenta (nombre, teléfono y dirección de despacho). El id del perfil es el
 * de la credencial en auth-service, que lo crea al registrar la cuenta (POST /internal/perfiles).
 * No llama a otros servicios, por eso no lleva @EnableFeignClients.
 * scanBasePackages = "cl.gamehub" incluye también las clases del módulo comun (errores, JWT, Swagger).
 */
@SpringBootApplication(scanBasePackages = "cl.gamehub")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
