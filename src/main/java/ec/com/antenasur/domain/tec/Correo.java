package ec.com.antenasur.domain.tec;

import ec.com.antenasur.domain.generic.EntidadAuditable;
import ec.com.antenasur.domain.generic.EntidadBase;
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

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the mail_data database table.
 *
 */
@Setter
@Getter

@Entity
@Table(name = "correos_enviados", schema = "tec")
@NamedQuery(name = "Correos.findAll", query = "SELECT c FROM Correo c")

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})

@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class Correo extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    //@Column(updatable = false)
    @Column(name = "de")
    private String de;

    //@Column(updatable = false)
    @Column(name = "para")
    private String para;

    @Column(name = "usu_id")
    private Integer userId;

    @Column(name = "detalles")
    private String detalles;

    @ManyToOne
    @JoinColumn(name = "corr_plan_id")
    private PlantillaCorreo plantillaCorreo;

    public Correo() {

    }

    public Correo(String para, Integer userId, PlantillaCorreo plantillaCorreo,String detalles) {
        this.para = para;
        this.userId = userId;
        this.plantillaCorreo = plantillaCorreo;
        this.detalles = detalles;
    }

}
