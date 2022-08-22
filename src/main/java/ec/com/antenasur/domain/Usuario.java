package ec.com.antenasur.domain;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

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
import javax.persistence.OneToMany;
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
@Table(name = "tb_usuario", schema = "public")
@NamedQuery(name = "Usuario.findAll", query = "SELECT u FROM Usuario u")

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Audited
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
public class Usuario extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usu_id")
    private Integer id;

    @Setter
    @Getter
    @Column(name = "usu_f_expiracion")
    private Timestamp usuarioFechaExpira;

    @Setter
    @Getter
    @Column(name = "usu_link")
    private String link;

    @Setter
    @Getter
    @Column(name = "usu_correo")
    private String correo;

    @Setter
    @Getter
    @Column(name = "usu_nombre")
    private String username;

    @Setter
    @Getter
    @Column(name = "usu_clave")
    private String contrasenia;

    @Setter
    @Getter
    @Column(name = "usu_clave_temp")
    private String contraseniaTemp;

    @Setter
    @Getter
    @Column(name = "usu_permanente")
    private Boolean permanente;

    // bi-directional many-to-one association to RolUsuario
    @Setter
    @Getter
    @OneToMany(mappedBy = "usuario")
    private List<RolUsuario> rolUsuarios;

    // bi-directional many-to-one association to Persona
    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "pers_id")
    private Persona personsa;

    public Usuario() {
    }

}
