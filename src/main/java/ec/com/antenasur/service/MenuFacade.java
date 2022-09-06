/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import ec.com.antenasur.domain.Menu;
import ec.com.antenasur.domain.RolUsuario;
import ec.com.antenasur.domain.generic.AbstractFacade;
import ec.com.antenasur.util.MenuVO;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class MenuFacade extends AbstractFacade<Menu, Integer> {

    public MenuFacade() {
        super(Menu.class, Integer.class);
    }

    public Menu findByMnemonic(String mnemonic) {
        TypedQuery<Menu> query = super.getEntityManager().createQuery("select o from Menu o where o.estado = true and o.nombre = :mnemonic", Menu.class);
        query.setParameter("mnemonic", mnemonic);
        return (Menu) query.getSingleResult();

    }

    /**
     * Buscar por rols
     *
     * @param rolsUsers
     * @param menu de aplicacion
     * @return MenuVO
     */
    public List<MenuVO> getMenusByrols(List<RolUsuario> rolsUsers, String menuMnemonic) {
        try {

            List<MenuVO> menus = null;
            Integer userId = 0;
            Integer parentMenu = 0;
            List<Integer> listarols = new ArrayList<Integer>();
            for (RolUsuario rolUser : rolsUsers) {
                userId = rolUser.getUsuario().getId();
                listarols.add(rolUser.getRol().getId());
            }

            List<Menu> menusList = findByrols(listarols, menuMnemonic);
            if (menusList != null) {
                menus = new ArrayList<MenuVO>();
                for (Menu menu : menusList) {
                    if (menu.getPadre() != null) {
                        parentMenu = (menu.getPadre().getId() != null || menu.getPadre().getId() != 0) ? menu.getPadre().getId()
                                : 0;

                        menus.add(new MenuVO(menu.getId(), menu.getNombre(), menu.getAccion(), menu.getUrl(), parentMenu, userId,
                                menu.getNodoFinal(), menu.getOrden(), menu.getIcono()));
                    }
                }
            }

            return menus;

        } catch (Exception e) {
            return null;
        }
    }

    private List<Menu> findByrols(List<Integer> rols, String menuMnemonic) {
        List<Menu> menus = new ArrayList<Menu>();
        try {
            if (rols.size() > 0) {
                //String hql = "select distinct mr.menu.id from MenuRol mr where mr.estado=true and mr.rol.id in :rols and (mr.menu.nombre != :menuMnemonic or mr.menu.nombre is null) ";

                String sql = "SELECT distinct m.menu_id from tb_menu_rol mr "
                        + "INNER JOIN tb_menu m on m.menu_id=mr.menu_id "
                        + "INNER JOIN tb_rol r on r.rol_id=mr.rol_id "
                        + "WHERE mr.estado= TRUE and mr.rol_id in :rols "
                        + "and m.menu_id in("
                        + "WITH RECURSIVE search_perspectives(id) AS ("
                        + "SELECT m.menu_id from tb_menu m WHERE m.estado= TRUE and m.menu_nombre =:menuMnemonic "
                        + "UNION ALL SELECT mm.menu_id from tb_menu mm, search_perspectives s WHERE mm.menu_padre_id = s.id  and mm.estado= TRUE ) "
                        + "SELECT * FROM search_perspectives)";
                //Query queryIds = super.getEntityManager().createQuery(hql);
                Query queryIds1 = super.getEntityManager().createNativeQuery(sql);
                queryIds1.setParameter("rols", rols);
                queryIds1.setParameter("menuMnemonic", menuMnemonic);
                List<Integer> menuIds = queryIds1.getResultList();
                List<Integer> menuIds1 = new ArrayList<Integer>();
                for (Integer item : menuIds) {
                    menuIds1.add(item.intValue());
                }

                String hql1 = "SELECT m from Menu m where m.id in :menuIds and m.estado= true order by m.orden";
                Query query = super.getEntityManager().createQuery(hql1);
                query.setParameter("menuIds", menuIds1);
                menus = (List<Menu>) query.getResultList();

                return menus;
            }
        } catch (Exception e) {
            System.out
                    .println("Error en 'private List<Menu> findByrols(List<Integer> roles, String menuMnemonic)':::: "
                            + e.getMessage());
            e.printStackTrace();
            menus = null;
        }

        return menus;

    }

    public Menu findByMenuName(String menuapp) {
        String hql = "select m from Menu m where m.estado = true and m.nombre = :menuapp";
        TypedQuery<Menu> query = super.getEntityManager().createQuery(hql, Menu.class);
        query.setParameter("menuapp", menuapp);
        return (Menu) query.getSingleResult();

    }

    public List<Menu> findByFather() {
        try {
            String sql = "FROM Menu m WHERE m.padre.id is null AND m.estado=TRUE ORDER BY orden";
            TypedQuery<Menu> query = super.getEntityManager().createQuery(sql, Menu.class);
            List<Menu> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public List<Menu> listaCatalogoHijo(Integer padreId) {
        try {
            String sql = "FROM Menu m WHERE m.padre.id=:padreId ORDER BY orden";
            TypedQuery<Menu> query = super.getEntityManager().createQuery(sql, Menu.class);
            query.setParameter("padreId", padreId);
            List<Menu> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public List<Menu> getMenusFinales() {
         try {
            String sql = "FROM Menu m WHERE m.nodoFinal=TRUE ORDER BY orden";
            TypedQuery<Menu> query = super.getEntityManager().createQuery(sql, Menu.class);
            
            List<Menu> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
