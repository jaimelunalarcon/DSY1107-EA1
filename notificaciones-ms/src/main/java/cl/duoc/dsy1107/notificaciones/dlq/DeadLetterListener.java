package cl.duoc.dsy1107.notificaciones.dlq;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Auditor de la DLQ: deja en el log por que murio cada mensaje (x-death) y lo
 * confirma. Para ver los mensajes acumulados en la UI de RabbitMQ, detener este
 * listener con POST /api/listeners/dead-letter/detener.
 */
@Component
public class DeadLetterListener {

    public static final String ID = "dead-letter";

    private static final Logger log = LoggerFactory.getLogger(DeadLetterListener.class);

    @RabbitListener(id = ID, queues = "${mensajeria.colas.dead-letter}")
    public void onMensajeMuerto(Message mensaje, Channel canal) throws IOException {
        MessageProperties p = mensaje.getMessageProperties();
        try {
            Map<String, ?> muerte = primeraMuerte(p);
            log.error("[DLQ] messageId={} motivo={} colaOrigen={} routingKey={} veces={} cuerpo={}",
                    p.getMessageId(),
                    muerte.get("reason"),
                    muerte.get("queue"),
                    muerte.get("routing-keys"),
                    muerte.get("count"),
                    new String(mensaje.getBody(), StandardCharsets.UTF_8));
            canal.basicAck(p.getDeliveryTag(), false);
        } catch (RuntimeException e) {
            log.error("[DLQ] no se pudo auditar messageId={}, queda en la DLQ: {}", p.getMessageId(), e.getMessage());
            canal.basicNack(p.getDeliveryTag(), false, true);
        }
    }

    private static Map<String, ?> primeraMuerte(MessageProperties p) {
        List<Map<String, ?>> xDeath = p.getXDeathHeader();
        return xDeath == null || xDeath.isEmpty() ? Map.of() : xDeath.getFirst();
    }
}
