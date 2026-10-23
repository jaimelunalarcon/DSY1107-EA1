package cl.duoc.dsy1107.rabbitadmin.dto;

/** Estado de una cola segun el broker. */
public record ColaInfo(String nombre, long mensajes, int consumidores) {
}
