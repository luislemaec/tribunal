package ec.com.antenasur.domain.tec;

import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.generic.EntidadBase;

import java.io.Serializable;
import javax.persistence.*;

import org.hibernate.annotations.Filter;

import lombok.Getter;
import lombok.Setter;

/**
 * The persistent class for the vw_subtotal database table.
 *
 */
@Entity
@Table(name = "vw_total_escrutinios", schema = "tec")
@NamedQuery(name = "VwTotalVotos.findAll", query = "SELECT v FROM VwTotalVotos v")
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
public class VwTotalVotos implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Setter
    @Getter
    @Column(name = "escr_id")
    private Integer id;

    @Setter
    @Getter    
    @Column(name = "nombre")
    private String categoria;

    @Setter
    @Getter    
    @Column(name = "t_votos")
    private Integer totalVotos;
    
    @Setter
    @Getter
    @Column(name = "cat_orden")
    private Integer orden;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "rec_id")
    private Recinto recinto;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "gelo_id")
    private Geograp geograp;
    
    @Setter
    @Getter        
    private boolean estado;
    

}
