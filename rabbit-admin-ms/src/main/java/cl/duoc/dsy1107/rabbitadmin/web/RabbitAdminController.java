package cl.duoc.dsy1107.rabbitadmin.web;

import cl.duoc.dsy1107.rabbitadmin.dto.BindingRequest;
import cl.duoc.dsy1107.rabbitadmin.dto.ColaInfo;
import cl.duoc.dsy1107.rabbitadmin.dto.CrearColaRequest;
import cl.duoc.dsy1107.rabbitadmin.dto.CrearExchangeRequest;
import cl.duoc.dsy1107.rabbitadmin.dto.ExchangeInfo;
import cl.duoc.dsy1107.rabbitadmin.dto.NombreValido;
import cl.duoc.dsy1107.rabbitadmin.servicio.RabbitAdminService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * API de administracion de RabbitMQ.
 *
 * <pre>
 * POST   /api/rabbitmq/queues                    crear cola (quorum por defecto)
 * GET    /api/rabbitmq/queues/{nombre}           mensajes y consumidores
 * DELETE /api/rabbitmq/queues/{nombre}           eliminar cola
 * DELETE /api/rabbitmq/queues/{nombre}/messages  vaciar cola
 * POST   /api/rabbitmq/exchanges                 crear exchange
 * DELETE /api/rabbitmq/exchanges/{nombre}        eliminar exchange
 * POST   /api/rabbitmq/bindings                  enlazar exchange -> cola
 * DELETE /api/rabbitmq/bindings?exchange=&amp;cola=&amp;routingKey=   quitar enlace
 * </pre>
 */
@RestController
@RequestMapping("/api/rabbitmq")
public class RabbitAdminController {

    private final RabbitAdminService servicio;

    public RabbitAdminController(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    // --- Colas ----------------------------------------------------------------

    @PostMapping("/queues")
    public ResponseEntity<ColaInfo> crearCola(@Valid @RequestBody CrearColaRequest datos) {
        ColaInfo cola = servicio.crearCola(datos);
        return ResponseEntity.created(URI.create("/api/rabbitmq/queues/" + cola.nombre())).body(cola);
    }

    @GetMapping("/queues/{nombre}")
    public ColaInfo cola(@PathVariable @NombreValido String nombre) {
        return servicio.infoCola(nombre);
    }

    @DeleteMapping("/queues/{nombre}")
    public ResponseEntity<Void> eliminarCola(
            @PathVariable @NombreValido String nombre,
            @RequestParam(defaultValue = "false") boolean soloSiSinConsumidores,
            @RequestParam(defaultValue = "false") boolean soloSiVacia) {
        servicio.eliminarCola(nombre, soloSiSinConsumidores, soloSiVacia);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/queues/{nombre}/messages")
    public Map<String, Object> vaciarCola(@PathVariable @NombreValido String nombre) {
        return Map.of("cola", nombre, "mensajesEliminados", servicio.vaciarCola(nombre));
    }

    // --- Exchanges ------------------------------------------------------------

    @PostMapping("/exchanges")
    public ResponseEntity<ExchangeInfo> crearExchange(@Valid @RequestBody CrearExchangeRequest datos) {
        servicio.crearExchange(datos);
        return ResponseEntity.created(URI.create("/api/rabbitmq/exchanges/" + datos.nombre()))
                .body(ExchangeInfo.de(datos));
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable @NombreValido String nombre) {
        servicio.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }

    // --- Bindings -------------------------------------------------------------

    @PostMapping("/bindings")
    public ResponseEntity<BindingRequest> crearBinding(@Valid @RequestBody BindingRequest datos) {
        servicio.crearBinding(datos);
        return ResponseEntity.status(201).body(datos);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @ModelAttribute BindingRequest datos) {
        servicio.eliminarBinding(datos);
        return ResponseEntity.noContent().build();
    }
}
