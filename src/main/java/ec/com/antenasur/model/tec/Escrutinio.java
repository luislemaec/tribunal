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

import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import ec.com.antenasur.model.tec.CategoriaVoto;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the tec_recintos database table.
 *
 */
@Entity
@Table(name = "escrutinio", schema = "tec",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_escrutinio_proceso_mesa_categoria",
                    columnNames = {"proce_id", "mesa_id", "cat_voto_id"})
        },
        indexes = {
            @Index(name = "idx_escrutinio_proce_id", columnList = "proce_id"),
            @Index(name = "idx_escrutinio_mesa_id", columnList = "mesa_id"),
            @Index(name = "idx_escrutinio_categoria_id", columnList = "cat_voto_id")
        })

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class Escrutinio extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "escr_id")
    private Integer id;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_escrutinio_mesa"))
    private Mesa mesa;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id", foreignKey = @ForeignKey(name = "fk_escrutinio_periodo"))
    private Periodo periodo;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proce_id", nullable = false, foreignKey = @ForeignKey(name = "fk_escrutinio_proceso_electoral"))
    private ProcesoElectoral proceso;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cat_voto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_escrutinio_categoria_voto"))
    private CategoriaVoto categoria;

    @Setter
    @Getter
    @Column(name = "total_votos", nullable = false)
    private Integer totalVotos;

    public Escrutinio() {
    }

}
