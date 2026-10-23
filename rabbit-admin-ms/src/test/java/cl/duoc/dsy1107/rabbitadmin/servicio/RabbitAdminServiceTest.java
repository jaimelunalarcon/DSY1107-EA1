package cl.duoc.dsy1107.rabbitadmin.servicio;

import cl.duoc.dsy1107.rabbitadmin.config.RabbitAdminProperties;
import cl.duoc.dsy1107.rabbitadmin.dto.CrearColaRequest;
import cl.duoc.dsy1107.rabbitadmin.dto.TipoCola;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RabbitAdminServiceTest {

    private final AmqpAdmin admin = mock(AmqpAdmin.class);
    private final RabbitAdminService servicio = new RabbitAdminService(
            admin,
            mock(RabbitTemplate.class),
            new RabbitAdminProperties(List.of("presupuestos."), new RabbitAdminProperties.ColaPorDefecto(5)));

    private static CrearColaRequest cola(String nombre, TipoCola tipo, Integer deliveryLimit) {
        return new CrearColaRequest(nombre, tipo, null, deliveryLimit, 60_000, null, "demo.dlx", "demo.dead");
    }

    @Test
    @DisplayName("por defecto crea una cola quorum durable con el delivery-limit configurado")
    void quorumPorDefecto() {
        Queue q = servicio.construirCola(cola("demo.q", null, null));

        assertThat(q.isDurable()).isTrue();
        assertThat(q.getArguments())
                .containsEntry("x-queue-type", "quorum")
                .containsEntry("x-delivery-limit", 5)
                .containsEntry("x-message-ttl", 60_000)
                .containsEntry("x-dead-letter-exchange", "demo.dlx")
                .containsEntry("x-dead-letter-routing-key", "demo.dead");
    }

    @Test
    @DisplayName("CLASSIC declara x-queue-type explicito (el cluster usa quorum por defecto)")
    void classicExplicita() {
        Queue q = servicio.construirCola(cola("demo.c", TipoCola.CLASSIC, null));

        assertThat(q.getArguments())
                .containsEntry("x-queue-type", "classic")
                .doesNotContainKey("x-delivery-limit");
    }

    @Test
    @DisplayName("no deja borrar ni redeclarar recursos de negocio")
    void protegidos() {
        assertThatThrownBy(() -> servicio.eliminarCola("presupuestos.dlq", false, false))
                .isInstanceOf(RecursoProtegidoException.class);
        assertThatThrownBy(() -> servicio.crearCola(cola("presupuestos.otra", null, null)))
                .isInstanceOf(RecursoProtegidoException.class);
        verifyNoInteractions(admin);
    }

    @Test
    @DisplayName("consultar una cola inexistente lanza RecursoNoEncontrado")
    void colaInexistente() {
        assertThatThrownBy(() -> servicio.infoCola("demo.nada"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("demo.nada");
    }
}
