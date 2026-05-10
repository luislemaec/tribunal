package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.IglesiaAsignacionDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.GeograpFacade;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.facade.tec.DocumentoFacade;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.Usuario;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Stateless
@Slf4j
public class IglesiaService extends AbstractService<Iglesia, Integer, IglesiaFacade> {

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private DocumentoFacade documentoFacade;

    @Inject
    private GeograpFacade geograpFacade;

    @Inject
    private UsuarioFacade usuarioFacade;

    @Override
    protected IglesiaFacade getFacade() {
        return iglesiaFacade;
    }

    public List<Iglesia> getIglesiasPorParroquia(Geograp parroquia) {
        return iglesiaFacade.getIglesiasPorParroquia(parroquia);
    }

    public Iglesia getIglesiaPorDocumento(String documento) {
        return iglesiaFacade.getIglesiaPorDocumento(documento);
    }

    public List<Iglesia> obtieneIglesiasAsignadasPorIds(List<Integer> listaIdIglesias) {
        return iglesiaFacade.obtieneIglesiasAsignadasPorIds(listaIdIglesias);
    }

    public List<Iglesia> obtieneIglesiasPorAsignarPorIds(List<Integer> listaIdIglesias, List<Integer> listaIdParroquias) {
        return iglesiaFacade.obtieneIglesiasPorAsignarPorIds(listaIdIglesias, listaIdParroquias);
    }

    public List<Iglesia> getIglesiasPorParroquias(List<Geograp> parroquias) {
        return iglesiaFacade.getIglesiasPorParroquias(parroquias);
    }

    public Iglesia getIglesiaPorNombreNombreComunidadYUbicacion(Iglesia iglesiaTmp) {
        return iglesiaFacade.getIglesiaPorNombreNombreComunidadYUbicacion(iglesiaTmp);
    }

    public void marcarConTieneDocumentos(List<Iglesia> iglesias, Integer tipoDocumentoId) {
        if (iglesias == null || iglesias.isEmpty() || tipoDocumentoId == null) {
            return;
        }
        java.util.Set<Integer> idsConDocs = documentoFacade.getEntidadesIdsConDocumentos(tipoDocumentoId);
        for (Iglesia iglesia : iglesias) {
            iglesia.setTieneDocumentos(idsConDocs.contains(iglesia.getId()));
        }
    }

    // ----- API basada en DTO -----

    public IglesiaDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return IglesiaDTO.fromEntity(iglesiaFacade.find(id));
    }

    public List<IglesiaDTO> listarDTOs() {
        return mapearLista(iglesiaFacade.findAll());
    }

    public List<IglesiaDTO> listarDTOsPorParroquias(List<Geograp> parroquias) {
        if (parroquias == null || parroquias.isEmpty()) {
            return Collections.emptyList();
        }
        return mapearLista(iglesiaFacade.getIglesiasPorParroquias(parroquias));
    }

    public List<IglesiaDTO> listarDTOsPorParroquia(Geograp parroquia) {
        if (parroquia == null || parroquia.getId() == null) {
            return Collections.emptyList();
        }
        return mapearLista(iglesiaFacade.getIglesiasPorParroquia(parroquia));
    }

    public List<IglesiaDTO> listarDTOsConFlagDocumentos(Integer tipoDocumentoId) {
        List<Iglesia> iglesias = iglesiaFacade.findAll();
        marcarConTieneDocumentos(iglesias, tipoDocumentoId);
        return mapearLista(iglesias);
    }

    /**
     * Retorna true si ya existe otra iglesia con el mismo nombre, parroquia y comunidad.
     * La comparación es case-insensitive y descarta espacios extremos.
     * Al editar, excluye el propio registro vía {@code idExcluir}.
     */
    public boolean existeDuplicado(String nombre, Integer ubicacionId, String comunidad, Integer idExcluir) {
        String nombreN = normalizar(nombre);
        if (nombreN == null || ubicacionId == null) return false;
        Geograp ubicacion = geograpFacade.find(ubicacionId);
        if (ubicacion == null) return false;
        Iglesia tmp = new Iglesia();
        tmp.setNombre(nombreN);
        tmp.setComunidad(normalizar(comunidad));
        tmp.setUbicacion(ubicacion);
        Iglesia encontrada = iglesiaFacade.getIglesiaPorNombreNombreComunidadYUbicacion(tmp);
        if (encontrada == null) return false;
        return idExcluir == null || !encontrada.getId().equals(idExcluir);
    }

    /**
     * Persiste la iglesia descrita por el DTO aplicando normalización de strings
     * (trim + uppercase) antes de persistir.
     *
     * Reglas de negocio aplicadas:
     * - RUC real no puede repetirse en dos iglesias distintas.
     * - Código genérico existente se conserva en ediciones sin cambio de RUC.
     * - En edición, la versión del DTO debe coincidir con la de la BD para detectar
     *   ediciones concurrentes (lanzará {@link NegocioException} si hay conflicto).
     */
    public IglesiaDTO guardarDesdeDTO(IglesiaDTO dto) {
        if (dto == null) {
            return null;
        }
        Geograp ubicacion = (dto.getUbicacionId() != null)
                ? geograpFacade.find(dto.getUbicacionId()) : null;
        if (ubicacion == null) {
            log.warn("guardarDesdeDTO: ubicacionId={} no encontrado", dto.getUbicacionId());
            return null;
        }

        String nombre    = normalizar(dto.getNombre());
        String comunidad = normalizar(dto.getComunidad());

        if (dto.getId() == null) {
            // â”€â”€ NUEVO REGISTRO â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            String documento = resolverDocumento(dto.getDocumento());
            validarRucUnico(documento, null);
            Iglesia nueva = dto.toEntity();
            nueva.setNombre(nombre);
            nueva.setComunidad(comunidad);
            nueva.setDocumento(documento);
            nueva.setUbicacion(ubicacion);
            Iglesia creada = iglesiaFacade.create(nueva);
            return IglesiaDTO.fromEntity(iglesiaFacade.findConCanton(creada.getId()));
        }

        // â”€â”€ EDICIÓN â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Iglesia actual = iglesiaFacade.find(dto.getId());
        if (actual == null) {
            log.warn("guardarDesdeDTO: iglesia id={} no encontrada para edición", dto.getId());
            return null;
        }

        // Control de edición concurrente: versión del DTO debe coincidir con la BD
        if (dto.getVersion() != null && !dto.getVersion().equals(actual.getVersion())) {
            throw new NegocioException(
                "El registro fue modificado por otro usuario mientras lo editaba. "
                + "Cierre el diálogo, recargue los datos e intente nuevamente.");
        }

        // Preservar el código genérico existente — no re-generar en cada edición
        String documento;
        if (esDocumentoGenerico(dto.getDocumento())
                && dto.getDocumento().equals(actual.getDocumento())) {
            documento = actual.getDocumento();
        } else {
            documento = resolverDocumento(dto.getDocumento());
        }

        validarRucUnico(documento, dto.getId());

        actual.setNombre(nombre);
        actual.setComunidad(comunidad);
        actual.setDocumento(documento);
        actual.setTotalMiembros(dto.getTotalMiembros());
        actual.setUbicacion(ubicacion);
        iglesiaFacade.edit(actual);
        return IglesiaDTO.fromEntity(iglesiaFacade.findConCanton(actual.getId()));
    }

    /**
     * Valida que el RUC real no esté ya asignado a otra iglesia.
     * Los códigos genéricos no se validan aquí: la secuencia PostgreSQL
     * ({@code seq_iglesia_codigo_generico}) garantiza que cada llamada a
     * {@code nextval()} retorne un valor distinto y nunca reusado.
     */
    private void validarRucUnico(String documento, Integer idExcluir) {
        if (documento == null || esDocumentoGenerico(documento)) return;
        Iglesia existente = iglesiaFacade.getIglesiaPorDocumento(documento);
        if (existente != null && !existente.getId().equals(idExcluir)) {
            throw new NegocioException(
                "El RUC " + documento + " ya está registrado en la iglesia \"" + existente.getNombre() + "\".");
        }
    }

    /** Delega en {@link IglesiaFacade#generarDocumentoGenerico()}. */
    public String generarDocumentoGenerico() {
        return iglesiaFacade.generarDocumentoGenerico();
    }

    /**
     * Calcula el progreso de registro/actualización de iglesias dentro del
     * rango de fechas de la fase activa.
     *
     * @return array [total, procesadas, porcentaje]
     */
    public int[] calcularProgresoRegistro(Date desde, Date hasta) {
        int[] resultado = {0, 0, 0};
        if (desde == null || hasta == null) return resultado;
        int total = iglesiaFacade.count();
        if (total == 0) return resultado;
        long procesadas = iglesiaFacade.countActualizadasEnRango(desde, hasta);
        resultado[0] = total;
        resultado[1] = (int) procesadas;
        resultado[2] = (int) Math.round((procesadas * 100.0) / total);
        return resultado;
    }

    public IglesiaDTO eliminarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        Iglesia i = iglesiaFacade.find(id);
        if (i == null) {
            return null;
        }
        return IglesiaDTO.fromEntity(iglesiaFacade.delete(i));
    }

    public IglesiaDTO buscarDTOPorDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            return null;
        }
        return IglesiaDTO.fromEntity(iglesiaFacade.getIglesiaPorDocumento(documento.trim()));
    }

    public List<IglesiaDTO> listarDTOsPorAsignarPorIds(List<Integer> idsExcluir, List<Integer> idsParroquias) {
        return mapearLista(iglesiaFacade.obtieneIglesiasPorAsignarPorIds(idsExcluir, idsParroquias));
    }

    // ----- API para Asignación de Usuarios -----

    /**
     * Lista todas las iglesias activas combinadas con su Usuario IglesiaAdmin
     * (si lo tienen) para la pantalla de asignación. Hace una sola consulta
     * para iglesias y otra para todos los admins, evitando N+1.
     */
    public List<IglesiaAsignacionDTO> listarParaAsignacionUsuarios() {
        List<Iglesia> iglesias = iglesiaFacade.findAll();
        Map<Integer, Usuario> adminPorIglesia = construirMapaAdmins();
        List<IglesiaAsignacionDTO> resultado = new ArrayList<>();
        if (iglesias == null) {
            return resultado;
        }
        for (Iglesia ig : iglesias) {
            Usuario admin = (ig.getId() != null) ? adminPorIglesia.get(ig.getId()) : null;
            resultado.add(IglesiaAsignacionDTO.fromEntity(ig, admin));
        }
        return resultado;
    }

    /** Variante filtrada por parroquias para el filtro geográfico. */
    public List<IglesiaAsignacionDTO> listarParaAsignacionPorParroquias(List<Geograp> parroquias) {
        if (parroquias == null || parroquias.isEmpty()) {
            return Collections.emptyList();
        }
        List<Iglesia> iglesias = iglesiaFacade.getIglesiasPorParroquias(parroquias);
        Map<Integer, Usuario> adminPorIglesia = construirMapaAdmins();
        List<IglesiaAsignacionDTO> resultado = new ArrayList<>();
        if (iglesias == null) {
            return resultado;
        }
        for (Iglesia ig : iglesias) {
            Usuario admin = (ig.getId() != null) ? adminPorIglesia.get(ig.getId()) : null;
            resultado.add(IglesiaAsignacionDTO.fromEntity(ig, admin));
        }
        return resultado;
    }

    /** Variante filtrada por una sola parroquia. */
    public List<IglesiaAsignacionDTO> listarParaAsignacionPorParroquia(Geograp parroquia) {
        if (parroquia == null || parroquia.getId() == null) {
            return Collections.emptyList();
        }
        List<Iglesia> iglesias = iglesiaFacade.getIglesiasPorParroquia(parroquia);
        Map<Integer, Usuario> adminPorIglesia = construirMapaAdmins();
        List<IglesiaAsignacionDTO> resultado = new ArrayList<>();
        if (iglesias == null) {
            return resultado;
        }
        for (Iglesia ig : iglesias) {
            Usuario admin = (ig.getId() != null) ? adminPorIglesia.get(ig.getId()) : null;
            resultado.add(IglesiaAsignacionDTO.fromEntity(ig, admin));
        }
        return resultado;
    }

    /**
     * Calcula el progreso de la fase de asignación de usuarios.
     *
     * @return array {@code [total, conAdmin, porcentaje]}.
     */
    public int[] calcularProgresoAsignacionUsuarios() {
        int[] resultado = {0, 0, 0};
        int total = iglesiaFacade.count();
        if (total == 0) return resultado;
        int conAdmin = construirMapaAdmins().size();
        resultado[0] = total;
        resultado[1] = conAdmin;
        resultado[2] = (int) Math.round((conAdmin * 100.0) / total);
        return resultado;
    }

    private Map<Integer, Usuario> construirMapaAdmins() {
        Map<Integer, Usuario> mapa = new HashMap<>();
        List<Usuario> admins = usuarioFacade.findAllIglesiaAdmins();
        if (admins != null) {
            for (Usuario u : admins) {
                if (u.getIglesia() != null && u.getIglesia().getId() != null) {
                    mapa.put(u.getIglesia().getId(), u);
                }
            }
        }
        return mapa;
    }

    // ----- helpers privados -----

    /** Trim + uppercase; retorna null si el string resultante está vacío. */
    private static String normalizar(String s) {
        if (s == null) return null;
        String r = s.trim().toUpperCase();
        return r.isEmpty() ? null : r;
    }

    /**
     * Si el documento es genérico (preview asignado en el toggle), re-genera uno
     * nuevo dentro de la transacción activa para garantizar unicidad concurrente.
     */
    private String resolverDocumento(String documento) {
        if (esDocumentoGenerico(documento)) {
            return iglesiaFacade.generarDocumentoGenerico();
        }
        return documento != null ? documento.trim() : null;
    }

    private static boolean esDocumentoGenerico(String doc) {
        return doc != null && doc.startsWith("00");
    }

    private List<IglesiaDTO> mapearLista(List<Iglesia> iglesias) {
        if (iglesias == null || iglesias.isEmpty()) {
            return new ArrayList<>();
        }
        List<IglesiaDTO> resultado = new ArrayList<>(iglesias.size());
        for (Iglesia i : iglesias) {
            resultado.add(IglesiaDTO.fromEntity(i));
        }
        return resultado;
    }
}
