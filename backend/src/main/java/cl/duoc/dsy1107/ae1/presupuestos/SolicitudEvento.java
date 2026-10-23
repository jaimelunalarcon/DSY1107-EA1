package cl.duoc.dsy1107.ae1.presupuestos;

import java.time.Instant;

/**
 * Evento de dominio que emite {@link SolicitudPresupuestoService}. El dominio no
 * sabe de RabbitMQ: quien lo traduce a un mensaje es el paquete mensajeria.
 */
public record SolicitudEvento(
        Tipo tipo,
        long id,
        String titulo,
        int monto,
        String solicitante,
        CategoriaSolicitud categoria,
        EstadoSolicitud estado,
        String comentarioAdmin,
        Instant ocurridoEn) {

    public enum Tipo {
        CREADA,
        DECIDIDA
    }

    static SolicitudEvento de(Tipo tipo, SolicitudPresupuesto s, Instant ocurridoEn) {
        return new SolicitudEvento(
                tipo,
                s.getId(),
                s.getTitulo(),
                s.getMonto(),
                s.getSolicitante(),
                s.getCategoria(),
                s.getEstado(),
                s.getComentarioAdmin(),
                ocurridoEn);
    }
}
