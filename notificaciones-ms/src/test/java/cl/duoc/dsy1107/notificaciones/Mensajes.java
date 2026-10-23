package cl.duoc.dsy1107.notificaciones;

import cl.duoc.dsy1107.notificaciones.mensajeria.LectorDeMensajes;
import cl.duoc.dsy1107.notificaciones.mensajeria.ManejoDeFallos;
import cl.duoc.dsy1107.notificaciones.mensajeria.MensajeriaProperties;
import cl.duoc.dsy1107.notificaciones.simulacion.SimulacionProperties;
import cl.duoc.dsy1107.notificaciones.simulacion.SimuladorDeFallos;
import jakarta.validation.Validation;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/** Fixtures para probar listeners sin broker. */
public final class Mensajes {

    public static final long TAG = 7L;

    public static final MensajeriaProperties PROPS = new MensajeriaProperties(
            "presupuestos.exchange",
            "presupuestos.dlx",
            new MensajeriaProperties.RoutingKeys("solicitud.creada", "solicitud.decidida", "solicitud.dead"),
            new MensajeriaProperties.Colas("c.creada", "c.decidida", "c.dlq"),
            3_600_000,
            604_800_000,
            new MensajeriaProperties.Reintentos(3, 5));

    private Mensajes() {
    }

    public static LectorDeMensajes lector() {
        return new LectorDeMensajes(
                JsonMapper.builder().build(),
                Validation.buildDefaultValidatorFactory().getValidator());
    }

    public static ManejoDeFallos fallos() {
        return new ManejoDeFallos(PROPS);
    }

    public static SimuladorDeFallos simulador() {
        return new SimuladorDeFallos(new SimulacionProperties("#temporal", "#permanente"));
    }

    public static String json(String titulo, String estado) {
        return """
                {"evento":"solicitud.creada","solicitudId":42,"titulo":"%s","monto":150000,
                 "solicitante":"t@duoc.cl","categoria":"SOFTWARE","estado":"%s",
                 "comentarioAdmin":null,"ocurridoEn":"2026-10-21T12:00:00Z"}"""
                .formatted(titulo, estado);
    }

    public static Message mensaje(String cuerpo) {
        return mensaje(cuerpo, null);
    }

    /** @param reentregas valor de x-delivery-count (null = primera entrega) */
    public static Message mensaje(String cuerpo, Long reentregas) {
        MessageProperties p = new MessageProperties();
        p.setDeliveryTag(TAG);
        p.setMessageId("msg-1");
        if (reentregas != null) {
            p.setHeader("x-delivery-count", reentregas);
        }
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), p);
    }
}
