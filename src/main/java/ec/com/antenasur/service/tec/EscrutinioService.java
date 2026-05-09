package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.enums.EstadoTarea;
import ec.com.antenasur.facade.tec.CategoriaVotoFacade;
import ec.com.antenasur.facade.tec.EscrutinioFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.PeriodoFacade;
import ec.com.antenasur.model.tec.CategoriaVoto;
import ec.com.antenasur.model.tec.Escrutinio;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.Periodo;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class EscrutinioService extends AbstractService<Escrutinio, Integer, EscrutinioFacade> {

    @Inject
    private EscrutinioFacade escrutinioFacade;

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private PeriodoFacade periodoFacade;

    @Inject
    private CategoriaVotoFacade categoriaVotoFacade;

    @Override
    protected EscrutinioFacade getFacade() {
        return escrutinioFacade;
    }

    public List<Escrutinio> buscaPorMesa(Mesa mesa) {
        return escrutinioFacade.buscaPorMesa(mesa);
    }

    public List<Escrutinio> buscaCanton(Mesa mesa) {
        return escrutinioFacade.buscaCanton(mesa);
    }

    /**
     * Devuelve el acta de escrutinios de la mesa: si ya existen registros, los
     * retorna; si no, construye una lista de Escrutinio "vacÃ­os" â€” uno por
     * categorÃ­a de voto â€” listos para que el operador ingrese los totales.
     *
     * @param mesa mesa cuya acta se estÃ¡ abriendo (no null)
     * @param periodo perÃ­odo al que pertenecen los nuevos registros
     * @param categorias categorÃ­as de voto a usar para los placeholders
     * @return lista nunca null; vacÃ­a si {@code mesa} o {@code categorias} son null
     */
    public List<Escrutinio> prepararActaPorMesa(Mesa mesa, Periodo periodo, List<CategoriaVoto> categorias) {
        if (mesa == null) {
            return new ArrayList<>();
        }
        List<Escrutinio> existentes = escrutinioFacade.buscaPorMesa(mesa);
        if (existentes != null && !existentes.isEmpty()) {
            return existentes;
        }
        List<Escrutinio> placeholders = new ArrayList<>();
        if (categorias != null) {
            for (CategoriaVoto categoria : categorias) {
                Escrutinio nuevo = new Escrutinio();
                nuevo.setMesa(mesa);
                nuevo.setPeriodo(periodo);
                nuevo.setCategoria(categoria);
                placeholders.add(nuevo);
            }
        }
        return placeholders;
    }

    /**
     * Guarda el acta completa de una mesa en una sola transacciÃ³n:
     * persiste/actualiza cada Escrutinio, suma los votos para obtener el total
     * de papeletas usadas y actualiza la Mesa con su estado y observaciÃ³n de
     * cuadre. Si {@code totalVotos} de la mesa â‰  {@code totalPapeletasUso},
     * marca {@code tieneErrorConteo=true} y describe la diferencia. La mesa
     * queda en {@link EstadoTarea#COMPLETADO}.
     *
     * @param mesa mesa a cerrar (no null)
     * @param actaItems escrutinios a persistir (cada uno debe tener totalVotos)
     * @return la mesa persistida con sus campos calculados; null si los args
     *         son invÃ¡lidos
     */
    public Mesa guardarActaCompleta(Mesa mesa, List<Escrutinio> actaItems) {
        if (mesa == null || actaItems == null || actaItems.isEmpty()) {
            return null;
        }
        int totalPapeletasUso = 0;
        for (Escrutinio item : actaItems) {
            if (item.getId() != null) {
                escrutinioFacade.edit(item);
            } else {
                escrutinioFacade.create(item);
            }
            totalPapeletasUso += item.getTotalVotos();
        }
        mesa.setEstadoTarea(EstadoTarea.COMPLETADO);
        mesa.setTotalPapetelasUso(totalPapeletasUso);
        aplicarCuadreVotos(mesa, totalPapeletasUso);
        return mesaFacade.edit(mesa);
    }

    private void aplicarCuadreVotos(Mesa mesa, int totalPapeletasUso) {
        int totalVotos = mesa.getTotalVotos();
        if (totalVotos == totalPapeletasUso) {
            mesa.setTieneErrorConteo(false);
            mesa.setObservacion("");
        } else if (totalVotos > totalPapeletasUso) {
            mesa.setTieneErrorConteo(true);
            mesa.setObservacion((totalVotos - totalPapeletasUso) + " PAPELETAS FALTANTES");
        } else {
            mesa.setTieneErrorConteo(true);
            mesa.setObservacion((totalPapeletasUso - totalVotos) + " PAPELETAS EXCEDENTES");
        }
    }

    // ----- API basada en DTO -----

    public EscrutinioDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return EscrutinioDTO.fromEntity(escrutinioFacade.find(id));
    }

    public List<EscrutinioDTO> listarDTOsPorMesa(Integer mesaId) {
        if (mesaId == null) {
            return new ArrayList<>();
        }
        Mesa mesa = mesaFacade.find(mesaId);
        return mapearLista(escrutinioFacade.buscaPorMesa(mesa));
    }

    /**
     * VersiÃ³n DTO de {@link #prepararActaPorMesa(Mesa, Periodo, List)}: dado
     * un id de mesa, id de periodo e ids de categorÃ­as, devuelve la lista de
     * Escrutinio (existentes o placeholders).
     */
    public List<EscrutinioDTO> prepararActaPorMesaDTO(Integer mesaId, Integer periodoId, List<Integer> categoriaIds) {
        List<EscrutinioDTO> resultado = new ArrayList<>();
        if (mesaId == null) {
            return resultado;
        }
        Mesa mesa = mesaFacade.find(mesaId);
        Periodo periodo = (periodoId != null) ? periodoFacade.find(periodoId) : null;
        List<CategoriaVoto> categorias = new ArrayList<>();
        if (categoriaIds != null) {
            for (Integer cid : categoriaIds) {
                CategoriaVoto cat = categoriaVotoFacade.find(cid);
                if (cat != null) {
                    categorias.add(cat);
                }
            }
        }
        return mapearLista(prepararActaPorMesa(mesa, periodo, categorias));
    }

    /**
     * VersiÃ³n DTO de {@link #guardarActaCompleta(Mesa, List)}: recibe el id
     * de la mesa y los DTOs de los items del acta. Reconstruye los
     * {@link Escrutinio} hidratando relaciones, ejecuta el cierre atÃ³mico, y
     * retorna el {@link MesaDTO} actualizado o null si la mesa no existe.
     *
     * <p>Usado desde el controller del acta sin tocar entidades.
     */
    public ec.com.antenasur.dto.MesaDTO guardarActaCompletaDTO(Integer mesaId, List<EscrutinioDTO> items) {
        if (mesaId == null || items == null || items.isEmpty()) {
            return null;
        }
        Mesa mesa = mesaFacade.find(mesaId);
        if (mesa == null) {
            return null;
        }
        List<Escrutinio> entidades = new ArrayList<>();
        for (EscrutinioDTO dto : items) {
            Escrutinio e;
            if (dto.getId() != null) {
                e = escrutinioFacade.find(dto.getId());
                if (e == null) {
                    continue;
                }
                e.setTotalVotos(dto.getTotalVotos());
            } else {
                e = new Escrutinio();
                e.setMesa(mesa);
                e.setPeriodo((dto.getPeriodoId() != null) ? periodoFacade.find(dto.getPeriodoId()) : null);
                e.setCategoria((dto.getCategoriaId() != null) ? categoriaVotoFacade.find(dto.getCategoriaId()) : null);
                e.setTotalVotos(dto.getTotalVotos());
            }
            entidades.add(e);
        }
        Mesa mesaCerrada = guardarActaCompleta(mesa, entidades);
        return ec.com.antenasur.dto.MesaDTO.fromEntity(mesaCerrada);
    }

    private List<EscrutinioDTO> mapearLista(List<Escrutinio> escrutinios) {
        List<EscrutinioDTO> resultado = new ArrayList<>();
        if (escrutinios == null) {
            return resultado;
        }
        for (Escrutinio e : escrutinios) {
            resultado.add(EscrutinioDTO.fromEntity(e));
        }
        return resultado;
    }
}
