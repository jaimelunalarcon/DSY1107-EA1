package cl.duoc.dsy1107.ae1.mensajeria;

import cl.duoc.dsy1107.ae1.presupuestos.CategoriaSolicitud;
import cl.duoc.dsy1107.ae1.presupuestos.EstadoSolicitud;
import cl.duoc.dsy1107.ae1.presupuestos.SolicitudEvento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SolicitudEventoPublisherTest {

    private static final MensajeriaProperties PROPS = new MensajeriaProperties(
            true,
            "presupuestos.exchange",
            new MensajeriaProperties.RoutingKeys("solicitud.creada", "solicitud.decidida"));

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final SolicitudEventoPublisher publisher = new SolicitudEventoPublisher(rabbit, PROPS);

    private static SolicitudEvento evento(SolicitudEvento.Tipo tipo, EstadoSolicitud estado) {
        return new SolicitudEvento(tipo, 7L, "Licencia IDE", 90000, "t@duoc.cl",
                CategoriaSolicitud.SOFTWARE, estado, null, Instant.parse("2026-10-21T12:00:00Z"));
    }

    @Test
    @DisplayName("CREADA sale por presupuestos.exchange con routing key solicitud.creada")
    void publicaCreada() {
        publisher.publicar(evento(SolicitudEvento.Tipo.CREADA, EstadoSolicitud.PENDIENTE));

        ArgumentCaptor<SolicitudMensaje> mensaje = ArgumentCaptor.forClass(SolicitudMensaje.class);
        verify(rabbit).convertAndSend(eq("presupuestos.exchange"), eq("solicitud.creada"),
                mensaje.capture(), any(MessagePostProcessor.class), any(CorrelationData.class));
        assertThat(mensaje.getValue().solicitudId()).isEqualTo(7L);
        assertThat(mensaje.getValue().evento()).isEqualTo("solicitud.creada");
        assertThat(mensaje.getValue().categoria()).isEqualTo("SOFTWARE");
    }

    @Test
    @DisplayName("DECIDIDA usa la routing key solicitud.decidida")
    void publicaDecidida() {
        publisher.publicar(evento(SolicitudEvento.Tipo.DECIDIDA, EstadoSolicitud.RECHAZADA));

        verify(rabbit).convertAndSend(eq("presupuestos.exchange"), eq("solicitud.decidida"),
                any(SolicitudMensaje.class), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    @DisplayName("si el broker no responde no propaga la excepcion al negocio")
    void brokerCaido() {
        doThrow(new AmqpConnectException(new ConnectException("refused")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatNoException().isThrownBy(
                () -> publisher.publicar(evento(SolicitudEvento.Tipo.CREADA, EstadoSolicitud.PENDIENTE)));
    }
}
