package cl.duoc.dsy1107.notificaciones.notificaciones;

import cl.duoc.dsy1107.notificaciones.mensajeria.SolicitudMensaje;
import cl.duoc.dsy1107.notificaciones.simulacion.SimuladorDeFallos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Avisa a los aprobadores que hay una solicitud nueva (simulado con log). */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final SimuladorDeFallos servicioCorreo;

    public NotificacionService(SimuladorDeFallos servicioCorreo) {
        this.servicioCorreo = servicioCorreo;
    }

    public void notificarCreacion(SolicitudMensaje solicitud) {
        servicioCorreo.verificar(solicitud, "Servicio de correo");
        log.info("[NOTIFICACION] Aviso a administradores: solicitud #{} '{}' de {} por ${} ({})",
                solicitud.solicitudId(),
                solicitud.titulo(),
                solicitud.solicitante(),
                solicitud.monto(),
                solicitud.categoria());
    }
}
