package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.enums.EstadoTarea;
import ec.com.antenasur.facade.GeograpFacade;
import ec.com.antenasur.facade.tec.DocumentoFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.RecintoFacade;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.service.AbstractService;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.util.Constantes;

/**
 * Mesas electorales. Las lecturas quedan abiertas (@PermitAll): las usan el acta, la App, JRV,
 * reportes y la página pública de resultados. Las escrituras exigen Administrador o Tribunal en
 * el EJB; asignarResponsable también el Presidente de Mesa (el acta la usa al resolver su mesa).
 * WildFly niega los métodos sin anotar de un EJB con seguridad, por eso la anotación de clase.
 */
@Stateless
@PermitAll
@DeclareRoles({ MesaService.ROL_ADMINISTRADOR, MesaService.ROL_TRIBUNAL, MesaService.ROL_PRESIDENTE_MESA })
public class MesaService extends AbstractService<Mesa, Integer, MesaFacade> {

    static final String ROL_ADMINISTRADOR = "SITEC-Administrador";
    static final String ROL_TRIBUNAL = "SITEC-Tribunal";
    static final String ROL_PRESIDENTE_MESA = "SITEC-Presidente-mesa";

    @Override
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public Mesa create(Mesa entity) {
        return super.create(entity);
    }

    @Override
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public Mesa edit(Mesa entity) {
        return super.edit(entity);
    }

    @Override
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public Mesa delete(Mesa entity) {
        return super.delete(entity);
    }

    @Override
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public void remove(Mesa entity) {
        super.remove(entity);
    }

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private DocumentoFacade documentoFacade;

    @Inject
    private RecintoFacade recintoFacade;

    @Inject
    private GeograpFacade geograpFacade;

    @Inject
    private ec.com.antenasur.facade.tec.ProcesoElectoralFacade procesoElectoralFacade;

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

    public Map<Integer, Long> contarActivasPorRecinto() {
        return mesaFacade.contarActivasPorRecinto();
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

    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL, ROL_PRESIDENTE_MESA })
    public MesaDTO asignarResponsable(Integer mesaId, String responsable) {
        if (mesaId == null || responsable == null || responsable.isBlank()) {
            return null;
        }
        Mesa mesa = mesaFacade.find(mesaId);
        if (mesa == null) {
            return null;
        }
        if (!responsable.equals(mesa.getResponsable())) {
            mesa.setResponsable(responsable);
            mesa = mesaFacade.edit(mesa);
        }
        return MesaDTO.fromEntity(mesa);
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

    public List<MesaDTO> listarDTOsPorRecintoId(Integer recintoId) {
        return mapearLista(mesaFacade.listarPorRecinto(recintoId));
    }

    /** Datos planos para la selección y filtrado de mesas en pantallas JSF. */
    public List<MesaDTO> listarDTOsActivasConUbicacion() {
        return mapearLista(mesaFacade.listarActivasConUbicacion());
    }

    public List<MesaDTO> listarDTOsPorRecintoYProceso(Integer recintoId, Integer procesoId) {
        return mapearLista(mesaFacade.listarPorRecintoYProceso(recintoId, procesoId));
    }

    public MesaDTO obtenerDetalleDTOPorId(Integer mesaId) {
        return MesaDTO.fromEntity(mesaFacade.buscarDetallePorId(mesaId));
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
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
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
            // El responsable es el usuario del Presidente de Mesa: lo asigna JRV al completar la junta.
            nueva.setResponsable(null);
            return MesaDTO.fromEntity(mesaFacade.create(nueva));
        }
        Mesa actual = mesaFacade.find(dto.getId());
        if (actual == null) {
            return null;
        }
        // Recintos solo edita el nombre. El recinto no cambia desde aquí (la pantalla no lo ofrece y
        // movería el padrón a otra ubicación); el responsable lo administra JRV (de él depende que la
        // junta figure completada y el acceso del Presidente); estado, votos, ausentismo y
        // observación son del escrutinio y no deben pisarse con datos del diálogo.
        actual.setNombre(dto.getNombre());
        return MesaDTO.fromEntity(mesaFacade.edit(actual));
    }

    /**
     * Baja lógica de la mesa. Se rechaza si el proceso electoral activo la usa: con padrón
     * activo (sus electores quedarían sin mesa), con miembros de JRV designados o con un
     * escrutinio registrado (actas y resultados). Primero hay que retirar el padrón y la JRV;
     * una mesa con escrutinio no se elimina.
     */
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public MesaDTO eliminarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        Mesa m = mesaFacade.find(id);
        if (m == null) {
            return null;
        }
        validarSinUsoEnProcesoActivo(m);
        return MesaDTO.fromEntity(mesaFacade.delete(m));
    }

    private void validarSinUsoEnProcesoActivo(Mesa mesa) {
        ProcesoElectoral activo = procesoElectoralFacade.getActivo();
        if (activo == null) {
            return;
        }
        String nombre = mesa.getNombre() == null ? "" : mesa.getNombre();
        if (mesaFacade.contarEscrutinios(mesa.getId(), activo.getId()) > 0) {
            throw new NegocioException(Constantes.getMensaje("mesas.mensaje.eliminar.con.escrutinio", nombre));
        }
        if (mesaFacade.contarMiembrosJrvActivos(mesa.getId(), activo.getId()) > 0) {
            throw new NegocioException(Constantes.getMensaje("mesas.mensaje.eliminar.con.jrv", nombre));
        }
        long empadronados = mesaFacade.contarPadronActivo(mesa.getId(), activo.getId());
        if (empadronados > 0) {
            throw new NegocioException(Constantes.getMensaje("mesas.mensaje.eliminar.con.padron", nombre, empadronados));
        }
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
