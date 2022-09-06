package ec.com.antenasur.domain;

import java.io.Serializable;
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
 * The persistent class for the tb_menu database table.
 *
 */
@Entity
@XmlRootElement
@Table(name = "tb_menu", schema = "public")
@NamedQuery(name = "Menu.findAll", query = "SELECT m FROM Menu m")
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})

@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class Menu extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Integer id;

    @Setter
    @Getter
    @Column(name = "menu_accion")
    private String accion;

    @Setter
    @Getter
    @Column(name = "menu_nodo_final")
    private Boolean nodoFinal;

    @Setter
    @Getter
    @Column(name = "menu_ico")
    private String icono;

    @Setter
    @Getter
    @Column(name = "menu_img")
    private String imagen;

    @Setter
    @Getter
    @Column(name = "menu_nombre")
    private String nombre;

    @Setter
    @Getter
    @Column(name = "menu_orden")
    private Integer orden;

    @Setter
    @Getter
    @Column(name = "menu_url")
    private String url;

    @Setter
    @Getter
    @Column(name = "componente_id")
    private String componenteid;

    // bi-directional many-to-one association to Menu
    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "menu_padre_id")
    private Menu padre;

    // bi-directional many-to-one association to Menu
    @Setter
    @Getter
    @OneToMany(mappedBy = "padre")
    private List<Menu> menus;

    // bi-directional many-to-one association to MenuRol
    @Setter
    @Getter
    @OneToMany(mappedBy = "menu")
    private List<MenuRol> menuRoles;

    public Menu() {
    }

}
