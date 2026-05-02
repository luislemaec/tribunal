package ec.com.antenasur.model.tec;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.enums.SeveridadCronograma;
import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Fase del cronograma electoral asociada a un {@link ProcesoElectoral}.
 * Define la ventana temporal en que la fase está vigente y si permite
 * edición del padrón. El banner del sistema toma {@code titulo}, {@code mensaje}
 * y {@code severidad} para componer la notificación visible.
 */
@Entity
@Table(name = "cronograma_fase", schema = "tec")
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
public class CronogramaFase extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cref_id")
    @Setter @Getter
    private Integer id;

    @Setter @Getter
    @ManyToOne
    @JoinColumn(name = "proce_id", nullable = false)
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
