package cl.duoc.dsy1107.notificaciones.mensajeria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Levanta el contexto sin broker (listeners sin auto-startup) y revisa la topologia. */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class RabbitTopologiaConfigTest {

    @Autowired
    @Qualifier("colaSolicitudCreada")
    private Queue colaCreada;

    @Autowired
    @Qualifier("colaDeadLetter")
    private Queue dlq;

    @Autowired
    @Qualifier("bindingSolicitudDecidida")
    private Binding bindingDecidida;

    @Test
    @DisplayName("colas de negocio: quorum, con DLX y delivery-limit desde application.yml")
    void colaDeNegocio() {
        assertThat(colaCreada.getName()).isEqualTo("presupuestos.notificaciones.solicitud-creada");
        assertThat(colaCreada.getArguments())
                .containsEntry("x-queue-type", "quorum")
                .containsEntry("x-dead-letter-exchange", "presupuestos.dlx")
                .containsEntry("x-dead-letter-routing-key", "solicitud.dead")
                .containsEntry("x-delivery-limit", 5);
    }

    @Test
    @DisplayName("la DLQ tambien es quorum")
    void dlqQuorum() {
        assertThat(dlq.getName()).isEqualTo("presupuestos.dlq");
        assertThat(dlq.getArguments()).containsEntry("x-queue-type", "quorum");
    }

    @Test
    @DisplayName("binding de decididas usa la routing key del yml")
    void bindingDecidida() {
        assertThat(bindingDecidida.getExchange()).isEqualTo("presupuestos.exchange");
        assertThat(bindingDecidida.getRoutingKey()).isEqualTo("solicitud.decidida");
        assertThat(bindingDecidida.getDestination()).isEqualTo("presupuestos.tickets.solicitud-decidida");
    }
}
