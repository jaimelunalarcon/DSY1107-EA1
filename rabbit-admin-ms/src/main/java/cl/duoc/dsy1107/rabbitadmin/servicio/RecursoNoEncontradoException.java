package cl.duoc.dsy1107.rabbitadmin.servicio;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String tipo, String nombre) {
        super("No existe " + tipo + " '" + nombre + "'");
    }
}
