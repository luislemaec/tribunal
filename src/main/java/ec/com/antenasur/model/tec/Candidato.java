package ec.com.antenasur.model.tec;

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

import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the tec_recintos database table.
 *
 */
@Entity
@Table(name = "candidatos", schema = "tec",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_candidatos_proceso_lista_cargo_persona",
                    columnNames = {"proce_id", "lista_id", "cargo_id", "igpe_id"})
        },
        indexes = {
            @Index(name = "idx_candidatos_proce_id", columnList = "proce_id"),
            @Index(name = "idx_candidatos_lista_id", columnList = "lista_id"),
            @Index(name = "idx_candidatos_cargo_id", columnList = "cargo_id"),
            @Index(name = "idx_candidatos_igpe_id", columnList = "igpe_id")
        })

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})

@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited

@AllArgsConstructor
@NoArgsConstructor
public class Candidato extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cand_id")
    private Integer id;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "igpe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_candidatos_iglesia_persona"))
    private IglesiaPersona iglesiaPersona;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_id", nullable = false, foreignKey = @ForeignKey(name = "fk_candidatos_lista"))
    private Lista lista;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id", foreignKey = @ForeignKey(name = "fk_candidatos_periodo"))
    private Periodo periodo;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proce_id", nullable = false, foreignKey = @ForeignKey(name = "fk_candidatos_proceso_electoral"))
    private ProcesoElectoral proceso;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_candidatos_cargo"))
    private CatalogoGeneral cargo;

}
