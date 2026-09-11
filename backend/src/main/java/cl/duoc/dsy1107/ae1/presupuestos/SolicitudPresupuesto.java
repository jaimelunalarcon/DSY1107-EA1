package cl.duoc.dsy1107.ae1.presupuestos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Una fila de solicitud_presupuesto. No sale por HTTP: el controlador convierte
 * a records de entrada/salida.
 */
@Entity
@Table(name = "solicitud_presupuesto")
public class SolicitudPresupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    /** Pesos chilenos, entero. */
    @Column(nullable = false)
    private int monto;

    @Column(nullable = false, length = 320)
    private String solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaSolicitud categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(name = "comentario_admin", length = 500)
    private String comentarioAdmin;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "decidido_en")
    private Instant decididoEn;

    protected SolicitudPresupuesto() {
    }

    public SolicitudPresupuesto(
            String titulo,
            String descripcion,
            int monto,
            String solicitante,
            CategoriaSolicitud categoria,
            Instant creadoEn) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.monto = monto;
        this.solicitante = solicitante;
        this.categoria = categoria;
        this.estado = EstadoSolicitud.PENDIENTE;
        this.creadoEn = creadoEn;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getMonto() {
        return monto;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public CategoriaSolicitud getCategoria() {
        return categoria;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public String getComentarioAdmin() {
        return comentarioAdmin;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getDecididoEn() {
        return decididoEn;
    }

    public void actualizarPendiente(
            String titulo, String descripcion, int monto, CategoriaSolicitud categoria) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.monto = monto;
        this.categoria = categoria;
    }

    public void decidir(EstadoSolicitud nuevo, String comentario, Instant cuando) {
        this.estado = nuevo;
        this.comentarioAdmin = comentario;
        this.decididoEn = cuando;
    }
}
