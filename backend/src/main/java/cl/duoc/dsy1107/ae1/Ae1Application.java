package cl.duoc.dsy1107.ae1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * DSY1107 - EA1 - Backend de solicitudes de presupuesto detras del API Manager.
 *
 * Expone /presupuestos. La seguridad (JWT + scopes) vive en el API Gateway;
 * este servicio se dedica al dominio (crear, editar, eliminar, aprobar/rechazar).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Ae1Application {

    public static void main(String[] args) {
        SpringApplication.run(Ae1Application.class, args);
    }
}
