package cl.duoc.dsy1107.notificaciones.tickets;

import cl.duoc.dsy1107.notificaciones.Mensajes;
import com.rabbitmq.client.Channel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static cl.duoc.dsy1107.notificaciones.Mensajes.TAG;
import static cl.duoc.dsy1107.notificaciones.Mensajes.json;
import static cl.duoc.dsy1107.notificaciones.Mensajes.mensaje;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SolicitudDecididaListenerTest {

    private final Channel canal = mock(Channel.class);
    private final SolicitudDecididaListener listener = new SolicitudDecididaListener(
            Mensajes.lector(), new TicketService(Mensajes.simulador()), Mensajes.fallos());

    @Test
    @DisplayName("ticket generado -> basicAck")
    void ack() throws Exception {
        listener.onSolicitudDecidida(mensaje(json("Licencia IDE", "APROBADA")), canal);

        verify(canal).basicAck(TAG, false);
    }

    @Test
    @DisplayName("sistema de tickets caido en el primer intento -> NACK con requeue")
    void reintenta() throws Exception {
        listener.onSolicitudDecidida(mensaje(json("Viaje #temporal", "RECHAZADA")), canal);

        verify(canal).basicNack(TAG, false, true);
    }

    @Test
    @DisplayName("datos rechazados por el sistema de tickets -> DLQ")
    void dlq() throws Exception {
        listener.onSolicitudDecidida(mensaje(json("Viaje #permanente", "RECHAZADA")), canal);

        verify(canal).basicNack(TAG, false, false);
    }
}
