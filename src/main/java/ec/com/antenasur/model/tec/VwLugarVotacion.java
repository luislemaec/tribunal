package ec.com.antenasur.model.tec;

import java.io.Serializable;
import javax.persistence.*;

import org.hibernate.annotations.Immutable;

import lombok.Getter;
import lombok.Setter;

/**
 * Mapeo a la <b>vista</b> PostgreSQL {@code tec.vw_lugar_votacion}.
 * {@code @Immutable} para evitar que Hibernate intente generar updates.
 */
@Entity
@Immutable
@Table(name = "vw_lugar_votacion", schema = "tec")
@NamedQuery(name = "VwLugarVotacion.findAll", query = "SELECT v FROM VwLugarVotacion v")
public class VwLugarVotacion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Setter
    @Getter
    @Column(name = "padron_id")
    private Integer id;

    @Setter
    @Getter
    @Column(name = "iglesia")
    private String iglesia;

    @Setter
    @Getter
    @Column(name = "comunicad")
    private String comunidad;

    @Setter
    @Getter
    @Column(name = "cedula")
    private String cedula;

    @Setter
    @Getter
    @Column(name = "nombre")
    private String nombres;

    @Setter
    @Getter
    @Column(name = "mesa")
    private String mesa;

    @Setter
    @Getter
    @Column(name = "recinto")
    private String recinto;

    @Setter
    @Getter
    @Column(name = "parroquia")
    private String parroquia;

    @Setter
    @Getter
    @Column(name = "canton")
    private String canton;

}
