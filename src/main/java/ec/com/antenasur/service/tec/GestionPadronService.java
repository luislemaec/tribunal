package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import ec.com.antenasur.dto.FiltroPadronDTO;
import ec.com.antenasur.dto.FilaPadronDTO;
import ec.com.antenasur.dto.MesaPadronDTO;
import ec.com.antenasur.dto.OpcionPadronDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.tec.GestionPadronFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.itext.ReporteXLSX;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.Padron;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.util.Constantes;

/** Operaciones atomicas de gestion; consultas y reportes no dependen del estado de JSF. */
@Stateless
public class GestionPadronService {
    @Inject private GestionPadronFacade facade;
    @Inject private MesaFacade mesaFacade;

    public List<ec.com.antenasur.dto.IglesiaPadronDTO> resumenIglesias(Integer mesa, Integer proceso, boolean asignadas) {
        return mesa == null || proceso == null ? List.of() : facade.resumenIglesias(mesa, proceso, asignadas);
    }

    public List<OpcionPadronDTO> geografia(Integer padre) { return facade.geografia(padre); }
    public List<OpcionPadronDTO> recintos(FiltroPadronDTO f) { return facade.recintos(f); }
    public List<OpcionPadronDTO> mesas(Integer recinto) { return facade.opcionesMesas(recinto); }
    public List<OpcionPadronDTO> iglesias(Integer mesa, Integer proceso) { return facade.iglesias(mesa, proceso); }

    public List<FilaPadronDTO> listar(FiltroPadronDTO f, boolean disponibles, int primero, int cantidad,
            String orden, boolean descendente) {
        if (f.getProcesoId() == null || (disponibles && (f.getIglesiaId() == null || f.getMesaId() == null))) return List.of();
        return facade.listar(f, disponibles, primero, Math.min(cantidad, 100), orden, descendente, null);
    }
    public int contar(FiltroPadronDTO f, boolean disponibles) {
        if (f.getProcesoId() == null || (disponibles && (f.getIglesiaId() == null || f.getMesaId() == null))) return 0;
        return Math.toIntExact(facade.contar(f, disponibles));
    }
    public List<MesaPadronDTO> listarMesas(FiltroPadronDTO f, int primero, int cantidad) {
        return f.getProcesoId() == null ? List.of() : facade.listarMesas(f, primero, Math.min(cantidad, 100));
    }
    public List<MesaPadronDTO> listarMesas(FiltroPadronDTO f, int primero, int cantidad, String campo, boolean descendente) {
        return f.getProcesoId() == null ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(facade.listarMesas(f, primero, Math.min(cantidad, 100), campo, descendente));
    }
    public int contarMesas(FiltroPadronDTO f) {
        return f.getProcesoId() == null ? 0 : Math.toIntExact(facade.contarMesas(f, f.getConPadron()));
    }
    public long[] resumen(FiltroPadronDTO f) {
        if (f.getProcesoId() == null) return new long[3];
        return new long[]{facade.contar(f, false), facade.contarMesas(f, true), facade.contarMesas(f, false)};
    }

    private ProcesoElectoral validarContexto(Integer procesoId, Integer recintoId, Integer mesaId) {
        if (procesoId == null || recintoId == null || mesaId == null) error("seleccion");
        ProcesoElectoral proceso = facade.bloquearProceso(procesoId);
        if (proceso == null || !Boolean.TRUE.equals(proceso.getEstado()) || !Boolean.TRUE.equals(proceso.getActivo())) error("proceso.inactivo");
        Mesa mesa = mesaFacade.buscarDetallePorId(mesaId);
        if (mesa == null || !Boolean.TRUE.equals(mesa.getEstado()) || mesa.getRecinto() == null
                || !Boolean.TRUE.equals(mesa.getRecinto().getEstado()) || !recintoId.equals(mesa.getRecinto().getId())) error("seleccion");
        if (facade.escrutinioIniciado(mesaId, procesoId)) error("escrutinio");
        return proceso;
    }

    public int asignarIglesia(Integer proceso, Integer recinto, Integer mesa, Integer iglesia) {
        validarContexto(proceso, recinto, mesa);
        if (iglesia == null) error("seleccion");
        if (facade.iglesiaEnOtraMesa(iglesia, mesa, proceso)) error("iglesia.otra.mesa");
        List<Integer> ids = facade.habilitadosPendientes(iglesia, mesa, proceso);
        if (ids.isEmpty()) error("iglesia.sin.pendientes");
        return asignar(proceso, recinto, mesa, iglesia, ids).size();
    }

    public int retirarIglesia(Integer proceso, Integer recinto, Integer mesa, Integer iglesia) {
        validarContexto(proceso, recinto, mesa);
        if (iglesia == null) error("seleccion");
        // Incluye todos los empadronados, aunque posteriormente hayan dejado de estar habilitados.
        List<Integer> ids = facade.empadronadosIglesia(iglesia, mesa, proceso);
        if (ids.isEmpty()) error("iglesia.sin.padron");
        return retirar(proceso, recinto, mesa, ids);
    }

    public List<Padron> asignar(Integer procesoId, Integer recintoId, Integer mesaId, Integer iglesiaId, List<Integer> ids) {
        ProcesoElectoral proceso = validarContexto(procesoId, recintoId, mesaId);
        validarIds(ids);
        if (iglesiaId == null) error("seleccion");
        Mesa mesa = mesaFacade.buscarDetallePorId(mesaId);
        List<IglesiaPersona> miembros = facade.miembros(ids);
        if (miembros.size() != ids.size()) error("miembro.invalido");
        for (IglesiaPersona ip : miembros) {
            if (!Boolean.TRUE.equals(ip.getEstado()) || !Boolean.TRUE.equals(ip.getHabilitadoPadron())
                    || ip.getPersona() == null || !Boolean.TRUE.equals(ip.getPersona().getEstado())
                    || ip.getIglesia() == null || !Boolean.TRUE.equals(ip.getIglesia().getEstado())
                    || !iglesiaId.equals(ip.getIglesia().getId())
                    || ip.getIglesia().getUbicacion() == null || mesa.getRecinto().getUbicacion() == null
                    || !ip.getIglesia().getUbicacion().getId().equals(mesa.getRecinto().getUbicacion().getId())) error("miembro.invalido");
        }
        List<Integer> personas = miembros.stream().map(ip -> ip.getPersona().getId()).distinct().toList();
        if (personas.size() != miembros.size()) error("duplicado");
        if (facade.iglesiaEnOtraMesa(iglesiaId, mesaId, procesoId)) error("iglesia.otra.mesa");
        List<Padron> existentes = facade.existentes(procesoId, personas);
        if (existentes.stream().anyMatch(p -> Boolean.TRUE.equals(p.getEstado()))) error("duplicado");
        Map<Integer, Padron> porVinculo = existentes.stream().collect(Collectors.toMap(
                p -> p.getIglesiaPersona().getId(), Function.identity(), (a, b) -> a));
        // Validar todo antes de cambiar entidades: NegocioException no solicita rollback por defecto.
        if (existentes.stream().anyMatch(p -> Boolean.TRUE.equals(p.getSufrago()))) error("sufrago");
        List<Padron> registros = new ArrayList<>();
        for (IglesiaPersona ip : miembros) {
            Padron p = porVinculo.get(ip.getId());
            if (p == null) p = new Padron(mesa, proceso, ip);
            else { p.setMesa(mesa); p.setEstado(true); }
            registros.add(p);
        }
        facade.guardar(registros);
        return registros;
    }

    public int retirar(Integer proceso, Integer recinto, Integer mesa, List<Integer> ids) {
        validarContexto(proceso, recinto, mesa);
        validarIds(ids);
        List<Padron> padrones = facade.seleccion(ids, mesa, proceso);
        if (padrones.size() != ids.size()) error("seleccion.obsoleta");
        if (padrones.stream().anyMatch(p -> Boolean.TRUE.equals(p.getSufrago()))) error("sufrago");
        if (facade.tieneJrv(padrones.stream().map(p -> p.getIglesiaPersona().getId()).toList(), proceso)) error("jrv");
        for (Padron p : padrones) p.setEstado(false);
        facade.guardar(padrones);
        return padrones.size();
    }

    private void validarIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(java.util.Objects::isNull)
                || ids.stream().distinct().count() != ids.size()) error("seleccion.personas");
    }

    public byte[] reporte(FiltroPadronDTO filtro) {
        if (filtro == null || filtro.getProcesoId() == null) {
            error("proceso.sin.activo");
        }
        ProcesoElectoral activo = facade.bloquearProceso(filtro.getProcesoId());
        if (activo == null || !Boolean.TRUE.equals(activo.getEstado()) || !Boolean.TRUE.equals(activo.getActivo())) {
            error("proceso.sin.activo");
        }
        if (facade.contar(filtro, false) == 0) error("reporte.vacio");
        try {
            return ReporteXLSX.generarPadronGeneral(consumir -> {
                int ultimo = 0;
                List<FilaPadronDTO> lote;
                do {
                    lote = facade.listar(filtro, false, 0, 500, null, false, ultimo);
                    for (FilaPadronDTO fila : lote) { consumir.accept(fila); ultimo = fila.getId(); }
                } while (lote.size() == 500);
            });
        } catch (java.io.IOException e) {
            throw new NegocioException(Constantes.getMensaje("gestionPadron.error.reporte"));
        }
    }

    private static void error(String clave) { throw new NegocioException(Constantes.getMensaje("gestionPadron.error." + clave)); }
}
