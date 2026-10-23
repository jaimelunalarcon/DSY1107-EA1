package cl.duoc.dsy1107.notificaciones.mensajeria;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Politica de NACK comun a todos los listeners (el ACK de exito lo hace cada
 * listener). Rutas de error:
 *
 * <ul>
 *   <li>No recuperable: NACK requeue=false, el broker lo manda a la DLQ.</li>
 *   <li>Recuperable con intentos disponibles: NACK requeue=true, vuelve a la cola.</li>
 *   <li>Recuperable sin intentos: NACK requeue=false, a la DLQ.</li>
 * </ul>
 *
 * El intento se lee de x-delivery-count, que la cola quorum incrementa en cada
 * reentrega. Si el consumidor muere sin responder, x-delivery-limit corta el ciclo.
 */
@Component
public class ManejoDeFallos {

    private static final Logger log = LoggerFactory.getLogger(ManejoDeFallos.class);

    static final String HEADER_DELIVERY_COUNT = "x-delivery-count";

    private final int maxIntentos;

    public ManejoDeFallos(MensajeriaProperties props) {
        this.maxIntentos = props.reintentos().maxIntentos();
    }

    public void enviarADlq(Channel canal, Message mensaje, String listener, Exception causa) throws IOException {
        log.error("[{}] NACK -> DLQ (no recuperable) messageId={} motivo={}",
                listener, messageId(mensaje), causa.getMessage());
        canal.basicNack(deliveryTag(mensaje), false, false);
    }

    public void reintentarOEnviarADlq(Channel canal, Message mensaje, String listener, Exception causa)
            throws IOException {
        int intento = intentoActual(mensaje);
        if (intento < maxIntentos) {
            log.warn("[{}] NACK + requeue intento {}/{} messageId={} motivo={}",
                    listener, intento, maxIntentos, messageId(mensaje), causa.getMessage());
            canal.basicNack(deliveryTag(mensaje), false, true);
        } else {
            log.error("[{}] NACK -> DLQ reintentos agotados ({}/{}) messageId={} motivo={}",
                    listener, intento, maxIntentos, messageId(mensaje), causa.getMessage());
            canal.basicNack(deliveryTag(mensaje), false, false);
        }
    }

    /** 1 en la primera entrega; la cola quorum agrega el header desde la segunda. */
    static int intentoActual(Message mensaje) {
        Object reentregas = mensaje.getMessageProperties().getHeaders().get(HEADER_DELIVERY_COUNT);
        return reentregas instanceof Number n ? n.intValue() + 1 : 1;
    }

    static long deliveryTag(Message mensaje) {
        return mensaje.getMessageProperties().getDeliveryTag();
    }

    static String messageId(Message mensaje) {
        return mensaje.getMessageProperties().getMessageId();
    }
}
