/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service.tec;

import javax.ejb.Stateless;

import ec.com.antenasur.domain.tec.Documentos;
import ec.com.antenasur.domain.generic.AbstractFacade;
import ec.com.antenasur.domain.tec.Mesa;

import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class DocumentoFacade extends AbstractFacade<Documentos, Integer> {

    private static final String HQL = " FROM Documentos d";
    private static final String ORDENADO = " ORDER BY d.id";

    public DocumentoFacade() {
        super(Documentos.class, Integer.class);
    }

    public List<Documentos> getDocumentosPorMesa(Mesa mesa) {
        try {
            String sql = HQL + " WHERE d.mesa=:mesa" + ORDENADO;
            TypedQuery<Documentos> query = super.getEntityManager().createQuery(sql, Documentos.class);
            query.setParameter("mesa", mesa);
            List<Documentos> result = query.getResultList();
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public Documentos obtenerDocumentoPorWorkspace(String workspace) {
        try {
            String sql = "FROM Documentos WHERE path =:workspace";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("workspace", workspace);
            List<Documentos> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }
}
