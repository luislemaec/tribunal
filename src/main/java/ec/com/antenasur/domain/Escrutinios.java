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
@Table(name = "escrutinios", schema = "tec")

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class Escrutinios extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "escr_id")
    private Integer id;

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
    @Column(name = "votos_lista_uno")
    private Integer listaUno;

    @Setter
    @Getter
    @Column(name = "votos_lista_dos")
    private Integer listaDos;

    @Setter
    @Getter
    @Column(name = "votos_lista_tres")
    private Integer listaTres;

    @Setter
    @Getter
    @Column(name = "votos_nulos")
    private Integer nulos;

    @Setter
    @Getter
    @Column(name = "votos_blancos")
    private Integer blancos;

    @Setter
    @Getter
    @Column(name = "total_votos")
    private Integer totalVotos;

    public Escrutinios() {
    }

}
