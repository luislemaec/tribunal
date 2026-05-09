package ec.com.antenasur.model.tec;

import java.io.Serializable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * The persistent class for the tb_menu database table.
 *
 */
@Entity
@XmlRootElement
@Table(name = "correo_plantilla", schema = "tec")
@NamedQuery(name = "PlantillaCorreo.findAll", query = "SELECT m FROM PlantillaCorreo m")
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})

@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class PlantillaCorreo extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "corr_plan_id")
    private Integer id;

    @Setter
    @Getter
    @Column(name = "asunto", length = 500)
    private String asunto;

    /**
     * Cuerpo de la plantilla de correo (HTML). Mapeado a TEXT en PostgreSQL
     * porque puede contener contenido extenso (>255 chars).
     */
    @Setter
    @Getter
    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;

    @Setter
    @Getter
    @Column(name = "descripcion", length = 500)
    private String descripcion;

    public PlantillaCorreo() {
    }

}
