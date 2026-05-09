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

import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the tec_recintos database table.
 *
 */
@Entity
@Table(name = "miembros_jrv", schema = "tec")

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Audited
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
public class MiembroJRV extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "miem_id")
    private Integer id;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "igpe_id")
    private IglesiaPersona iglesiaPersona;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "periodo_id")
    private Periodo periodo;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "cargo_id")
    private CatalogoGeneral cargo;

    public MiembroJRV() {
    }

}
