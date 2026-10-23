package cl.duoc.dsy1107.notificaciones.dlq;

import com.rabbitmq.client.Channel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

import java.util.List;
import java.util.Map;

import static cl.duoc.dsy1107.notificaciones.Mensajes.TAG;
import static cl.duoc.dsy1107.notificaciones.Mensajes.json;
import static cl.duoc.dsy1107.notificaciones.Mensajes.mensaje;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeadLetterListenerTest {

    private final Channel canal = mock(Channel.class);
    private final DeadLetterListener listener = new DeadLetterListener();

    @Test
    @DisplayName("registra el x-death y confirma el mensaje muerto")
    void auditaYConfirma() throws Exception {
        Message muerto = mensaje(json("Compra #permanente", "PENDIENTE"));
        muerto.getMessageProperties().setHeader("x-death", List.of(Map.of(
                "reason", "rejected",
                "queue", "presupuestos.notificaciones.solicitud-creada",
                "routing-keys", List.of("solicitud.creada"),
                "count", 1L)));

        listener.onMensajeMuerto(muerto, canal);

        verify(canal).basicAck(TAG, false);
    }

    @Test
    @DisplayName("sin header x-death igual confirma")
    void sinXDeath() throws Exception {
        listener.onMensajeMuerto(mensaje("{}"), canal);

        verify(canal).basicAck(TAG, false);
    }
}
