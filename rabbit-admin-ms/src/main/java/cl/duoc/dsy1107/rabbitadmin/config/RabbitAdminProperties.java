package cl.duoc.dsy1107.rabbitadmin.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "rabbit-admin")
public record RabbitAdminProperties(
        @NotNull List<String> prefijosProtegidos,
        @Valid @NotNull ColaPorDefecto colaPorDefecto) {

    public record ColaPorDefecto(@Min(1) int deliveryLimit) {
    }

    public boolean esProtegido(String nombre) {
        return prefijosProtegidos.stream().anyMatch(nombre::startsWith);
    }
}
