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
 * The persistent class for the tb_role_user database table.
 *
 */
@XmlRootElement
@Entity
@Table(name = "tb_role_user", schema = "public")
@NamedQuery(name = "RolUsuario.findAll", query = "SELECT r FROM RolUsuario r")
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class RolUsuario extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rous_id")
    @Setter
    @Getter
    private Integer id;

    // bi-directional many-to-one association to Rol
    @ManyToOne
    @JoinColumn(name = "rol_id")
    @Setter
    @Getter
    private Rol rol;

    // bi-directional many-to-one association to Usuario
    @ManyToOne
    @JoinColumn(name = "usu_id")
    @Setter
    @Getter
    private Usuario usuario;

    public RolUsuario() {
    }

}
