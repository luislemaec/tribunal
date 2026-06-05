package ec.com.antenasur.model.tec;

import java.io.Serializable;
import java.util.Date;

import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "escrutinio_cabecera", schema = "tec",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_escrutinio_cabecera_proceso_mesa",
                    columnNames = {"proce_id", "mesa_id"})
        },
        indexes = {
            @Index(name = "idx_escrutinio_cabecera_proce_id", columnList = "proce_id"),
            @Index(name = "idx_escrutinio_cabecera_mesa_id", columnList = "mesa_id"),
            @Index(name = "idx_escrutinio_cabecera_estado", columnList = "esca_estado")
        })
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Audited
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
public class EscrutinioCabecera extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "esca_id")
    private Integer id;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_escrutinio_cabecera_mesa"))
    private Mesa mesa;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proce_id", nullable = false, foreignKey = @ForeignKey(name = "fk_escrutinio_cabecera_proceso"))
    private ProcesoElectoral proceso;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "esca_estado", nullable = false, length = 40)
    private EstadoEscrutinio estadoEscrutinio = EstadoEscrutinio.PENDIENTE;

    @Setter
    @Getter
    @Column(name = "esca_presidente", length = 100)
    private String presidenteResponsable;

    @Setter
    @Getter
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "esca_fecha_apertura")
    private Date fechaApertura;

    @Setter
    @Getter
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "esca_fecha_inicio_conteo")
    private Date fechaInicioConteo;

    @Setter
    @Getter
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "esca_fecha_cierre")
    private Date fechaCierre;

    @Setter
    @Getter
    @Column(name = "esca_total_sufragantes", nullable = false)
    private Integer totalSufragantes = 0;

    @Setter
    @Getter
    @Column(name = "esca_total_votos_registrados", nullable = false)
    private Integer totalVotosRegistrados = 0;

    @Setter
    @Getter
    @Column(name = "esca_total_votos_validos", nullable = false)
    private Integer totalVotosValidos = 0;

    @Setter
    @Getter
    @Column(name = "esca_total_votos_blancos", nullable = false)
    private Integer totalVotosBlancos = 0;

    @Setter
    @Getter
    @Column(name = "esca_total_votos_nulos", nullable = false)
    private Integer totalVotosNulos = 0;

    @Setter
    @Getter
    @Column(name = "esca_obs_apertura", length = 1000)
    private String observacionApertura;

    @Setter
    @Getter
    @Column(name = "esca_obs_conteo", length = 1000)
    private String observacionConteo;

    @Setter
    @Getter
    @Column(name = "esca_obs_cierre", length = 1000)
    private String observacionCierre;
}
