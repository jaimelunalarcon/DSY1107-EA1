package cl.duoc.dsy1107.ae1.presupuestos;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class SolicitudPresupuestoService {

    private static final Sort ORDEN = Sort.by(Sort.Direction.DESC, "creadoEn");

    private final SolicitudPresupuestoRepository repositorio;
    private final Clock reloj;

    public SolicitudPresupuestoService(SolicitudPresupuestoRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<SolicitudPresupuesto> listar() {
        return repositorio.findAll(ORDEN);
    }

    @Transactional(readOnly = true)
    public SolicitudPresupuesto obtener(long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new SolicitudNoEncontradaException(id));
    }

    @Transactional
    public SolicitudPresupuesto crear(String titulo, String descripcion, int monto, String solicitante) {
        return repositorio.save(
                new SolicitudPresupuesto(titulo, descripcion, monto, solicitante, reloj.instant()));
    }

    /**
     * Solo PENDIENTE puede pasar a APROBADA o RECHAZADA. La autorizacion de
     * "quien puede decidir" la hace el API Gateway (scope presupuestos/decidir).
     */
    @Transactional
    public SolicitudPresupuesto decidir(long id, EstadoSolicitud nuevoEstado, String comentario) {
        if (nuevoEstado != EstadoSolicitud.APROBADA && nuevoEstado != EstadoSolicitud.RECHAZADA) {
            throw new DecisionInvalidaException("La decision debe ser APROBADA o RECHAZADA");
        }
        SolicitudPresupuesto s = obtener(id);
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new DecisionInvalidaException(
                    "La solicitud " + id + " ya esta " + s.getEstado() + " y no se puede volver a decidir");
        }
        s.decidir(nuevoEstado, comentario, reloj.instant());
        return s;
    }
}
