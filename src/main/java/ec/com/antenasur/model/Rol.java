package ec.com.antenasur.model;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlRootElement;

import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the tb_role database table.
 *
 */
@XmlRootElement
@Entity
@Table(name = "tb_rol", schema = "public")
@NamedQuery(name = "Rol.findAll", query = "SELECT r FROM Rol r")
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})

@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class Rol extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_id")
    @Setter
    @Getter
    private Integer id;

    @Column(name = "rol_description")
    @Setter
    @Getter
    private String descripcion;

    @Column(name = "rol_nombre")
    @Setter
    @Getter
    private String nombre;

    // bi-directional many-to-one association to MenuRol
    @OneToMany(mappedBy = "rol")
    @Setter
    @Getter
    private List<MenuRol> menuRoles;

    // bi-directional many-to-one association to RolUsuario
    @OneToMany(mappedBy = "rol")
    @Setter
    @Getter
    private List<RolUsuario> rolUsuarios;

    public Rol() {
    }

}
