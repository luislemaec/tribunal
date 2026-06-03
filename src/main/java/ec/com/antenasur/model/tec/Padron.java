package ec.com.antenasur.model.tec;

import ec.com.antenasur.model.IglesiaPersona;
import java.io.Serializable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the tec_recintos database table.
 *
 */
@Entity
@Table(name = "padron", schema = "tec",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_padron_proceso_iglesia_persona", columnNames = {"proce_id", "igpe_id"})
        },
        indexes = {
            @Index(name = "idx_padron_proce_id", columnList = "proce_id"),
            @Index(name = "idx_padron_mesa_id", columnList = "mesa_id"),
            @Index(name = "idx_padron_igpe_id", columnList = "igpe_id"),
            @Index(name = "idx_padron_proceso_mesa", columnList = "proce_id, mesa_id")
        })

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Audited
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
public class Padron extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "padron_id")
    private Integer id;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "igpe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_padron_iglesia_persona"))
    private IglesiaPersona iglesiaPersona;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_padron_mesa"))
    private Mesa mesa;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id", foreignKey = @ForeignKey(name = "fk_padron_periodo"))
    private Periodo periodo;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proce_id", nullable = false, foreignKey = @ForeignKey(name = "fk_padron_proceso_electoral"))
    private ProcesoElectoral proceso;

    @Getter
    @Setter
    @Column(name = "sufrago", nullable = false)
    protected Boolean sufrago = false;

    public Padron() {
    }

    public Padron(Mesa mesa, Periodo periodo, IglesiaPersona iglesiaPersona) {
        this.mesa = mesa;
        this.periodo = periodo;
        this.iglesiaPersona = iglesiaPersona;
    }

    public Padron(Mesa mesa, ProcesoElectoral proceso, IglesiaPersona iglesiaPersona) {
        this.mesa = mesa;
        this.proceso = proceso;
        this.iglesiaPersona = iglesiaPersona;
    }
}
