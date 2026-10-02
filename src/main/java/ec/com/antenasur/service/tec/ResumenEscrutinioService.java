package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ResultadoCategoriaPublicaDTO;
import ec.com.antenasur.dto.ResumenEscrutinioDTO;
import ec.com.antenasur.facade.tec.EscrutinioCabeceraFacade;
import ec.com.antenasur.facade.tec.EscrutinioFacade;
import ec.com.antenasur.facade.tec.PadronFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.model.tec.ProcesoElectoral;

/**
 * Resumen de escrutinio de escrutinios.xhtml: solo el proceso electoral activo y solo mesas
 * cerradas con acta física VALIDADA (mismo criterio que el portal público). Todo se calcula con
 * consultas agregadas sobre el conjunto de mesas del filtro geográfico, sin recorrer mesa por mesa.
 */
@Stateless
public class ResumenEscrutinioService {

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

    @Inject
    private EscrutinioFacade escrutinioFacade;

    @Inject
    private EscrutinioCabeceraFacade escrutinioCabeceraFacade;

    @Inject
    private PadronFacade padronFacade;

    /** Ids de las mesas, entre {@code mesaIds}, cerradas y con acta física VALIDADA del proceso. */
    public List<Integer> mesasValidadas(Integer procesoId, List<Integer> mesaIds) {
        return escrutinioCabeceraFacade.listarMesasValidadasPorProceso(procesoId, mesaIds);
    }

    /**
     * @param recintos número de recintos del filtro
     * @param mesaIds  mesas del filtro geográfico (activas)
     */
    public ResumenEscrutinioDTO resumir(long recintos, List<Integer> mesaIds) {
        ResumenEscrutinioDTO resumen = new ResumenEscrutinioDTO();
        resumen.setRecintos(recintos);
        ProcesoElectoral proceso = procesoElectoralFacade.getActivo();
        if (proceso == null || proceso.getId() == null) {
            return resumen;
        }
        resumen.setProcesoNombre(proceso.getNombre());
        if (mesaIds == null || mesaIds.isEmpty()) {
            return resumen;
        }
        Integer procesoId = proceso.getId();

        Map<Integer, Long> empadronadosPorMesa = padronFacade.contarSufragantesPorMesas(procesoId, mesaIds);
        List<Integer> validadas = escrutinioCabeceraFacade.listarMesasValidadasPorProceso(procesoId, mesaIds);
        resumen.setMesasProceso(empadronadosPorMesa.size());
        resumen.setMesasEscrutadas(validadas.size());
        resumen.setEmpadronados(empadronadosPorMesa.values().stream().mapToLong(Long::longValue).sum());
        resumen.setEmpadronadosEscrutados(validadas.stream()
                .mapToLong(mesaId -> empadronadosPorMesa.getOrDefault(mesaId, 0L)).sum());

        List<ResultadoCategoriaPublicaDTO> categorias = validadas.isEmpty()
                ? new ArrayList<>()
                : escrutinioFacade.obtenerResultadosValidadosPorCategoria(procesoId, validadas);
        for (ResultadoCategoriaPublicaDTO categoria : categorias) {
            long votos = categoria.getTotalVotos() != null ? categoria.getTotalVotos() : 0L;
            String clase = EscrutinioService.clasificarCategoriaPublica(categoria.getTipo(), categoria.getCategoria());
            if (EscrutinioService.CLASE_LISTA.equals(clase)) {
                resumen.setVotosValidos(resumen.getVotosValidos() + votos);
            } else if (EscrutinioService.CLASE_BLANCOS.equals(clase)) {
                resumen.setVotosBlancos(resumen.getVotosBlancos() + votos);
            } else if (EscrutinioService.CLASE_NULOS.equals(clase)) {
                resumen.setVotosNulos(resumen.getVotosNulos() + votos);
            }
        }
        resumen.setSufragantes(resumen.getVotosValidos() + resumen.getVotosBlancos() + resumen.getVotosNulos());

        // Ranking: la lista con más votos primero; a igualdad, el orden de la categoría.
        List<ResultadoCategoriaPublicaDTO> listas = EscrutinioService.soloListas(categorias);
        listas.sort(Comparator.comparing(ResultadoCategoriaPublicaDTO::getTotalVotos, Comparator.reverseOrder())
                .thenComparing(ResultadoCategoriaPublicaDTO::getOrden, Comparator.nullsLast(Comparator.naturalOrder())));
        resumen.setListas(listas);
        return resumen;
    }
}
