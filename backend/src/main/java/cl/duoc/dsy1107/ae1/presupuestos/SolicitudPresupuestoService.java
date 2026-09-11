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
    public SolicitudPresupuesto crear(
            String titulo,
            String descripcion,
            int monto,
            String solicitante,
            CategoriaSolicitud categoria) {
        return repositorio.save(
                new SolicitudPresupuesto(
                        titulo, descripcion, monto, solicitante, categoria, reloj.instant()));
    }

    /** Solo mientras esta PENDIENTE (CRUD del trabajador). */
    @Transactional
    public SolicitudPresupuesto actualizar(
            long id,
            String titulo,
            String descripcion,
            int monto,
            CategoriaSolicitud categoria) {
        SolicitudPresupuesto s = obtener(id);
        exigirPendiente(s, "editar");
        s.actualizarPendiente(titulo, descripcion, monto, categoria);
        return s;
    }

    /** Solo mientras esta PENDIENTE. */
    @Transactional
    public void eliminar(long id) {
        SolicitudPresupuesto s = obtener(id);
        exigirPendiente(s, "eliminar");
        repositorio.delete(s);
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
        exigirPendiente(s, "decidir");
        s.decidir(nuevoEstado, comentario, reloj.instant());
        return s;
    }

    private static void exigirPendiente(SolicitudPresupuesto s, String accion) {
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new DecisionInvalidaException(
                    "No se puede " + accion + " la solicitud " + s.getId()
                            + ": esta " + s.getEstado());
        }
    }
}
