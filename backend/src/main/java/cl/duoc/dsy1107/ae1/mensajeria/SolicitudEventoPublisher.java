package cl.duoc.dsy1107.ae1.mensajeria;

import cl.duoc.dsy1107.ae1.presupuestos.SolicitudEvento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Productor: traduce eventos de dominio a mensajes en el exchange de presupuestos.
 *
 * Se publica despues del commit para no anunciar solicitudes que se revirtieron.
 * Si RabbitMQ no responde, la operacion HTTP ya quedo hecha: solo se registra
 * en el log (la mensajeria no debe tumbar el negocio).
 */
@Component
@ConditionalOnProperty(prefix = "mensajeria", name = "habilitada", havingValue = "true", matchIfMissing = true)
public class SolicitudEventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(SolicitudEventoPublisher.class);
    private static final String APP_ID = "presupuestos-backend";

    private final RabbitTemplate rabbit;
    private final MensajeriaProperties props;

    public SolicitudEventoPublisher(RabbitTemplate rabbit, MensajeriaProperties props) {
        this.rabbit = rabbit;
        this.props = props;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(SolicitudEvento evento) {
        String routingKey = routingKeyDe(evento.tipo());
        String messageId = UUID.randomUUID().toString();
        SolicitudMensaje mensaje = SolicitudMensaje.de(evento, routingKey);
        try {
            rabbit.convertAndSend(props.exchange(), routingKey, mensaje, m -> {
                m.getMessageProperties().setMessageId(messageId);
                m.getMessageProperties().setAppId(APP_ID);
                return m;
            }, new CorrelationData(messageId));
            log.info("Publicado {} solicitud={} messageId={}", routingKey, evento.id(), messageId);
        } catch (AmqpException e) {
            log.error("No se pudo publicar {} solicitud={}: {}", routingKey, evento.id(), e.getMessage());
        }
    }

    private String routingKeyDe(SolicitudEvento.Tipo tipo) {
        return switch (tipo) {
            case CREADA -> props.routingKeys().solicitudCreada();
            case DECIDIDA -> props.routingKeys().solicitudDecidida();
        };
    }
}
