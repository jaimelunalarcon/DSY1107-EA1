package cl.duoc.dsy1107.ae1.presupuestos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SolicitudPresupuestoControllerTest {

    private static final String NUEVA = """
            {"titulo":"Viaje a feria","descripcion":"Stand y pasajes","monto":250000,"categoria":"VIATICOS","solicitante":"trabajador@duoc.cl"}""";

    @Autowired
    private MockMvc mvc;

    private long crear() throws Exception {
        String respuesta = mvc.perform(post("/presupuestos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    @DisplayName("POST crea en PENDIENTE con Location")
    void crearSolicitud() throws Exception {
        mvc.perform(post("/presupuestos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVA))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.categoria").value("VIATICOS"))
                .andExpect(jsonPath("$.monto").value(250000));
    }

    @Test
    @DisplayName("GET lista lo creado")
    void listar() throws Exception {
        crear();
        mvc.perform(get("/presupuestos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("PUT edita una pendiente")
    void editar() throws Exception {
        long id = crear();
        mvc.perform(put("/presupuestos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Viaje actualizado","descripcion":"Nuevo detalle","monto":300000,"categoria":"MARKETING"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Viaje actualizado"))
                .andExpect(jsonPath("$.categoria").value("MARKETING"))
                .andExpect(jsonPath("$.monto").value(300000))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("DELETE borra una pendiente")
    void eliminar() throws Exception {
        long id = crear();
        mvc.perform(delete("/presupuestos/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/presupuestos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST decision aprueba una pendiente")
    void aprobar() throws Exception {
        long id = crear();
        mvc.perform(post("/presupuestos/" + id + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"APROBADA","comentario":"Ok presupuesto"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.comentarioAdmin").value("Ok presupuesto"));
    }

    @Test
    @DisplayName("no se puede editar ni decidir dos veces")
    void yaDecidida() throws Exception {
        long id = crear();
        mvc.perform(post("/presupuestos/" + id + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"RECHAZADA","comentario":"Sin cupo"}"""))
                .andExpect(status().isOk());

        mvc.perform(put("/presupuestos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"x","descripcion":"y","monto":1,"categoria":"OTRO"}"""))
                .andExpect(status().isConflict());

        mvc.perform(delete("/presupuestos/" + id)).andExpect(status().isConflict());

        mvc.perform(post("/presupuestos/" + id + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"APROBADA","comentario":"tarde"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("id inexistente es 404")
    void noEncontrada() throws Exception {
        mvc.perform(get("/presupuestos/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Solicitud no encontrada"));
    }

    @Test
    @DisplayName("cuerpo invalido es 400")
    void invalido() throws Exception {
        mvc.perform(post("/presupuestos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":" ","descripcion":"x","monto":0,"solicitante":"no-es-mail"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.titulo").exists())
                .andExpect(jsonPath("$.campos.monto").exists());
    }
}
