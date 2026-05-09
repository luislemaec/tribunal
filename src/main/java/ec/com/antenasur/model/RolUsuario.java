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
