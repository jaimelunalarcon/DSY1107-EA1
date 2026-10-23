package cl.duoc.dsy1107.notificaciones.mensajeria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/** Contrato JSON que publica el backend de presupuestos (mismos campos). */
public record SolicitudMensaje(
        @NotBlank String evento,
        @Positive long solicitudId,
        @NotBlank String titulo,
        @Positive int monto,
        @NotBlank String solicitante,
        String categoria,
        @NotBlank String estado,
        String comentarioAdmin,
        @NotNull Instant ocurridoEn) {
}
