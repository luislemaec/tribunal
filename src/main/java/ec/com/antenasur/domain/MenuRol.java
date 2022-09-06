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
 * The persistent class for the tb_menu_role database table.
 *
 */
@Entity
@XmlRootElement
@Table(name = "tb_menu_rol", schema = "public")
@NamedQuery(name = "MenuRol.findAll", query = "SELECT m FROM MenuRol m")
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class MenuRol extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mero_id")
    private Integer id;

    // bi-directional many-to-one association to Menu
    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "menu_id")
    private Menu menu;

    // bi-directional many-to-one association to Rol
    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "rol_id")
    private Rol rol;

    public MenuRol() {
    }

    public MenuRol(Menu menu, Rol rol) {
        this.menu = menu;
        this.rol = rol;
    }

}
