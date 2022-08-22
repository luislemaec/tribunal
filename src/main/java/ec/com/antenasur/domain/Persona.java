package ec.com.antenasur.domain;

import java.io.Serializable;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
 * The persistent class for the tb_people database table.
 *
 */
@XmlRootElement
@Entity
@Table(name = "tb_persona", schema = "public")
@NamedQuery(name = "Persona.findAll", query = "SELECT p FROM Persona p")
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class Persona extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pers_id")
    private Integer id;

    @Setter
    @Getter
    @Column(name = "pers_nombre")
    private String nombres;

    @Setter
    @Getter
    @Column(name = "pers_apellido")
    private String apellidos;

    @Setter
    @Getter
    @Column(name = "pers_documento")
    private String documento;

    @Setter
    @Getter
    @Column(name = "pers_tratamiento")
    private String tratamiento;

    @Setter
    @Getter
    @Column(name = "pers_sexo")
    private String sexo;

    public Persona() {
    }

}
