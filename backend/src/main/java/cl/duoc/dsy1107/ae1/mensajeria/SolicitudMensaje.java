package cl.duoc.dsy1107.ae1.mensajeria;

import cl.duoc.dsy1107.ae1.presupuestos.SolicitudEvento;

import java.time.Instant;

/**
 * Contrato JSON que viaja por RabbitMQ. Desacoplado de la entidad JPA para que
 * los consumidores no dependan del modelo interno de este servicio.
 */
public record SolicitudMensaje(
        String evento,
        long solicitudId,
        String titulo,
        int monto,
        String solicitante,
        String categoria,
        String estado,
        String comentarioAdmin,
        Instant ocurridoEn) {

    static SolicitudMensaje de(SolicitudEvento e, String routingKey) {
        return new SolicitudMensaje(
                routingKey,
                e.id(),
                e.titulo(),
                e.monto(),
                e.solicitante(),
                e.categoria() == null ? null : e.categoria().name(),
                e.estado().name(),
                e.comentarioAdmin(),
                e.ocurridoEn());
    }
}
