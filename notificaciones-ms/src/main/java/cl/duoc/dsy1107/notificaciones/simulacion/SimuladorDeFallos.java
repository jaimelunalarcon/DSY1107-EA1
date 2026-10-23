package cl.duoc.dsy1107.notificaciones.simulacion;

import cl.duoc.dsy1107.notificaciones.mensajeria.ErrorNoRecuperableException;
import cl.duoc.dsy1107.notificaciones.mensajeria.ErrorRecuperableException;
import cl.duoc.dsy1107.notificaciones.mensajeria.SolicitudMensaje;

import org.springframework.stereotype.Component;

/**
 * Hace de "servicio externo" (correo, sistema de tickets) que a veces falla, para
 * poder mostrar reintento y DLQ creando una solicitud desde el frontend.
 */
@Component
public class SimuladorDeFallos {

    private final SimulacionProperties props;

    public SimuladorDeFallos(SimulacionProperties props) {
        this.props = props;
    }

    public void verificar(SolicitudMensaje solicitud, String servicioExterno) {
        String titulo = solicitud.titulo();
        if (titulo.contains(props.fallaPermanente())) {
            throw new ErrorNoRecuperableException(
                    servicioExterno + " rechazo la solicitud " + solicitud.solicitudId() + " (datos invalidos)");
        }
        if (titulo.contains(props.fallaTemporal())) {
            throw new ErrorRecuperableException(
                    servicioExterno + " no responde (timeout) para la solicitud " + solicitud.solicitudId());
        }
    }
}
