package cl.duoc.dsy1107.ae1.mensajeria;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de la topologia RabbitMQ que usa este productor. Unica fuente de
 * verdad: bloque {@code mensajeria} de application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "mensajeria")
public record MensajeriaProperties(
        boolean habilitada,
        @NotBlank String exchange,
        @Valid @NotNull RoutingKeys routingKeys) {

    public record RoutingKeys(
            @NotBlank String solicitudCreada,
            @NotBlank String solicitudDecidida) {
    }
}
