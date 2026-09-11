package cl.duoc.dsy1107.ae1.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del backend. Los defaults viven en application.yml; en ECS se
 * pisan con variables de entorno (p. ej. BACKEND_CORS_ORIGENES_0).
 */
@ConfigurationProperties(prefix = "backend")
public record BackendProperties(Cors cors) {

    /**
     * Origenes que pueden llamar a este backend desde un navegador.
     *
     * En el despliegue real el CORS lo resuelve el API Gateway y el navegador
     * no habla directo con este servicio. Esto existe para el modo local
     * (Vite en :5173 apuntando al :8080).
     */
    public record Cors(List<String> origenes) {
    }
}
