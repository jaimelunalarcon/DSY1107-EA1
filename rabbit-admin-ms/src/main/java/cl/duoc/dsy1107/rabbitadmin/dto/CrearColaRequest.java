package cl.duoc.dsy1107.rabbitadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/rabbitmq/queues. Solo {@code nombre} es obligatorio; el
 * resto tiene valores por defecto seguros (quorum, durable).
 */
public record CrearColaRequest(
        @NotBlank(message = "el nombre de la cola es obligatorio")
        @Size(max = NombresRabbit.LARGO_MAXIMO)
        @Pattern(regexp = NombresRabbit.PATRON, message = NombresRabbit.MENSAJE)
        String nombre,

        TipoCola tipo,

        Boolean durable,

        @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 100, message = "no puede superar 100")
        Integer deliveryLimit,

        @Min(value = 1000, message = "debe ser al menos 1000 ms")
        @Max(value = 604_800_000, message = "no puede superar 7 dias")
        Integer ttlMs,

        @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 1_000_000, message = "no puede superar 1.000.000")
        Integer maxMensajes,

        @Size(max = NombresRabbit.LARGO_MAXIMO)
        @Pattern(regexp = NombresRabbit.PATRON, message = NombresRabbit.MENSAJE)
        String deadLetterExchange,

        @Size(max = NombresRabbit.LARGO_MAXIMO_ROUTING_KEY)
        @Pattern(regexp = NombresRabbit.PATRON_ROUTING_KEY, message = NombresRabbit.MENSAJE_ROUTING_KEY)
        String deadLetterRoutingKey) {

    public TipoCola tipoEfectivo() {
        return tipo == null ? TipoCola.QUORUM : tipo;
    }

    public boolean durableEfectivo() {
        return durable == null || durable;
    }

    @AssertTrue(message = "una cola QUORUM siempre es durable: no se puede pedir durable=false")
    public boolean isQuorumDurable() {
        return tipoEfectivo() != TipoCola.QUORUM || durableEfectivo();
    }

    @AssertTrue(message = "deliveryLimit solo aplica a colas QUORUM")
    public boolean isDeliveryLimitSoloQuorum() {
        return deliveryLimit == null || tipoEfectivo() == TipoCola.QUORUM;
    }

    @AssertTrue(message = "deadLetterRoutingKey requiere deadLetterExchange")
    public boolean isDeadLetterCompleto() {
        return deadLetterRoutingKey == null || deadLetterExchange != null;
    }
}
