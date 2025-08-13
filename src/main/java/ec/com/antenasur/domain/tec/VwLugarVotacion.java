package ec.com.antenasur.domain.tec;

import ec.com.antenasur.domain.Geograp;

import java.io.Serializable;
import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;

/**
 * The persistent class for the vw_subtotal database table.
 *
 */
@Entity
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
