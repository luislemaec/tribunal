package ec.com.antenasur.domain;

import java.io.Serializable;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import ec.com.antenasur.domain.generic.EntidadAuditable;
import ec.com.antenasur.domain.generic.EntidadBase;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the tb_user database table.
 *
 */
@XmlRootElement
@Entity
@Table(name = "tb_iglesia", schema = "public")
@NamedQuery(name = "Iglesia.findAll", query = "SELECT u FROM Iglesia u")

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "iglesiaCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "iglesiaActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class Iglesia extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "igl_id")
    private Integer id;

    @Setter
    @Getter
    @Column(name = "igl_nombre")
    private String nombre;

    @Setter
    @Getter
    @Column(name = "igl_documento")
    private String documento;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "gelo_id")
    private Geograp ubicacion;

    public Iglesia() {
    }

}
