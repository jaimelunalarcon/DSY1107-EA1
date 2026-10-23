package cl.duoc.dsy1107.notificaciones.mensajeria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ de notificaciones-ms, un bloque por caso de uso.
 *
 * <pre>
 * backend --solicitud.creada----> presupuestos.exchange --> presupuestos.notificaciones.solicitud-creada
 * backend --solicitud.decidida--> presupuestos.exchange --> presupuestos.tickets.solicitud-decidida
 *                     (NACK sin requeue / TTL / delivery-limit)
 *                                         |
 *                                         v
 *                 presupuestos.dlx --solicitud.dead--> presupuestos.dlq
 * </pre>
 *
 * Todas las colas son quorum: se replican en los 3 nodos del cluster y siguen
 * operativas si cae uno.
 */
@Configuration
public class RabbitTopologiaConfig {

    private final MensajeriaProperties props;

    public RabbitTopologiaConfig(MensajeriaProperties props) {
        this.props = props;
    }

    @Bean
    DirectExchange presupuestosExchange() {
        return ExchangeBuilder.directExchange(props.exchange()).durable(true).build();
    }

    // --- Caso de uso 1: solicitud creada -> notificar a los aprobadores ------

    @Bean
    Queue colaSolicitudCreada() {
        return colaDeNegocio(props.colas().solicitudCreada());
    }

    @Bean
    Binding bindingSolicitudCreada() {
        return BindingBuilder.bind(colaSolicitudCreada())
                .to(presupuestosExchange())
                .with(props.routingKeys().solicitudCreada());
    }

    // --- Caso de uso 2: solicitud decidida -> ticket de resolucion ------------

    @Bean
    Queue colaSolicitudDecidida() {
        return colaDeNegocio(props.colas().solicitudDecidida());
    }

    @Bean
    Binding bindingSolicitudDecidida() {
        return BindingBuilder.bind(colaSolicitudDecidida())
                .to(presupuestosExchange())
                .with(props.routingKeys().solicitudDecidida());
    }

    // --- Caso de uso 3: mensajes no entregados -> DLQ -------------------------

    @Bean
    DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(props.deadLetterExchange()).durable(true).build();
    }

    @Bean
    Queue colaDeadLetter() {
        return QueueBuilder.durable(props.colas().deadLetter())
                .quorum()
                .ttl(props.dlqTtlMs())
                .build();
    }

    @Bean
    Binding bindingDeadLetter() {
        return BindingBuilder.bind(colaDeadLetter())
                .to(deadLetterExchange())
                .with(props.routingKeys().deadLetter());
    }

    /** Quorum + DLX comun: lo rechazado, expirado o sobre el delivery-limit va a la DLQ. */
    private Queue colaDeNegocio(String nombre) {
        return QueueBuilder.durable(nombre)
                .quorum()
                .deliveryLimit(props.reintentos().deliveryLimit())
                .ttl(props.ttlMs())
                .deadLetterExchange(props.deadLetterExchange())
                .deadLetterRoutingKey(props.routingKeys().deadLetter())
                .build();
    }
}
