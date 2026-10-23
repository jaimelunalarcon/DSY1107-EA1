package cl.duoc.dsy1107.rabbitadmin.dto;

/** Reglas de nombre compartidas por DTOs y path variables. */
public final class NombresRabbit {

    /** Letras, numeros, '.', '_' y '-'; sin el prefijo reservado amq. del broker. */
    public static final String PATRON = "^(?!amq\\.)[A-Za-z0-9][A-Za-z0-9._-]*$";
    public static final String MENSAJE =
            "solo letras, numeros, '.', '_' y '-', debe empezar con letra o numero y no puede empezar con 'amq.'";
    public static final int LARGO_MAXIMO = 200;

    /** Routing keys: ademas admite los comodines '*' y '#' de los exchanges topic. Puede ser vacia. */
    public static final String PATRON_ROUTING_KEY = "^[A-Za-z0-9._*#-]*$";
    public static final String MENSAJE_ROUTING_KEY = "solo letras, numeros, '.', '_', '-', '*' y '#'";
    public static final int LARGO_MAXIMO_ROUTING_KEY = 255;

    private NombresRabbit() {
    }
}
