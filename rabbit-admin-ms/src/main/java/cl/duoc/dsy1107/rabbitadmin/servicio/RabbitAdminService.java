package cl.duoc.dsy1107.rabbitadmin.servicio;

import cl.duoc.dsy1107.rabbitadmin.config.RabbitAdminProperties;
import cl.duoc.dsy1107.rabbitadmin.dto.BindingRequest;
import cl.duoc.dsy1107.rabbitadmin.dto.ColaInfo;
import cl.duoc.dsy1107.rabbitadmin.dto.CrearColaRequest;
import cl.duoc.dsy1107.rabbitadmin.dto.CrearExchangeRequest;
import cl.duoc.dsy1107.rabbitadmin.dto.TipoCola;
import com.rabbitmq.client.ShutdownSignalException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.connection.RabbitUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Unico punto que habla con el broker para administrar recursos. El controlador
 * llama metodos de alto nivel (crearCola, eliminarExchange...) y no conoce
 * QueueBuilder, argumentos x-* ni canales AMQP.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private static final String ARG_TIPO_COLA = "x-queue-type";
    private static final String TIPO_CLASSIC = "classic";

    private final AmqpAdmin admin;
    private final RabbitTemplate rabbit;
    private final RabbitAdminProperties props;

    public RabbitAdminService(AmqpAdmin admin, RabbitTemplate rabbit, RabbitAdminProperties props) {
        this.admin = admin;
        this.rabbit = rabbit;
        this.props = props;
    }

    // --- Colas ----------------------------------------------------------------

    public ColaInfo crearCola(CrearColaRequest datos) {
        exigirNoProtegido(datos.nombre(), "redeclarar");
        admin.declareQueue(construirCola(datos));
        log.info("Cola creada: {} ({})", datos.nombre(), datos.tipoEfectivo());
        return infoCola(datos.nombre());
    }

    public ColaInfo infoCola(String nombre) {
        QueueInformation info = admin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("la cola", nombre);
        }
        return new ColaInfo(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public int vaciarCola(String nombre) {
        exigirNoProtegido(nombre, "vaciar");
        infoCola(nombre);
        int purgados = admin.purgeQueue(nombre);
        log.info("Cola vaciada: {} ({} mensajes)", nombre, purgados);
        return purgados;
    }

    /** Con las banderas en true el broker rechaza (409) si la cola tiene consumidores o mensajes. */
    public void eliminarCola(String nombre, boolean soloSiSinConsumidores, boolean soloSiVacia) {
        exigirNoProtegido(nombre, "eliminar");
        infoCola(nombre);
        admin.deleteQueue(nombre, soloSiSinConsumidores, soloSiVacia);
        log.info("Cola eliminada: {}", nombre);
    }

    Queue construirCola(CrearColaRequest datos) {
        QueueBuilder cola = datos.durableEfectivo()
                ? QueueBuilder.durable(datos.nombre())
                : QueueBuilder.nonDurable(datos.nombre());
        if (datos.tipoEfectivo() == TipoCola.QUORUM) {
            int limite = datos.deliveryLimit() != null
                    ? datos.deliveryLimit()
                    : props.colaPorDefecto().deliveryLimit();
            cola.quorum().deliveryLimit(limite);
        } else {
            // Explicito: el cluster tiene default_queue_type = quorum.
            cola.withArgument(ARG_TIPO_COLA, TIPO_CLASSIC);
        }
        if (datos.ttlMs() != null) {
            cola.ttl(datos.ttlMs());
        }
        if (datos.maxMensajes() != null) {
            cola.maxLength(datos.maxMensajes());
        }
        if (datos.deadLetterExchange() != null) {
            cola.deadLetterExchange(datos.deadLetterExchange());
        }
        if (datos.deadLetterRoutingKey() != null) {
            cola.deadLetterRoutingKey(datos.deadLetterRoutingKey());
        }
        return cola.build();
    }

    // --- Exchanges ------------------------------------------------------------

    public void crearExchange(CrearExchangeRequest datos) {
        exigirNoProtegido(datos.nombre(), "redeclarar");
        ExchangeBuilder builder = new ExchangeBuilder(datos.nombre(), datos.tipo().tipoAmqp())
                .durable(datos.durableEfectivo());
        if (datos.autoDeleteEfectivo()) {
            builder.autoDelete();
        }
        Exchange exchange = builder.build();
        admin.declareExchange(exchange);
        log.info("Exchange creado: {} ({})", datos.nombre(), datos.tipo());
    }

    public void eliminarExchange(String nombre) {
        exigirNoProtegido(nombre, "eliminar");
        exigirExchange(nombre);
        admin.deleteExchange(nombre);
        log.info("Exchange eliminado: {}", nombre);
    }

    // --- Bindings -------------------------------------------------------------

    /** Se permite enlazar colas nuevas a exchanges protegidos (p. ej. una cola de auditoria). */
    public void crearBinding(BindingRequest datos) {
        exigirExchange(datos.exchange());
        infoCola(datos.cola());
        admin.declareBinding(binding(datos));
        log.info("Binding creado: {} --[{}]--> {}", datos.exchange(), datos.routingKey(), datos.cola());
    }

    public void eliminarBinding(BindingRequest datos) {
        exigirNoProtegido(datos.cola(), "desenlazar");
        exigirExchange(datos.exchange());
        infoCola(datos.cola());
        admin.removeBinding(binding(datos));
        log.info("Binding eliminado: {} --[{}]--> {}", datos.exchange(), datos.routingKey(), datos.cola());
    }

    private static Binding binding(BindingRequest datos) {
        return new Binding(datos.cola(), Binding.DestinationType.QUEUE, datos.exchange(), datos.routingKey(), null);
    }

    // --- Reglas ---------------------------------------------------------------

    private void exigirNoProtegido(String nombre, String accion) {
        if (props.esProtegido(nombre)) {
            throw new RecursoProtegidoException(nombre, accion);
        }
    }

    private void exigirExchange(String nombre) {
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("el exchange", nombre);
        }
    }

    /** AmqpAdmin no consulta exchanges: se usa una declaracion pasiva (404 si no existe). */
    boolean existeExchange(String nombre) {
        try {
            rabbit.execute(canal -> canal.exchangeDeclarePassive(nombre));
            return true;
        } catch (AmqpException e) {
            if (esNoEncontrado(e)) {
                return false;
            }
            throw e;
        }
    }

    private static boolean esNoEncontrado(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ShutdownSignalException senal) {
                return RabbitUtils.isPassiveDeclarationChannelClose(senal);
            }
        }
        return false;
    }
}
