package cl.duoc.dsy1107.notificaciones.notificaciones;

import cl.duoc.dsy1107.notificaciones.Mensajes;
import com.rabbitmq.client.Channel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static cl.duoc.dsy1107.notificaciones.Mensajes.TAG;
import static cl.duoc.dsy1107.notificaciones.Mensajes.json;
import static cl.duoc.dsy1107.notificaciones.Mensajes.mensaje;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SolicitudCreadaListenerTest {

    private final Channel canal = mock(Channel.class);
    private final SolicitudCreadaListener listener = new SolicitudCreadaListener(
            Mensajes.lector(), new NotificacionService(Mensajes.simulador()), Mensajes.fallos());

    @Test
    @DisplayName("procesado OK -> basicAck")
    void ack() throws Exception {
        listener.onSolicitudCreada(mensaje(json("Licencia IDE", "PENDIENTE")), canal);

        verify(canal).basicAck(TAG, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("error no recuperable -> NACK sin requeue (DLQ) al primer intento")
    void noRecuperable() throws Exception {
        listener.onSolicitudCreada(mensaje(json("Compra #permanente", "PENDIENTE")), canal);

        verify(canal).basicNack(TAG, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("error recuperable con intentos disponibles -> NACK con requeue")
    void recuperableReintenta() throws Exception {
        listener.onSolicitudCreada(mensaje(json("Compra #temporal", "PENDIENTE"), 1L), canal);

        verify(canal).basicNack(TAG, false, true);
    }

    @Test
    @DisplayName("error recuperable en el ultimo intento -> NACK sin requeue (DLQ)")
    void recuperableAgotado() throws Exception {
        listener.onSolicitudCreada(mensaje(json("Compra #temporal", "PENDIENTE"), 2L), canal);

        verify(canal).basicNack(TAG, false, false);
    }

    @Test
    @DisplayName("JSON corrupto -> NACK sin requeue")
    void jsonInvalido() throws Exception {
        listener.onSolicitudCreada(mensaje("{no es json"), canal);

        verify(canal).basicNack(TAG, false, false);
    }

    @Test
    @DisplayName("mensaje sin campos obligatorios -> NACK sin requeue")
    void incompleto() throws Exception {
        listener.onSolicitudCreada(mensaje("{\"evento\":\"solicitud.creada\"}"), canal);

        verify(canal).basicNack(TAG, false, false);
    }
}
