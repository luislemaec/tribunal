package ec.com.antenasur.domain;

import ec.com.antenasur.domain.generic.EntidadBase;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import lombok.Data;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the rpm_geograp database table.
 *
 */
@Data
@Entity
@Table(name = "tb_geograp", schema = "public")
@NamedQuery(name = "Geograp.findAll", query = "SELECT g FROM Geograp g")
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "gelo_status = 'TRUE'")
@Audited
public class Geograp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gelo_id")
    private Integer id;

    @Column(name = "gelo_codification_inec")
    private String codificationInec;

    @Column(name = "gelo_name")
    private String name;

    @Column(name = "gelo_observations")
    private String observations;

    @ManyToOne
    @JoinColumn(name = "gelo_parent_id")
    private Geograp geograp;

    @Column(name = "gelo_status")
    private Boolean status;

    @Column(name = "pglo_id")
    private Integer pgloId;

    @Column(name = "zone_id")
    private Integer zoneId;

    // bi-directional many-to-one association to Media
    public Geograp() {
    }

}
