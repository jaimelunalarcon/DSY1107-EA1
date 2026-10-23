package cl.duoc.dsy1107.notificaciones.mensajeria;

/**
 * Fallo transitorio (servicio externo caido, timeout): vale la pena reintentar.
 * El listener responde NACK con requeue hasta agotar los intentos.
 */
public class ErrorRecuperableException extends RuntimeException {

    public ErrorRecuperableException(String mensaje) {
        super(mensaje);
    }
}
