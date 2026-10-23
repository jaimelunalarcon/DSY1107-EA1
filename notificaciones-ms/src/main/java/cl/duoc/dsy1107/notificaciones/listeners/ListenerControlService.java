package cl.duoc.dsy1107.notificaciones.listeners;

import org.springframework.amqp.core.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Detiene / arranca los contenedores de @RabbitListener (stop()/start(), que es
 * lo que ofrece Spring AMQP). Un listener detenido deja los mensajes en su cola.
 */
@Service
public class ListenerControlService {

    private final RabbitListenerEndpointRegistry registro;

    public ListenerControlService(RabbitListenerEndpointRegistry registro) {
        this.registro = registro;
    }

    /** id del listener -> si esta consumiendo. */
    public Map<String, Boolean> estados() {
        Map<String, Boolean> estados = new TreeMap<>();
        registro.getListenerContainerIds()
                .forEach(id -> estados.put(id, registro.getListenerContainer(id).isRunning()));
        return estados;
    }

    public Optional<Boolean> detener(String id) {
        return contenedor(id).map(c -> {
            c.stop();
            return c.isRunning();
        });
    }

    public Optional<Boolean> iniciar(String id) {
        return contenedor(id).map(c -> {
            c.start();
            return c.isRunning();
        });
    }

    private Optional<MessageListenerContainer> contenedor(String id) {
        return Optional.ofNullable(registro.getListenerContainer(id));
    }
}
