package cl.duoc.dsy1107.ae1.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia del lado productor: solo el exchange de presupuestos. Las colas,
 * bindings y la DLQ son de quien consume (notificaciones-ms).
 */
@Configuration
@ConditionalOnProperty(prefix = "mensajeria", name = "habilitada", havingValue = "true", matchIfMissing = true)
public class RabbitMensajeriaConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMensajeriaConfig.class);

    @Bean
    DirectExchange presupuestosExchange(MensajeriaProperties props) {
        return ExchangeBuilder.directExchange(props.exchange()).durable(true).build();
    }

    @Bean
    MessageConverter mensajeriaJsonConverter() {
        return new JacksonJsonMessageConverter();
    }

    /** Mensajes que el broker no pudo enrutar o no confirmo quedan en el log. */
    @Bean
    RabbitTemplateCustomizer confirmacionesDePublicacion() {
        return template -> {
            template.setMandatory(true);
            template.setReturnsCallback(devuelto -> log.warn(
                    "Mensaje NO enrutado (sin cola enlazada): exchange={} routingKey={} codigo={} motivo={}",
                    devuelto.getExchange(),
                    devuelto.getRoutingKey(),
                    devuelto.getReplyCode(),
                    devuelto.getReplyText()));
            template.setConfirmCallback((correlacion, ack, causa) -> {
                String id = correlacion == null ? "?" : correlacion.getId();
                if (ack) {
                    log.debug("Broker confirmo el mensaje {}", id);
                } else {
                    log.warn("Broker rechazo el mensaje {}: {}", id, causa);
                }
            });
        };
    }
}
