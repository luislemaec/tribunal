package ec.com.antenasur.model;

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
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlRootElement;

import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import java.sql.Timestamp;
import jakarta.persistence.Transient;
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
@Table(name = "tb_iglesia_persona", schema = "public")
@NamedQuery(name = "IglesiaPersona.findAll", query = "SELECT u FROM IglesiaPersona u")

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "iglesiaCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "iglesiaActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class IglesiaPersona extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "igpe_id")
    private Integer id;

    // bi-directional many-to-one association to Rol
    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "igl_id")
    private Iglesia iglesia;

    // bi-directional many-to-one association to Rol
    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "pers_id")
    private Persona persona;

    @Setter
    @Getter
    @Column(name = "igpe_f_desde")
    private Timestamp desde;

    @Setter
    @Getter
    @Column(name = "igpe_f_hasta")
    private Timestamp hasta;

    @Setter
    @Getter
    @Transient
    private String novedad;

    public IglesiaPersona() {
    }

    public IglesiaPersona(Iglesia iglesia, Persona persona, Timestamp desde, Timestamp hasta) {
        this.iglesia = iglesia;
        this.persona = persona;
        this.desde = desde;
        this.hasta = hasta;
    }

    public IglesiaPersona(Iglesia iglesia, Persona persona) {
        this.iglesia = iglesia;
        this.persona = persona;
    }

    public IglesiaPersona(Iglesia iglesia, Persona persona, String novedad) {
        this.iglesia = iglesia;
        this.persona = persona;
        this.novedad = novedad;
    }
}
