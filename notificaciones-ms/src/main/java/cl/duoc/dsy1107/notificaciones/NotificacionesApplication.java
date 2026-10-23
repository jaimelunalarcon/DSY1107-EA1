package cl.duoc.dsy1107.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * DSY1107 - EP2 - Consumidor de eventos de solicitudes de presupuesto.
 *
 * Dominios: notificaciones (solicitud creada) y tickets (solicitud decidida).
 * La topologia vive en el paquete mensajeria; los listeners solo procesan.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class NotificacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificacionesApplication.class, args);
    }
}
