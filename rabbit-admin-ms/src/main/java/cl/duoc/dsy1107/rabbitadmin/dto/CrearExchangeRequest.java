package cl.duoc.dsy1107.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/rabbitmq/exchanges. */
public record CrearExchangeRequest(
        @NotBlank(message = "el nombre del exchange es obligatorio")
        @Size(max = NombresRabbit.LARGO_MAXIMO)
        @Pattern(regexp = NombresRabbit.PATRON, message = NombresRabbit.MENSAJE)
        String nombre,

        @NotNull(message = "el tipo es obligatorio: DIRECT, TOPIC, FANOUT o HEADERS")
        TipoExchange tipo,

        Boolean durable,

        Boolean autoDelete) {

    public boolean durableEfectivo() {
        return durable == null || durable;
    }

    public boolean autoDeleteEfectivo() {
        return autoDelete != null && autoDelete;
    }
}
