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

import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import java.util.Date;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * The persistent class for the sel_proceso database table.
 *
 */
@Entity
@Table(name = "procesos", schema = "tec")
@NamedQuery(name = "Proceso.findAll", query = "SELECT p FROM Proceso p")
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
public class Proceso extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proceso_id")
    @Setter
    @Getter
    private Integer id;

    @Setter
    @Getter
    private String actividad;

    @Setter
    @Getter
    private String ip;

    @Setter
    @Getter
    @Transient
    private String horas;

    @Setter
    @Getter
    @Transient
    private String dias;

    public Proceso() {
    }

    public Proceso(String ip) {
        this.ip = ip;
    }

}
