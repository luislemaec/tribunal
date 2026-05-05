package ec.com.antenasur.service;

import java.util.List;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.generic.EntidadBase;

/**
 * Base genérica para servicios de dominio. Delega operaciones CRUD al facade
 * correspondiente. Las subclases definen lógica de negocio adicional.
 */
public abstract class AbstractService<T extends EntidadBase, E, F extends AbstractFacade<T, E>> {

    protected abstract F getFacade();

    public T create(T entity) {
        return getFacade().create(entity);
    }

    public T edit(T entity) {
        return getFacade().edit(entity);
    }

    public T delete(T entity) {
        return getFacade().delete(entity);
    }

    public void remove(T entity) {
        getFacade().remove(entity);
    }

    public T find(E id) {
        return getFacade().find(id);
    }

    public List<T> findAll() {
        return getFacade().findAll();
    }

    public List<T> findRange(int[] range) {
        return getFacade().findRange(range);
    }

    public int count() {
        return getFacade().count();
    }
}
