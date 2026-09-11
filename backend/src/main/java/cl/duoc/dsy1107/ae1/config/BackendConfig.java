package cl.duoc.dsy1107.ae1.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackendConfig {

    /** Reloj inyectable para timestamps de solicitudes (facilita pruebas). */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
