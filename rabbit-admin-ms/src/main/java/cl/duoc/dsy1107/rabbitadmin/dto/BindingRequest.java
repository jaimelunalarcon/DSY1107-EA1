package cl.duoc.dsy1107.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Enlace exchange -> cola. Cuerpo de POST /api/rabbitmq/bindings y parametros
 * de DELETE /api/rabbitmq/bindings.
 */
public record BindingRequest(
        @NotBlank(message = "el exchange es obligatorio")
        @Size(max = NombresRabbit.LARGO_MAXIMO)
        @Pattern(regexp = NombresRabbit.PATRON, message = NombresRabbit.MENSAJE)
        String exchange,

        @NotBlank(message = "la cola es obligatoria")
        @Size(max = NombresRabbit.LARGO_MAXIMO)
        @Pattern(regexp = NombresRabbit.PATRON, message = NombresRabbit.MENSAJE)
        String cola,

        @NotNull(message = "la routing key es obligatoria (puede ser \"\" para exchanges fanout)")
        @Size(max = NombresRabbit.LARGO_MAXIMO_ROUTING_KEY)
        @Pattern(regexp = NombresRabbit.PATRON_ROUTING_KEY, message = NombresRabbit.MENSAJE_ROUTING_KEY)
        String routingKey) {
}
