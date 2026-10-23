package cl.duoc.dsy1107.notificaciones.mensajeria;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convierte el cuerpo AMQP en {@link SolicitudMensaje} dentro del listener (y no
 * en el contenedor) para que un JSON invalido tambien pase por el NACK explicito.
 */
@Component
public class LectorDeMensajes {

    private final JsonMapper json;
    private final Validator validador;

    public LectorDeMensajes(JsonMapper json, Validator validador) {
        this.json = json;
        this.validador = validador;
    }

    public SolicitudMensaje leer(Message mensaje) {
        SolicitudMensaje solicitud;
        try {
            solicitud = json.readValue(mensaje.getBody(), SolicitudMensaje.class);
        } catch (JacksonException e) {
            throw new ErrorNoRecuperableException("JSON invalido: " + e.getOriginalMessage(), e);
        }
        Set<ConstraintViolation<SolicitudMensaje>> errores = validador.validate(solicitud);
        if (!errores.isEmpty()) {
            String detalle = errores.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new ErrorNoRecuperableException("Mensaje incompleto: " + detalle);
        }
        return solicitud;
    }
}
