package ec.com.antenasur.domain.tec;

import java.io.Serializable;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import ec.com.antenasur.domain.generic.EntidadAuditable;
import ec.com.antenasur.domain.generic.EntidadBase;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the rpm_general_catalogue database table.
 *
 */
@Entity
@Table(name = "catalogo_general", schema = "tec")

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class CatalogoGeneral extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "catalogo_id")
    private Integer id;

    @Setter
    @Getter
    @Column(name = "catalogo_descripcion")
    private String descripcion;

    @Setter
    @Getter
    @Column(name = "catalogo_historial_id")
    private Integer historial;

    @Setter
    @Getter
    @Column(name = "catalogo_nombre")
    private String nombre;

    @Setter
    @Getter
    @Column(name = "catalogo_orden")
    private Integer orden;

    @Setter
    @Getter
    @Column(name = "catalogo_info")
    private String info;

    // bi-directional many-to-one association to GeneralCatalogue
    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalogo_padre_id")
    private CatalogoGeneral padre;

    // bi-directional many-to-one association to GeneralCatalogue
    @Setter
    @Getter
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "padre")
    private List<CatalogoGeneral> listCatalogoGeneralHijos;

    public CatalogoGeneral() {
    }

}
