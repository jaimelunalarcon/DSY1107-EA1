package cl.duoc.dsy1107.rabbitadmin.servicio;

public class RecursoProtegidoException extends RuntimeException {

    public RecursoProtegidoException(String nombre, String accion) {
        super("'" + nombre + "' es un recurso de negocio protegido: no se puede " + accion + " desde esta API");
    }
}
