package ec.com.antenasur.facade.tec;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.TipoDocumento;

@Stateless
public class TipoDocumentoFacade extends AbstractFacade<TipoDocumento, Integer> {

    public TipoDocumentoFacade() {
        super(TipoDocumento.class, Integer.class);
    }

    public TipoDocumento buscarActivoPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        TypedQuery<TipoDocumento> query = getEntityManager().createQuery(
                "SELECT t FROM TipoDocumento t WHERE UPPER(t.nombre) = :nombre AND t.estado = TRUE",
                TipoDocumento.class);
        query.setParameter("nombre", nombre.trim().toUpperCase());
        query.setMaxResults(1);
        List<TipoDocumento> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
