package ec.com.antenasur.model.tec;

import java.io.Serializable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
@Table(name = "escrutinio", schema = "tec")

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
    @ManyToOne
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "periodo_id")
    private Periodo periodo;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "cat_voto_id")
    private CategoriaVoto categoria;

    @Setter
    @Getter
    @Column(name = "total_votos")
    private Integer totalVotos;

    public Escrutinio() {
    }

}
