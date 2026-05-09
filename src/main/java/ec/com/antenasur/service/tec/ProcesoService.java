package ec.com.antenasur.service.tec;

import java.util.Date;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.facade.tec.ProcesoFacade;
import ec.com.antenasur.model.tec.Proceso;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class ProcesoService extends AbstractService<Proceso, Integer, ProcesoFacade> {

    @Inject
    private ProcesoFacade procesoFacade;

    @Override
    protected ProcesoFacade getFacade() {
        return procesoFacade;
    }

    public List<Proceso> getProcesoPorUsuario(String usuario) {
        return procesoFacade.getProcesoPorUsuario(usuario);
    }

    /**
     * Acepta {@link java.util.Date} (lo que JSF/PrimeFaces y otros consumidores
     * usan habitualmente) y convierte internamente a {@link java.sql.Date} que
     * es lo que requiere el facade JPA.
     */
    public List<Proceso> getProcesoPorUsuario(Date fechaInicio, Date fechaFin, String usuario) {
        java.sql.Date sqlInicio = (fechaInicio != null) ? new java.sql.Date(fechaInicio.getTime()) : null;
        java.sql.Date sqlFin = (fechaFin != null) ? new java.sql.Date(fechaFin.getTime()) : null;
        return procesoFacade.getProcesoPorUsuario(sqlInicio, sqlFin, usuario);
    }
}
