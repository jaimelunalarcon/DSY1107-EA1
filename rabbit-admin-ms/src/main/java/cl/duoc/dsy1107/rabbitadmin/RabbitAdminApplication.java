package cl.duoc.dsy1107.rabbitadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * DSY1107 - EP2 - Administrador de RabbitMQ.
 *
 * El controlador solo traduce HTTP; toda operacion sobre el broker pasa por
 * {@link cl.duoc.dsy1107.rabbitadmin.servicio.RabbitAdminService}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RabbitAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitAdminApplication.class, args);
    }
}
