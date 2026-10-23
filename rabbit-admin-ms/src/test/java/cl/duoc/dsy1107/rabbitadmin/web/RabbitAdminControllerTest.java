package cl.duoc.dsy1107.rabbitadmin.web;

import cl.duoc.dsy1107.rabbitadmin.dto.BindingRequest;
import cl.duoc.dsy1107.rabbitadmin.dto.ColaInfo;
import cl.duoc.dsy1107.rabbitadmin.dto.CrearColaRequest;
import cl.duoc.dsy1107.rabbitadmin.servicio.RabbitAdminService;
import cl.duoc.dsy1107.rabbitadmin.servicio.RecursoNoEncontradoException;
import cl.duoc.dsy1107.rabbitadmin.servicio.RecursoProtegidoException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RabbitAdminController.class)
class RabbitAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RabbitAdminService servicio;

    private ResultActions postJson(String ruta, String json) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    @DisplayName("POST /queues valida y responde 201 con Location")
    void crearCola() throws Exception {
        when(servicio.crearCola(any(CrearColaRequest.class))).thenReturn(new ColaInfo("demo.auditoria", 0, 0));

        postJson("/api/rabbitmq/queues", """
                {"nombre":"demo.auditoria"}""")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/rabbitmq/queues/demo.auditoria"))
                .andExpect(jsonPath("$.nombre").value("demo.auditoria"));
    }

    @Test
    @DisplayName("nombre de cola vacio -> 400 sin tocar el broker")
    void colaSinNombre() throws Exception {
        postJson("/api/rabbitmq/queues", """
                {"nombre":"  "}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("prefijo reservado amq. o caracteres invalidos -> 400")
    void nombreInvalido() throws Exception {
        postJson("/api/rabbitmq/queues", """
                {"nombre":"amq.mia"}""")
                .andExpect(status().isBadRequest());
        postJson("/api/rabbitmq/queues", """
                {"nombre":"cola con espacios"}""")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("configuraciones incoherentes -> 400 con el motivo")
    void configuracionInvalida() throws Exception {
        postJson("/api/rabbitmq/queues", """
                {"nombre":"demo.q","tipo":"QUORUM","durable":false}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.quorumDurable").exists());
        postJson("/api/rabbitmq/queues", """
                {"nombre":"demo.q","tipo":"CLASSIC","deliveryLimit":3}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.deliveryLimitSoloQuorum").exists());
        postJson("/api/rabbitmq/queues", """
                {"nombre":"demo.q","ttlMs":10}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.ttlMs").exists());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("exchange sin tipo o con tipo inexistente -> 400")
    void exchangeInvalido() throws Exception {
        postJson("/api/rabbitmq/exchanges", """
                {"nombre":"demo.ex"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tipo").exists());
        postJson("/api/rabbitmq/exchanges", """
                {"nombre":"demo.ex","tipo":"RARO"}""")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("POST /exchanges valido -> 201")
    void crearExchange() throws Exception {
        postJson("/api/rabbitmq/exchanges", """
                {"nombre":"demo.ex","tipo":"TOPIC"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("TOPIC"))
                .andExpect(jsonPath("$.durable").value(true));
    }

    @Test
    @DisplayName("binding sin routing key -> 400")
    void bindingSinRoutingKey() throws Exception {
        postJson("/api/rabbitmq/bindings", """
                {"exchange":"demo.ex","cola":"demo.q"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.routingKey").exists());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("DELETE /bindings por query params")
    void eliminarBinding() throws Exception {
        mvc.perform(delete("/api/rabbitmq/bindings")
                        .param("exchange", "presupuestos.exchange")
                        .param("cola", "demo.auditoria")
                        .param("routingKey", "solicitud.creada"))
                .andExpect(status().isNoContent());
        verify(servicio).eliminarBinding(
                new BindingRequest("presupuestos.exchange", "demo.auditoria", "solicitud.creada"));
    }

    @Test
    @DisplayName("nombre invalido en la URL -> 400")
    void rutaInvalida() throws Exception {
        mvc.perform(delete("/api/rabbitmq/queues/amq.gen-x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("cola inexistente -> 404")
    void noExiste() throws Exception {
        when(servicio.infoCola("demo.nada")).thenThrow(new RecursoNoEncontradoException("la cola", "demo.nada"));

        mvc.perform(get("/api/rabbitmq/queues/demo.nada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("borrar un recurso de negocio -> 409")
    void protegido() throws Exception {
        doThrow(new RecursoProtegidoException("presupuestos.dlq", "eliminar"))
                .when(servicio).eliminarCola("presupuestos.dlq", false, false);

        mvc.perform(delete("/api/rabbitmq/queues/presupuestos.dlq"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Recurso protegido"));
    }
}
