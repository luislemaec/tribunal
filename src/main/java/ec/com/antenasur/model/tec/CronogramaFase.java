package ec.com.antenasur.model.tec;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.enums.SeveridadCronograma;
import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * Fase del cronograma electoral asociada a un {@link ProcesoElectoral}.
 * Define la ventana temporal en que la fase está vigente y si permite
 * edición del padrón. El banner del sistema toma {@code titulo}, {@code mensaje}
 * y {@code severidad} para componer la notificación visible.
 */
@Entity
@Table(name = "cronograma_fase", schema = "tec",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_cronograma_fase_proceso_fase", columnNames = {"proce_id", "cref_fase"})
        },
        indexes = {
            @Index(name = "idx_cronograma_fase_proce_id", columnList = "proce_id"),
            @Index(name = "idx_cronograma_fase_fechas", columnList = "cref_fecha_inicio, cref_fecha_fin")
        })
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class CronogramaFase extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cref_id")
    @Setter @Getter
    private Integer id;

    @Setter @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proce_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cronograma_fase_proceso_electoral"))
    private ProcesoElectoral proceso;

    @Setter @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "cref_fase", length = 40, nullable = false)
    private FaseElectoral fase;

    @Setter @Getter
    @Column(name = "cref_titulo", length = 150)
    private String titulo;

    @Setter @Getter
    @Column(name = "cref_mensaje", length = 1000)
    private String mensaje;

    @Setter @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "cref_severidad", length = 20)
    private SeveridadCronograma severidad;

    @Setter @Getter
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cref_fecha_inicio", nullable = false)
    private Date fechaInicio;

    @Setter @Getter
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cref_fecha_fin", nullable = false)
    private Date fechaFin;

    @Setter @Getter
    @Column(name = "cref_permite_edicion")
    private Boolean permiteEdicion;

    @Setter @Getter
    @Column(name = "cref_orden")
    private Integer orden;

    public CronogramaFase() {
    }
}
