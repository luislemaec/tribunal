package ec.com.antenasur.domain.generic;

import java.io.Serializable;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.envers.Audited;

/**
 * POJO Entidad base de la que heredan todas las entidades del negocio
 *
 * @author Luis Lema
 *
 */
@MappedSuperclass
@FilterDef(name = EntidadBase.FILTER_ACTIVE)
@EntityListeners(EntityAuditable.class)
public abstract class EntidadBase implements Serializable {

    private static final long serialVersionUID = -8952068548320139605L;

    public static final String FILTER_ACTIVE = "filterActive";

    @Getter
    @Setter
    @Audited
    protected Boolean estado = true;

    @Getter
    @Setter
    @Transient
    private boolean seleccionado;

    public abstract Integer getId();

    public abstract void setId(Integer id);

    public boolean isPersisted() {
        return getId() != null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) 
            return false;
        EntidadBase base = (EntidadBase) obj;
        if (this.getId() == null && base.getId() == null) 
            return super.equals(obj);
        if (this.getId() == null || base.getId() == null) 
            return false;
        return this.getId().equals(base.getId());
    }

}
