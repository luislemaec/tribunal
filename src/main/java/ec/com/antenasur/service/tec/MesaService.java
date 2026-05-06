package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.ResumenVotosDTO;
import ec.com.antenasur.enums.EstadoTarea;
import ec.com.antenasur.facade.GeograpFacade;
import ec.com.antenasur.facade.tec.DocumentoFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.RecintoFacade;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class MesaService extends AbstractService<Mesa, Integer, MesaFacade> {

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private DocumentoFacade documentoFacade;

    @Inject
    private RecintoFacade recintoFacade;

    @Inject
    private GeograpFacade geograpFacade;

    @Override
    protected MesaFacade getFacade() {
        return mesaFacade;
    }

    public Mesa buscaRecintoPorNombre(String nombreRecinto) {
        return mesaFacade.buscaRecintoPorNombre(nombreRecinto);
    }

    /** Suma totalVotos de todas las mesas activas en una sola query agregada. */
    public long sumTotalVotos() {
        return mesaFacade.sumTotalVotos();
    }

    public List<Mesa> getMesasPorParroquias(List<Geograp> parroquias) {
        return mesaFacade.getMesasPorParroquias(parroquias);
    }

    public List<Mesa> getMesasPorRecintos(List<Recinto> recintos) {
        return mesaFacade.getMesasPorRecintos(recintos);
    }

    public List<Mesa> getMesasEscrutadasPorRecintos(List<Recinto> recintos) {
        return mesaFacade.getMesasEscrutadasPorRecintos(recintos);
    }

    public List<Mesa> mesasEscrutadas(EstadoTarea estadoTarea) {
        return mesaFacade.mesasEscrutadas(estadoTarea);
    }

    public List<Mesa> getMesasPorRecinto(Recinto recinto) {
        return mesaFacade.getMesasPorRecinto(recinto);
    }

    public Mesa getMesaPorUsuario(String usuario) {
        return mesaFacade.getMesaPorUsuario(usuario);
    }

    public Mesa buscaPorNombreMesa(String nombreMesa) {
        return mesaFacade.buscaPorNombreMesa(nombreMesa);
    }

    // ----- API basada en DTO -----

    public MesaDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return MesaDTO.fromEntity(mesaFacade.find(id));
    }

    public List<MesaDTO> listarDTOs() {
        return mapearLista(mesaFacade.findAll());
    }

    public List<MesaDTO> listarDTOsPorRecintos(List<Recinto> recintos) {
        return mapearLista(mesaFacade.getMesasPorRecintos(recintos));
    }

    public List<MesaDTO> listarDTOsEscrutadas(EstadoTarea estadoTarea) {
        return mapearLista(mesaFacade.mesasEscrutadas(estadoTarea));
    }

    /**
     * Variante que rellena el flag {@code tieneDocumentos} en cada DTO antes
     * de retornar (idéntico patrón a {@code IglesiaService.listarDTOsConFlagDocumentos}).
     */
    public List<MesaDTO> listarDTOsConFlagDocumentos(Integer tipoDocumentoId) {
        List<Mesa> mesas = mesaFacade.findAll();
        marcarConTieneDocumentos(mesas, tipoDocumentoId);
        return mapearLista(mesas);
    }

    /**
     * Persiste la mesa descrita por el DTO. Resuelve {@code recinto} y
     * {@code ubicacion} a partir de los ids embebidos. Si el id es null crea,
     * si no hidrata la entidad existente con los campos del DTO.
     */
    public MesaDTO guardarDesdeDTO(MesaDTO dto) {
        if (dto == null) {
            return null;
        }
        Recinto recinto = (dto.getRecinto() != null && dto.getRecinto().getId() != null)
                ? recintoFacade.find(dto.getRecinto().getId()) : null;
        Geograp ubicacion = (dto.getUbicacionId() != null)
                ? geograpFacade.find(dto.getUbicacionId()) : null;

        if (dto.getId() == null) {
            Mesa nueva = dto.toEntity();
            nueva.setRecinto(recinto);
            nueva.setUbicacion(ubicacion);
            return MesaDTO.fromEntity(mesaFacade.create(nueva));
        }
        Mesa actual = mesaFacade.find(dto.getId());
        if (actual == null) {
            return null;
        }
        actual.setNombre(dto.getNombre());
        actual.setRecinto(recinto);
        actual.setUbicacion(ubicacion);
        actual.setTotalVotos(dto.getTotalVotos());
        actual.setResponsable(dto.getResponsable());
        return MesaDTO.fromEntity(mesaFacade.edit(actual));
    }

    public MesaDTO eliminarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        Mesa m = mesaFacade.find(id);
        if (m == null) {
            return null;
        }
        return MesaDTO.fromEntity(mesaFacade.delete(m));
    }

    public MesaDTO buscarDTOPorNombre(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            return null;
        }
        return MesaDTO.fromEntity(mesaFacade.buscaPorNombreMesa(nombre));
    }

    private List<MesaDTO> mapearLista(List<Mesa> mesas) {
        List<MesaDTO> resultado = new ArrayList<>();
        if (mesas == null) {
            return resultado;
        }
        for (Mesa m : mesas) {
            resultado.add(MesaDTO.fromEntity(m));
        }
        return resultado;
    }

    /**
     * Construye un resumen para los recintos dados: lista mesas activas, mesas
     * escrutadas (COMPLETADO), suma {@code totalVotos} de cada mesa y calcula
     * el porcentaje escrutado. Maneja correctamente el caso de listas vacías
     * (porcentaje 0, sin {@code ArithmeticException}).
     *
     * @param recintos recintos a filtrar; si null/vacío, retorna un resumen vacío
     */
    public ResumenVotosDTO calcularResumenVotos(List<Recinto> recintos) {
        ResumenVotosDTO resumen = new ResumenVotosDTO();
        if (recintos == null || recintos.isEmpty()) {
            return resumen;
        }
        List<Mesa> mesas = mesaFacade.getMesasPorRecintos(recintos);
        List<Mesa> mesasEscrutadas = mesaFacade.getMesasEscrutadasPorRecintos(recintos);
        resumen.setMesas(mesas != null ? mesas : Collections.<Mesa>emptyList());
        resumen.setMesasEscrutadas(mesasEscrutadas != null ? mesasEscrutadas : Collections.<Mesa>emptyList());

        int totalVotantes = 0;
        for (Mesa mesa : resumen.getMesas()) {
            totalVotantes += mesa.getTotalVotos();
        }
        resumen.setTotalVotantes(totalVotantes);

        if (!resumen.getMesas().isEmpty() && !resumen.getMesasEscrutadas().isEmpty()) {
            resumen.setPorcentajeMesasEscrutadas(
                    (resumen.getMesasEscrutadas().size() * 100f) / resumen.getMesas().size());
        }
        return resumen;
    }

    /**
     * Calcula el porcentaje de mesas escrutadas (estado COMPLETADO) sobre el
     * total de mesas activas. Retorna 0 si no hay mesas o si no hay
     * escrutadas. Trunca a entero (mantiene el comportamiento previo del
     * controller).
     */
    /**
     * Marca cada mesa con su flag {@code tieneDocumentos} consultando si
     * existe al menos un documento del tipo dado vinculado a su id. Modifica
     * la lista in-place; no persiste cambios. No-op si la lista es null o
     * vacía.
     */
    /**
     * Marca el flag {@code tieneDocumentos} en cada mesa con una sola query
     * agregada (igual optimización que {@code IglesiaService}). Reemplaza el
     * patrón N+1 anterior.
     */
    public void marcarConTieneDocumentos(List<Mesa> mesas, Integer tipoDocumentoId) {
        if (mesas == null || mesas.isEmpty() || tipoDocumentoId == null) {
            return;
        }
        java.util.Set<Integer> idsConDocs = documentoFacade.getEntidadesIdsConDocumentos(tipoDocumentoId);
        for (Mesa mesa : mesas) {
            mesa.setTieneDocumentos(idsConDocs.contains(mesa.getId()));
        }
    }

    public float calcularPorcentajeEscrutado() {
        List<Mesa> total = mesaFacade.findAll();
        List<Mesa> escrutadas = mesaFacade.mesasEscrutadas(EstadoTarea.COMPLETADO);
        if (total == null || total.isEmpty() || escrutadas == null || escrutadas.isEmpty()) {
            return 0f;
        }
        return (escrutadas.size() * 100f) / total.size();
    }
}
