package ec.com.antenasur.domain.tec;

import ec.com.antenasur.domain.IglesiaPersona;
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
import javax.persistence.Table;

import ec.com.antenasur.domain.generic.EntidadAuditable;
import ec.com.antenasur.domain.generic.EntidadBase;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the tec_recintos database table.
 *
 */
@Entity
@Table(name = "padron", schema = "tec")

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Audited
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
public class Padron extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "padron_id")
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

    @Getter
    @Setter
    @Column(name = "sufrago")
    protected Boolean sufrago = false;

    public Padron() {
    }

    public Padron(Mesa mesa, Periodo periodo, IglesiaPersona iglesiaPersona) {
        this.mesa = mesa;
        this.periodo = periodo;
        this.iglesiaPersona = iglesiaPersona;
    }
}
