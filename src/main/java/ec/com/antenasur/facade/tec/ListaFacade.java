/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade.tec;


import java.util.Collections;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.generic.EntidadBase;
import org.hibernate.Filter;
import org.hibernate.Session;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class ListaFacade extends AbstractFacade<Lista, Integer> {

    private static final String HQL = " SELECT l FROM Lista l";

    public ListaFacade() {
        super(Lista.class, Integer.class);
    }

    public List<Lista> buscarPaginado(String termino, boolean incluirInactivas,
            int inicio, int limite, String campoOrden, boolean ascendente) {
        Session session = getEntityManager().unwrap(Session.class);
        Filter filtroActivo = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
        if (incluirInactivas && filtroActivo != null) {
            session.disableFilter(EntidadBase.FILTER_ACTIVE);
        }
        try {
            TypedQuery<Lista> query = session.createQuery(construirConsulta(termino,
                    incluirInactivas, campoOrden, ascendente), Lista.class);
            establecerTermino(query, termino);
            query.setFirstResult(Math.max(0, inicio));
            query.setMaxResults(Math.max(1, limite));
            return query.getResultList();
        } finally {
            if (incluirInactivas && filtroActivo != null) {
                session.enableFilter(EntidadBase.FILTER_ACTIVE);
            }
        }
    }

    public int contar(String termino, boolean incluirInactivas) {
        Session session = getEntityManager().unwrap(Session.class);
        Filter filtroActivo = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
        if (incluirInactivas && filtroActivo != null) {
            session.disableFilter(EntidadBase.FILTER_ACTIVE);
        }
        try {
            StringBuilder hql = new StringBuilder("SELECT COUNT(l) FROM Lista l");
            agregarCondiciones(hql, termino, incluirInactivas);
            TypedQuery<Long> query = session.createQuery(hql.toString(), Long.class);
            establecerTermino(query, termino);
            return query.getSingleResult().intValue();
        } finally {
            if (incluirInactivas && filtroActivo != null) {
                session.enableFilter(EntidadBase.FILTER_ACTIVE);
            }
        }
    }

    public List<Lista> buscarPorNombreONumeroIncluyendoInactivas(String nombre, String numero) {
        if ((nombre == null || nombre.isBlank()) && (numero == null || numero.isBlank())) {
            return Collections.emptyList();
        }
        Session session = getEntityManager().unwrap(Session.class);
        Filter filtroActivo = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
        if (filtroActivo != null) {
            session.disableFilter(EntidadBase.FILTER_ACTIVE);
        }
        try {
            TypedQuery<Lista> query = session.createQuery(
                    HQL + " WHERE LOWER(TRIM(l.nombre)) = :nombre OR TRIM(l.numero) = :numero"
                    + " ORDER BY l.estado DESC, l.id", Lista.class);
            query.setParameter("nombre", nombre == null ? "" : nombre.trim().toLowerCase());
            query.setParameter("numero", numero == null ? "" : numero.trim());
            return query.getResultList();
        } finally {
            if (filtroActivo != null) {
                session.enableFilter(EntidadBase.FILTER_ACTIVE);
            }
        }
    }

    public Lista buscarPorIdIncluyendoInactivas(Integer id) {
        if (id == null) {
            return null;
        }
        Session session = getEntityManager().unwrap(Session.class);
        Filter filtroActivo = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
        if (filtroActivo != null) {
            session.disableFilter(EntidadBase.FILTER_ACTIVE);
        }
        try {
            return session.find(Lista.class, id);
        } finally {
            if (filtroActivo != null) {
                session.enableFilter(EntidadBase.FILTER_ACTIVE);
            }
        }
    }

    private String construirConsulta(String termino, boolean incluirInactivas,
            String campoOrden, boolean ascendente) {
        StringBuilder hql = new StringBuilder(HQL);
        agregarCondiciones(hql, termino, incluirInactivas);
        String campo = switch (campoOrden == null ? "" : campoOrden) {
            case "nombre" -> "l.nombre";
            case "slogan" -> "l.slogan";
            case "numero" -> "l.numero";
            default -> "l.id";
        };
        hql.append(" ORDER BY ").append(campo).append(ascendente ? " ASC" : " DESC")
                .append(", l.id ASC");
        return hql.toString();
    }

    private void agregarCondiciones(StringBuilder hql, String termino, boolean incluirInactivas) {
        boolean tieneTermino = termino != null && !termino.isBlank();
        if (!incluirInactivas || tieneTermino) {
            hql.append(" WHERE ");
            if (!incluirInactivas) {
                hql.append("l.estado = TRUE");
            }
            if (!incluirInactivas && tieneTermino) {
                hql.append(" AND ");
            }
            if (tieneTermino) {
                hql.append("(LOWER(l.nombre) LIKE :termino OR LOWER(l.slogan) LIKE :termino"
                        + " OR LOWER(l.numero) LIKE :termino)");
            }
        }
    }

    private void establecerTermino(TypedQuery<?> query, String termino) {
        if (termino != null && !termino.isBlank()) {
            query.setParameter("termino", "%" + termino.trim().toLowerCase() + "%");
        }
    }

}
