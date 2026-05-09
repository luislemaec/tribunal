package ec.com.antenasur.model.tec;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Cabecera del proceso electoral. Solo uno debe estar {@code activo=true}
 * a la vez. Las fases del cronograma se modelan en {@link CronogramaFase}
 * con FK a esta entidad. Tabla nueva para no chocar con
 * {@code tec.procesos} (bitÃ¡cora de actividad).
 */
@Entity
@Table(name = "proceso_electoral", schema = "tec")
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
public class ProcesoElectoral extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proce_id")
    @Setter @Getter
    private Integer id;

    @Setter @Getter
    @Column(name = "proce_nombre", length = 150, nullable = false)
    private String nombre;

    @Setter @Getter
    @Column(name = "proce_descripcion", length = 500)
    private String descripcion;

    @Setter @Getter
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "proce_fecha_inicio")
    private Date fechaInicio;

    @Setter @Getter
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "proce_fecha_fin")
    private Date fechaFin;

    /** Solo UN proceso activo a la vez (responsabilidad del service). */
    @Setter @Getter
    @Column(name = "proce_activo")
    private Boolean activo;

    public ProcesoElectoral() {
    }
}
