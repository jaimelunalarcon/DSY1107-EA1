package cl.duoc.dsy1107.rabbitadmin.web;

import cl.duoc.dsy1107.rabbitadmin.servicio.RecursoNoEncontradoException;
import cl.duoc.dsy1107.rabbitadmin.servicio.RecursoProtegidoException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce errores de validacion y del broker a HTTP con cuerpo JSON. */
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    public record ErrorHttp(String error, String detalle, Instant momento) {
    }

    public record Cuerpo400(String error, Map<String, String> campos, Instant momento) {
    }

    /** Cuerpo JSON (@RequestBody) o parametros (@ModelAttribute) invalidos. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Cuerpo400> invalido(BindException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (ObjectError error : e.getBindingResult().getAllErrors()) {
            String campo = error instanceof FieldError f ? f.getField() : error.getObjectName();
            campos.putIfAbsent(campo, error.getDefaultMessage() == null ? "valor invalido" : error.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new Cuerpo400("Parametros invalidos", campos, Instant.now()));
    }

    /** {nombre} de la URL invalido. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Cuerpo400> rutaInvalida(HandlerMethodValidationException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getParameterValidationResults().forEach(r -> r.getResolvableErrors().forEach(err ->
                campos.putIfAbsent(r.getMethodParameter().getParameterName(), err.getDefaultMessage())));
        return ResponseEntity.badRequest()
                .body(new Cuerpo400("Parametros invalidos", campos, Instant.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorHttp> jsonIlegible(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErrorHttp(
                "JSON invalido",
                "Revisa la sintaxis y los valores permitidos (p. ej. tipo: DIRECT, TOPIC, FANOUT, HEADERS)",
                Instant.now()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorHttp> noEncontrado(RecursoNoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorHttp("Recurso no encontrado", e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(RecursoProtegidoException.class)
    public ResponseEntity<ErrorHttp> protegido(RecursoProtegidoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorHttp("Recurso protegido", e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<ErrorHttp> sinBroker(AmqpConnectException e) {
        log.error("Ningun nodo de RabbitMQ responde: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorHttp("RabbitMQ no disponible", "Ningun nodo del cluster responde", Instant.now()));
    }

    /** p. ej. PRECONDITION_FAILED al redeclarar una cola con otros argumentos. */
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ErrorHttp> rechazadoPorBroker(AmqpException e) {
        log.warn("RabbitMQ rechazo la operacion: {}", causaRaiz(e));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorHttp("RabbitMQ rechazo la operacion", causaRaiz(e), Instant.now()));
    }

    private static String causaRaiz(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}
