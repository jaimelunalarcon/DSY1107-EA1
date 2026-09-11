package cl.duoc.dsy1107.ae1.web;

import cl.duoc.dsy1107.ae1.presupuestos.DecisionInvalidaException;
import cl.duoc.dsy1107.ae1.presupuestos.SolicitudNoEncontradaException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduce las excepciones de dominio a codigos HTTP con cuerpo JSON.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    public record ErrorHttp(String error, String detalle, Instant momento) {
    }

    @ExceptionHandler(SolicitudNoEncontradaException.class)
    public ResponseEntity<ErrorHttp> solicitudNoEncontrada(SolicitudNoEncontradaException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorHttp("Solicitud no encontrada", e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(DecisionInvalidaException.class)
    public ResponseEntity<ErrorHttp> decisionInvalida(DecisionInvalidaException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorHttp("Decision no permitida", e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Cuerpo400> invalido(MethodArgumentNotValidException e) {
        Map<String, String> campos = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        f -> f.getDefaultMessage() == null ? "valor invalido" : f.getDefaultMessage(),
                        (primero, segundo) -> primero));

        return ResponseEntity.badRequest()
                .body(new Cuerpo400("El cuerpo de la peticion no es valido", campos, Instant.now()));
    }

    public record Cuerpo400(String error, Map<String, String> campos, Instant momento) {
    }
}
