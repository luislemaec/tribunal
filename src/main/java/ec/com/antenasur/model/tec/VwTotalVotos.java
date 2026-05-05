package ec.com.antenasur.model.tec;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.generic.EntidadBase;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Immutable;

import lombok.Getter;
import lombok.Setter;

/**
 * Mapeo a la <b>vista</b> PostgreSQL {@code tec.vw_total_escrutinios}.
 *
 * <p>Como es una vista (no una tabla), no admite constraints. Los
 * {@code @JoinColumn} llevan {@code ConstraintMode.NO_CONSTRAINT} para que
 * Hibernate (con {@code hbm2ddl.auto=update}) no intente generar FKs que
 * fallarían contra la vista. La entidad es {@code @Immutable} — solo lectura.
 */
@Entity
@Immutable
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
    @JoinColumn(name = "rec_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private Recinto recinto;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "mesa_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private Mesa mesa;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "gelo_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private Geograp geograp;

    @Setter
    @Getter
    private boolean estado;
}
