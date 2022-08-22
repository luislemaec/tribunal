/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * AND open the template in the editor.
 */
package ec.com.antenasur.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.domain.CatalogoGeneral;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class CatalogoGeneralFacade extends AbstractFacade<CatalogoGeneral, Integer> {

    public CatalogoGeneralFacade() {
        super(CatalogoGeneral.class, Integer.class);
    }

    public List<CatalogoGeneral> findByFatherCatalogue(CatalogoGeneral generalCatalogue) {
        try {
            String sql = "FROM CatalogoGeneral gc WHERE gc.padre=:generalCatalogue AND gc.estado=TRUE";
            TypedQuery<CatalogoGeneral> query = super.getEntityManager().createQuery(sql, CatalogoGeneral.class);
            query.setParameter("generalCatalogue", generalCatalogue);
            List<CatalogoGeneral> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public CatalogoGeneral findByName(String catalogoNombre) {
        try {
            String sql = "FROM CatalogoGeneral gc WHERE gc.nombre=:catalogoNombre AND gc.estado=TRUE";
            TypedQuery<CatalogoGeneral> query = super.getEntityManager().createQuery(sql, CatalogoGeneral.class);
            query.setParameter("catalogoNombre", catalogoNombre);
            List<CatalogoGeneral> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<CatalogoGeneral> findByFatherName(String catalogoNombre) {
        try {

            String sql = "FROM CatalogoGeneral gc WHERE gc.padre.nombre=:catalogoNombre AND gc.estado=TRUE ORDER BY orden";
            TypedQuery<CatalogoGeneral> query = super.getEntityManager().createQuery(sql, CatalogoGeneral.class);
            query.setParameter("catalogoNombre", catalogoNombre);
            List<CatalogoGeneral> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<CatalogoGeneral> listaCatalogoHijo(Integer padreId) {
        try {
            String sql = "FROM CatalogoGeneral gc WHERE gc.padre.id=:padreId AND gc.estado=TRUE ORDER BY orden";
            TypedQuery<CatalogoGeneral> query = super.getEntityManager().createQuery(sql, CatalogoGeneral.class);
            query.setParameter("padreId", padreId);
            List<CatalogoGeneral> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     *
     *
     * @autor Luis Lema
     * @fecha 09-08-2022
     * @param padreId catalogo padre
     * @param listaIdCargos lista de cargo asignados
     * @return lista de cargos pendientes de asignar
     */
    public List<CatalogoGeneral> listaCatalogoHijo(Integer padreId, List<Integer> listaIdCargos) {
        try {
            String sql = "FROM CatalogoGeneral gc WHERE gc.padre.id=:padreId AND gc.id NOT IN :listaIdCargos AND gc.estado=TRUE ORDER BY orden";
            TypedQuery<CatalogoGeneral> query = super.getEntityManager().createQuery(sql, CatalogoGeneral.class);
            query.setParameter("padreId", padreId);
            query.setParameter("listaIdCargos", listaIdCargos);
            List<CatalogoGeneral> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<CatalogoGeneral> findByFather() {
        try {
            String sql = "FROM CatalogoGeneral gc WHERE gc.padre.id is null AND gc.estado=TRUE ORDER BY orden";
            TypedQuery<CatalogoGeneral> query = super.getEntityManager().createQuery(sql, CatalogoGeneral.class);
            List<CatalogoGeneral> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public CatalogoGeneral findByFatherIdAndCatalogueName(Integer idPadre, String nombreCatalogo) {
        try {
            String sql = "FROM CatalogoGeneral m WHERE m.padre.id=:idPadre AND m.nombre LIKE '%'||:nombreCatalogo||'%'";

            TypedQuery<CatalogoGeneral> query = super.getEntityManager().createQuery(sql, CatalogoGeneral.class);
            query.setParameter("idPadre", idPadre);
            query.setParameter("nombreCatalogo", nombreCatalogo);
            List<CatalogoGeneral> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public List<CatalogoGeneral> findByFatherName2(String nombreCatalogo) {
        try {

            String sql = "FROM CatalogoGeneral gc WHERE gc.padre.nombre=:nombreCatalogo ORDER BY orden";
            TypedQuery<CatalogoGeneral> query = super.getEntityManager().createQuery(sql, CatalogoGeneral.class);
            query.setParameter("nombreCatalogo", nombreCatalogo);
            List<CatalogoGeneral> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<CatalogoGeneral> findByFatherNameInactive(String nombreCatalogo) {
        try {
            String sql = "FROM CatalogoGeneral gc WHERE gc.padre.nombre=:nombreCatalogo AND gc.estado=false ORDER BY orden";
            TypedQuery<CatalogoGeneral> query = super.getEntityManager().createQuery(sql, CatalogoGeneral.class);
            query.setParameter("nombreCatalogo", nombreCatalogo);
            List<CatalogoGeneral> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public CatalogoGeneral findByNameAll(String nombreCatalogo) {
        try {
            String sql = "FROM CatalogoGeneral gc WHERE gc.nombre=:nombreCatalogo";
            TypedQuery<CatalogoGeneral> query = super.getEntityManager().createQuery(sql, CatalogoGeneral.class);
            query.setParameter("nombreCatalogo", nombreCatalogo);
            List<CatalogoGeneral> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     *
     * @param nombreCatalogo variable
     * @param tamanioCadena tamanio de la cadena a comparar
     * @return catalogo buscado
     */
    public CatalogoGeneral findByNamelike(String nombreCatalogo, int tamanioCadena) {
        try {
            String sql = "FROM CatalogoGeneral gc WHERE gc.nombre LIKE CONCAT(:generalCatalogueName,'%')";
            TypedQuery<CatalogoGeneral> query = super.getEntityManager().createQuery(sql, CatalogoGeneral.class);
            query.setParameter("nombreCatalogo", nombreCatalogo.substring(0, tamanioCadena));
            List<CatalogoGeneral> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

}
