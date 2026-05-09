package ec.com.antenasur.model.tec;

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
import jakarta.persistence.Table;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import ec.com.antenasur.enums.EstadoTarea;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the tec_recintos database table.
 *
 */
@Entity
@Table(name = "recintos", schema = "tec")

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})

@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class Recinto extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rec_id")
    @Setter
    @Getter
    private Integer id;

    @Setter
    @Getter
    @Column(name = "rec_nombre")
    private String nombre;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "gelo_id")
    private Geograp ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_tarea", length = 100)
    @Getter
    @Setter
    private EstadoTarea estadoTarea;

    public Recinto() {
    }

}
