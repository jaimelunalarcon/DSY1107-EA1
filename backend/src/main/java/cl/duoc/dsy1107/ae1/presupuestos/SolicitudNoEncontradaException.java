package cl.duoc.dsy1107.ae1.presupuestos;

public class SolicitudNoEncontradaException extends RuntimeException {

    public SolicitudNoEncontradaException(long id) {
        super("No existe una solicitud de presupuesto con id " + id);
    }
}
