package cl.duoc.dsy1107.notificaciones.tickets;

import cl.duoc.dsy1107.notificaciones.mensajeria.SolicitudMensaje;
import cl.duoc.dsy1107.notificaciones.simulacion.SimuladorDeFallos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Genera el ticket de resolucion y avisa al solicitante (simulado con log). */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final SimuladorDeFallos sistemaTickets;

    public TicketService(SimuladorDeFallos sistemaTickets) {
        this.sistemaTickets = sistemaTickets;
    }

    public String generarTicket(SolicitudMensaje solicitud) {
        sistemaTickets.verificar(solicitud, "Sistema de tickets");
        String ticket = "TCK-" + solicitud.solicitudId() + "-" + solicitud.estado();
        log.info("[TICKET] {} para {}: solicitud #{} '{}' {}{}",
                ticket,
                solicitud.solicitante(),
                solicitud.solicitudId(),
                solicitud.titulo(),
                solicitud.estado(),
                solicitud.comentarioAdmin() == null ? "" : " - \"" + solicitud.comentarioAdmin() + "\"");
        return ticket;
    }
}
