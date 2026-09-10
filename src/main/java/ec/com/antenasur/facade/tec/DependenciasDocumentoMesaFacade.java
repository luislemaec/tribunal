package ec.com.antenasur.facade.tec;

import java.util.List;
import jakarta.ejb.Stateless;
import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.Mesa;

/** Consultas escalares por lote: su numero no depende de la cantidad de mesas. */
@Stateless
public class DependenciasDocumentoMesaFacade extends AbstractFacade<Mesa, Integer> {
    public DependenciasDocumentoMesaFacade() { super(Mesa.class, Integer.class); }

    public List<String> tiposActivos() {
        return getEntityManager().createQuery("SELECT t.nombre FROM TipoDocumento t WHERE t.estado = TRUE", String.class).getResultList();
    }

    public List<Object[]> padrones(Integer proceso, List<Integer> mesas) {
        return getEntityManager().createQuery("SELECT p.mesa.id, COUNT(p),"
                + " SUM(CASE WHEN ip.estado = TRUE AND ip.habilitadoPadron = TRUE AND pr.estado = TRUE AND i.estado = TRUE THEN 1 ELSE 0 END),"
                + " COUNT(DISTINCT CASE WHEN ip.estado = TRUE AND ip.habilitadoPadron = TRUE AND pr.estado = TRUE AND i.estado = TRUE THEN pr.id ELSE NULL END)"
                + " FROM Padron p JOIN p.iglesiaPersona ip JOIN ip.persona pr JOIN ip.iglesia i"
                + " WHERE p.estado = TRUE AND p.proceso.id = :proceso AND p.mesa.id IN :mesas GROUP BY p.mesa.id", Object[].class)
                .setParameter("proceso", proceso).setParameter("mesas", mesas).getResultList();
    }

    public List<Object[]> cabeceras(Integer proceso, List<Integer> mesas) {
        return getEntityManager().createQuery("SELECT c.mesa.id, c.estadoEscrutinio FROM EscrutinioCabecera c"
                + " WHERE c.estado = TRUE AND c.proceso.id = :proceso AND c.mesa.id IN :mesas", Object[].class)
                .setParameter("proceso", proceso).setParameter("mesas", mesas).getResultList();
    }

    public List<Object[]> documentos(Integer proceso, List<Integer> mesas) {
        return getEntityManager().createQuery("SELECT d.mesa.id, t.nombre, d.id, d.path, d.mime FROM Documentos d JOIN d.tipoDocumento t"
                + " WHERE d.estado = TRUE AND t.estado = TRUE AND d.proceso.id = :proceso AND d.mesa.id IN :mesas"
                + " AND d.recinto.id = d.mesa.recinto.id"
                + " AND d.id = (SELECT MAX(v.id) FROM Documentos v WHERE v.estado = TRUE AND v.mesa.id = d.mesa.id"
                + " AND v.proceso.id = :proceso AND v.tipoDocumento.id = t.id)", Object[].class)
                .setParameter("proceso", proceso).setParameter("mesas", mesas).getResultList();
    }
}
