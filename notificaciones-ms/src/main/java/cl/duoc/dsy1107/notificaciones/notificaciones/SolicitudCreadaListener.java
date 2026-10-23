package cl.duoc.dsy1107.notificaciones.notificaciones;

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

/** Dominio notificaciones: consume solicitud.creada con ACK manual. */
@Component
public class SolicitudCreadaListener {

    public static final String ID = "solicitud-creada";

    private static final Logger log = LoggerFactory.getLogger(SolicitudCreadaListener.class);

    private final LectorDeMensajes lector;
    private final NotificacionService notificaciones;
    private final ManejoDeFallos fallos;

    public SolicitudCreadaListener(
            LectorDeMensajes lector, NotificacionService notificaciones, ManejoDeFallos fallos) {
        this.lector = lector;
        this.notificaciones = notificaciones;
        this.fallos = fallos;
    }

    @RabbitListener(id = ID, queues = "${mensajeria.colas.solicitud-creada}")
    public void onSolicitudCreada(Message mensaje, Channel canal) throws IOException {
        long deliveryTag = mensaje.getMessageProperties().getDeliveryTag();
        try {
            SolicitudMensaje solicitud = lector.leer(mensaje);
            notificaciones.notificarCreacion(solicitud);
            canal.basicAck(deliveryTag, false);
            log.info("[{}] ACK solicitud={} messageId={}",
                    ID, solicitud.solicitudId(), mensaje.getMessageProperties().getMessageId());
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
