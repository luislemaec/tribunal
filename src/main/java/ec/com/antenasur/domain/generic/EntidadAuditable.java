package ec.com.antenasur.domain.generic;

import java.util.Date;

import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Getter;
import lombok.Setter;

/**
 * Entidad de la que heredan todas las entidades que se manejen en formulaios
 *
 * @author Luis Lema
 *
 */
@MappedSuperclass
public abstract class EntidadAuditable extends EntidadBase {

    private static final long serialVersionUID = -7015471629526726517L;

    @Temporal(TemporalType.TIMESTAMP)
    @Getter
    @Setter
    protected Date fechaCrea;

    @Temporal(TemporalType.TIMESTAMP)
    @Getter
    @Setter
    protected Date fechaActualiza;

    @Getter
    @Setter
    protected String usuarioCrea;

    @Getter
    @Setter
    protected String usuarioActualiza;

}
