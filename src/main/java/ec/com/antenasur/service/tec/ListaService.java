package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ListaDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.tec.CandidatoFacade;
import ec.com.antenasur.facade.tec.ListaFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.AbstractService;
import ec.com.antenasur.util.Constantes;

@Stateless
public class ListaService extends AbstractService<Lista, Integer, ListaFacade> {

    @Inject
    private ListaFacade listaFacade;

    @Inject
    private CandidatoFacade candidatoFacade;

    @Inject
    private CategoriaVotoService categoriaVotoService;

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

    @Resource
    private SessionContext sessionContext;

    @Override
    protected ListaFacade getFacade() {
        return listaFacade;
    }

    public ListaDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return ListaDTO.fromEntity(listaFacade.find(id));
    }

    public List<ListaDTO> listarDTOs() {
        List<ListaDTO> resultado = new ArrayList<>();
        List<Lista> listas = listaFacade.findAll();
        if (listas == null) return resultado;
        for (Lista l : listas) resultado.add(ListaDTO.fromEntity(l));
        return resultado;
    }

    public List<ListaDTO> buscarPaginado(String termino, boolean incluirInactivas,
            int inicio, int limite, String campoOrden, boolean ascendente) {
        return mapear(listaFacade.buscarPaginado(termino, incluirInactivas,
                inicio, limite, campoOrden, ascendente));
    }

    public int contar(String termino, boolean incluirInactivas) {
        return listaFacade.contar(termino, incluirInactivas);
    }

    public ListaDTO guardarDesdeDTO(ListaDTO dto) {
        return guardarDesdeDTO(dto, procesoActivoId());
    }

    public ListaDTO guardarDesdeDTO(ListaDTO dto, Integer procesoId) {
        if (dto == null) {
            throw new NegocioException("No se pudo determinar la lista electoral a guardar.");
        }
        validarProceso(procesoId);
        normalizarYValidar(dto);
        List<Lista> coincidencias = listaFacade.buscarPorNombreONumeroIncluyendoInactivas(
                dto.getNombre(), dto.getNumero());
        if (dto.getId() == null) {
            Lista inactiva = null;
            for (Lista coincidencia : coincidencias) {
                if (Boolean.TRUE.equals(coincidencia.getEstado())) {
                    throw new NegocioException("Ya existe una lista activa con el mismo nombre o número.");
                }
                if (inactiva == null) {
                    inactiva = coincidencia;
                }
            }
            if (inactiva != null) {
                actualizarDatos(inactiva, dto);
                inactiva.setEstado(true);
                Lista persistida = listaFacade.edit(inactiva);
                sincronizarDespuesDePersistir(persistida, procesoId);
                return ListaDTO.fromEntity(persistida);
            }
            Lista persistida = listaFacade.create(dto.toEntity());
            sincronizarDespuesDePersistir(persistida, procesoId);
            return ListaDTO.fromEntity(persistida);
        }
        Lista actual = listaFacade.find(dto.getId());
        if (actual == null) {
            throw new NegocioException("La lista seleccionada ya no está disponible.");
        }
        for (Lista coincidencia : coincidencias) {
            if (!actual.getId().equals(coincidencia.getId()) && Boolean.TRUE.equals(coincidencia.getEstado())) {
                throw new NegocioException("Ya existe una lista activa con el mismo nombre o número.");
            }
        }
        actualizarDatos(actual, dto);
        Lista persistida = listaFacade.edit(actual);
        sincronizarDespuesDePersistir(persistida, procesoId);
        return ListaDTO.fromEntity(persistida);
    }

    public ListaDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        Lista l = listaFacade.find(id);
        if (l == null) return null;
        if (candidatoFacade.contarActivosPorLista(id) > 0) {
            throw new NegocioException("No se puede dar de baja la lista porque tiene candidatos activos.");
        }
        Lista eliminada = listaFacade.delete(l);
        try {
            categoriaVotoService.desactivarCategoriasLista(id);
        } catch (RuntimeException e) {
            marcarRollback();
            throw new NegocioException(Constantes.getMensaje("form.candidatos.lista.error.categoria.baja"));
        }
        return ListaDTO.fromEntity(eliminada);
    }

    public ListaDTO reactivarPorId(Integer id) {
        return reactivarPorId(id, procesoActivoId());
    }

    public ListaDTO reactivarPorId(Integer id, Integer procesoId) {
        validarProceso(procesoId);
        Lista lista = listaFacade.buscarPorIdIncluyendoInactivas(id);
        if (lista == null) {
            throw new NegocioException("La lista seleccionada no existe.");
        }
        if (Boolean.TRUE.equals(lista.getEstado())) {
            sincronizarDespuesDePersistir(lista, procesoId);
            return ListaDTO.fromEntity(lista);
        }
        for (Lista coincidencia : listaFacade.buscarPorNombreONumeroIncluyendoInactivas(
                lista.getNombre(), lista.getNumero())) {
            if (!lista.getId().equals(coincidencia.getId()) && Boolean.TRUE.equals(coincidencia.getEstado())) {
                throw new NegocioException("No se puede reactivar: ya existe una lista activa con el mismo nombre o número.");
            }
        }
        lista.setEstado(true);
        Lista persistida = listaFacade.edit(lista);
        sincronizarDespuesDePersistir(persistida, procesoId);
        return ListaDTO.fromEntity(persistida);
    }

    private List<ListaDTO> mapear(List<Lista> listas) {
        List<ListaDTO> resultado = new ArrayList<>();
        if (listas != null) {
            for (Lista lista : listas) {
                resultado.add(ListaDTO.fromEntity(lista));
            }
        }
        return resultado;
    }

    private void normalizarYValidar(ListaDTO dto) {
        dto.setNombre(normalizar(dto.getNombre()));
        dto.setSlogan(normalizar(dto.getSlogan()));
        dto.setNumero(normalizar(dto.getNumero()));
        if (dto.getNombre() == null || dto.getSlogan() == null || dto.getNumero() == null) {
            throw new NegocioException("Nombre, slogan y número de lista son obligatorios.");
        }
    }

    private void actualizarDatos(Lista lista, ListaDTO dto) {
        lista.setNombre(dto.getNombre());
        lista.setSlogan(dto.getSlogan());
        lista.setNumero(dto.getNumero());
    }

    private void sincronizarDespuesDePersistir(Lista lista, Integer procesoId) {
        try {
            categoriaVotoService.actualizarCategoriasLista(lista);
            categoriaVotoService.sincronizarLista(lista, procesoId);
        } catch (RuntimeException e) {
            marcarRollback();
            throw new NegocioException(Constantes.getMensaje("form.candidatos.lista.error.categoria.sync"));
        }
    }

    private Integer procesoActivoId() {
        ProcesoElectoral proceso = procesoElectoralFacade.getActivo();
        return proceso != null ? proceso.getId() : null;
    }

    private void validarProceso(Integer procesoId) {
        if (procesoId == null || procesoElectoralFacade.find(procesoId) == null) {
            throw new NegocioException(Constantes.getMensaje("form.candidatos.lista.error.proceso"));
        }
    }

    private void marcarRollback() {
        if (sessionContext != null) {
            sessionContext.setRollbackOnly();
        }
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim().toUpperCase();
    }
}
