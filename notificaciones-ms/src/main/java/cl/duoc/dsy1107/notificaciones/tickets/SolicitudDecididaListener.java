package cl.duoc.dsy1107.notificaciones.tickets;

import cl.duoc.dsy1107.notificaciones.mensajeria.ErrorNoRecuperableException;
import cl.duoc.dsy1107.notificaciones.mensajeria.ErrorRecuperableException;
import cl.duoc.dsy1107.notificaciones.mensajeria.LectorDeMensajes;
import cl.duoc.dsy1107.notificaciones.mensajeria.ManejoDeFallos;
import cl.duoc.dsy1107.notificaciones.mensajeria.SolicitudMensaje;
import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Dominio tickets: consume solicitud.decidida con ACK manual. */
@Component
public class SolicitudDecididaListener {

    public static final String ID = "solicitud-decidida";

    private static final Logger log = LoggerFactory.getLogger(SolicitudDecididaListener.class);

    private final LectorDeMensajes lector;
    private final TicketService tickets;
    private final ManejoDeFallos fallos;

    public SolicitudDecididaListener(LectorDeMensajes lector, TicketService tickets, ManejoDeFallos fallos) {
        this.lector = lector;
        this.tickets = tickets;
        this.fallos = fallos;
    }

    @RabbitListener(id = ID, queues = "${mensajeria.colas.solicitud-decidida}")
    public void onSolicitudDecidida(Message mensaje, Channel canal) throws IOException {
        long deliveryTag = mensaje.getMessageProperties().getDeliveryTag();
        try {
            SolicitudMensaje solicitud = lector.leer(mensaje);
            String ticket = tickets.generarTicket(solicitud);
            canal.basicAck(deliveryTag, false);
            log.info("[{}] ACK solicitud={} ticket={} messageId={}",
                    ID, solicitud.solicitudId(), ticket, mensaje.getMessageProperties().getMessageId());
        } catch (ErrorRecuperableException e) {
            fallos.reintentarOEnviarADlq(canal, mensaje, ID, e);
        } catch (ErrorNoRecuperableException e) {
            fallos.enviarADlq(canal, mensaje, ID, e);
        } catch (RuntimeException e) {
            // Error no previsto: reintentar podria repetirlo sin fin, se aisla en la DLQ.
            fallos.enviarADlq(canal, mensaje, ID, e);
        }
    }
}
