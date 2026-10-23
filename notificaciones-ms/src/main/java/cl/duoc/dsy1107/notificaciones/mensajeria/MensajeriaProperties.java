package cl.duoc.dsy1107.notificaciones.mensajeria;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de colas, exchanges y routing keys de este microservicio. Unica
 * fuente: bloque {@code mensajeria} de application.yml. Ningun listener ni
 * servicio escribe un nombre de cola a mano.
 */
@Validated
@ConfigurationProperties(prefix = "mensajeria")
public record MensajeriaProperties(
        @NotBlank String exchange,
        @NotBlank String deadLetterExchange,
        @Valid @NotNull RoutingKeys routingKeys,
        @Valid @NotNull Colas colas,
        @Positive int ttlMs,
        @Positive int dlqTtlMs,
        @Valid @NotNull Reintentos reintentos) {

    public record RoutingKeys(
            @NotBlank String solicitudCreada,
            @NotBlank String solicitudDecidida,
            @NotBlank String deadLetter) {
    }

    public record Colas(
            @NotBlank String solicitudCreada,
            @NotBlank String solicitudDecidida,
            @NotBlank String deadLetter) {
    }

    /**
     * @param maxIntentos   intentos del consumidor ante errores recuperables
     * @param deliveryLimit tope del broker (x-delivery-limit), por si el consumidor
     *                      muere en medio del reintento
     */
    public record Reintentos(@Min(1) int maxIntentos, @Min(1) int deliveryLimit) {
    }
}
