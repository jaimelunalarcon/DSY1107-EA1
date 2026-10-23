package cl.duoc.dsy1107.notificaciones.listeners;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/** Control de consumidores para la demo (no administra colas: eso es rabbit-admin-ms). */
@RestController
@RequestMapping("/api/listeners")
public class ListenerController {

    private final ListenerControlService servicio;

    public ListenerController(ListenerControlService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Map<String, Boolean> listar() {
        return servicio.estados();
    }

    @PostMapping("/{id}/detener")
    public ResponseEntity<Map<String, Object>> detener(@PathVariable String id) {
        return respuesta(id, servicio.detener(id));
    }

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<Map<String, Object>> iniciar(@PathVariable String id) {
        return respuesta(id, servicio.iniciar(id));
    }

    private static ResponseEntity<Map<String, Object>> respuesta(String id, Optional<Boolean> corriendo) {
        return corriendo
                .map(c -> ResponseEntity.ok(Map.<String, Object>of("listener", id, "corriendo", c)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("error", "Listener no encontrado", "listener", id)));
    }
}
