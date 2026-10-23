package cl.duoc.dsy1107.notificaciones.simulacion;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Marcas que, puestas en el titulo de una solicitud, fuerzan un fallo en la demo. */
@Validated
@ConfigurationProperties(prefix = "notificaciones.simulacion")
public record SimulacionProperties(
        @NotBlank String fallaTemporal,
        @NotBlank String fallaPermanente) {
}
