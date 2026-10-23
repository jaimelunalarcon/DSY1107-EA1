package cl.duoc.dsy1107.notificaciones.mensajeria;

/**
 * Fallo que no se arregla reintentando (JSON corrupto, datos invalidos, regla
 * de negocio). El listener responde NACK sin requeue: va directo a la DLQ.
 */
public class ErrorNoRecuperableException extends RuntimeException {

    public ErrorNoRecuperableException(String mensaje) {
        super(mensaje);
    }

    public ErrorNoRecuperableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
