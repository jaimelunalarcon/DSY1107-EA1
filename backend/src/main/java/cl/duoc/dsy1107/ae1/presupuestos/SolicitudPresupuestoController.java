package cl.duoc.dsy1107.ae1.presupuestos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * API de solicitudes de presupuesto.
 *
 * No mira el JWT: el authorizer del API Gateway exige presupuestos/read,
 * presupuestos/write o presupuestos/decidir segun la ruta.
 */
@RestController
@RequestMapping("/presupuestos")
public class SolicitudPresupuestoController {

    private final SolicitudPresupuestoService servicio;

    public SolicitudPresupuestoController(SolicitudPresupuestoService servicio) {
        this.servicio = servicio;
    }

    public record SolicitudNueva(
            @NotBlank(message = "el titulo es obligatorio")
            @Size(max = 160, message = "el titulo no puede pasar de 160 caracteres")
            String titulo,

            @NotBlank(message = "la descripcion es obligatoria")
            @Size(max = 1000, message = "la descripcion no puede pasar de 1000 caracteres")
            String descripcion,

            @Positive(message = "el monto debe ser mayor que cero")
            int monto,

            @NotNull(message = "la categoria es obligatoria")
            CategoriaSolicitud categoria,

            @NotBlank(message = "el solicitante es obligatorio")
            @Email(message = "el solicitante debe ser un correo valido")
            @Size(max = 320, message = "el solicitante no puede pasar de 320 caracteres")
            String solicitante) {
    }

    /** Edicion: titulo, descripcion, monto y categoria (el solicitante no cambia). */
    public record SolicitudEdicion(
            @NotBlank(message = "el titulo es obligatorio")
            @Size(max = 160, message = "el titulo no puede pasar de 160 caracteres")
            String titulo,

            @NotBlank(message = "la descripcion es obligatoria")
            @Size(max = 1000, message = "la descripcion no puede pasar de 1000 caracteres")
            String descripcion,

            @Positive(message = "el monto debe ser mayor que cero")
            int monto,

            @NotNull(message = "la categoria es obligatoria")
            CategoriaSolicitud categoria) {
    }

    public record DecisionNueva(
            @NotNull(message = "el estado es obligatorio")
            EstadoSolicitud estado,

            @Size(max = 500, message = "el comentario no puede pasar de 500 caracteres")
            String comentario) {
    }

    public record SolicitudVista(
            Long id,
            String titulo,
            String descripcion,
            int monto,
            CategoriaSolicitud categoria,
            String solicitante,
            EstadoSolicitud estado,
            String comentarioAdmin,
            Instant creadoEn,
            Instant decididoEn) {

        static SolicitudVista de(SolicitudPresupuesto s) {
            return new SolicitudVista(
                    s.getId(),
                    s.getTitulo(),
                    s.getDescripcion(),
                    s.getMonto(),
                    s.getCategoria(),
                    s.getSolicitante(),
                    s.getEstado(),
                    s.getComentarioAdmin(),
                    s.getCreadoEn(),
                    s.getDecididoEn());
        }
    }

    @GetMapping
    public List<SolicitudVista> listar() {
        return servicio.listar().stream().map(SolicitudVista::de).toList();
    }

    @GetMapping("/{id}")
    public SolicitudVista obtener(@PathVariable long id) {
        return SolicitudVista.de(servicio.obtener(id));
    }

    @PostMapping
    public ResponseEntity<SolicitudVista> crear(@Valid @RequestBody SolicitudNueva datos) {
        SolicitudPresupuesto creada = servicio.crear(
                datos.titulo(),
                datos.descripcion(),
                datos.monto(),
                datos.solicitante(),
                datos.categoria());
        SolicitudVista vista = SolicitudVista.de(creada);
        return ResponseEntity.created(URI.create("/presupuestos/" + vista.id())).body(vista);
    }

    @PutMapping("/{id}")
    public SolicitudVista actualizar(@PathVariable long id, @Valid @RequestBody SolicitudEdicion datos) {
        return SolicitudVista.de(
                servicio.actualizar(
                        id, datos.titulo(), datos.descripcion(), datos.monto(), datos.categoria()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable long id) {
        servicio.eliminar(id);
    }

    /** POST (no PUT) para no chocar con la edicion PUT /{id} en el API Gateway. */
    @PostMapping("/{id}/decision")
    public SolicitudVista decidir(@PathVariable long id, @Valid @RequestBody DecisionNueva datos) {
        return SolicitudVista.de(servicio.decidir(id, datos.estado(), datos.comentario()));
    }
}
