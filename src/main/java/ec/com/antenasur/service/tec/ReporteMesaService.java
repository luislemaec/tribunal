package ec.com.antenasur.service.tec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.CategoriaVotoDTO;
import ec.com.antenasur.dto.DocumentoDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.PadronDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.dto.ReporteMesaDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.tec.DocumentoFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.facade.tec.TipoDocumentoFacade;
import ec.com.antenasur.itext.ReportePFD;
import ec.com.antenasur.itext.ReporteXLSX;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.TipoDocumento;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.RepositorioDocumentos;

/** Reglas de consulta y generación documental por proceso, recinto y mesa. */
@Stateless
public class ReporteMesaService {

    @Inject private ProcesoElectoralService procesoService;
    @Inject private ProcesoElectoralFacade procesoFacade;
    @Inject private RecintoService recintoService;
    @Inject private MesaService mesaService;
    @Inject private MesaFacade mesaFacade;
    @Inject private EscrutinioService escrutinioService;
    @Inject private CategoriaVotoService categoriaVotoService;
    @Inject private PadronService padronService;
    @Inject private MiembroJRVService miembroJrvService;
    @Inject private DocumentoService documentoService;
    @Inject private DocumentoFacade documentoFacade;
    @Inject private TipoDocumentoFacade tipoDocumentoFacade;

    @Resource
    private SessionContext sessionContext;

    public List<ProcesoElectoralDTO> listarProcesos() {
        return procesoService.listarDTOs();
    }

    public List<RecintoDTO> listarRecintos(Integer procesoId, Integer personaId, boolean presidenteRestringido) {
        if (procesoId == null) {
            return Collections.emptyList();
        }
        if (presidenteRestringido) {
            MiembroJRVDTO designacion = obtenerDesignacionPresidente(personaId, procesoId);
            if (designacion == null || designacion.getMesa() == null
                    || designacion.getMesa().getRecinto() == null) {
                return Collections.emptyList();
            }
            return List.of(designacion.getMesa().getRecinto());
        }
        return recintoService.listarDTOsPorProceso(procesoId);
    }

    public List<MesaDTO> listarMesas(Integer procesoId, Integer recintoId,
            Integer personaId, boolean presidenteRestringido) {
        if (procesoId == null || recintoId == null) {
            return Collections.emptyList();
        }
        if (presidenteRestringido) {
            MiembroJRVDTO designacion = obtenerDesignacionPresidente(personaId, procesoId);
            if (designacion == null || designacion.getMesa() == null
                    || designacion.getMesa().getRecinto() == null
                    || !recintoId.equals(designacion.getMesa().getRecinto().getId())) {
                return Collections.emptyList();
            }
            return List.of(designacion.getMesa());
        }
        return mesaService.listarDTOsPorRecintoYProceso(recintoId, procesoId);
    }

    public ReporteMesaDTO consultar(Integer procesoId, Integer recintoId, Integer mesaId,
            Integer personaId, boolean presidenteRestringido) {
        ProcesoElectoral proceso = procesoId != null ? procesoFacade.find(procesoId) : null;
        Mesa mesa = mesaId != null ? mesaFacade.buscarDetallePorId(mesaId) : null;
        validarSeleccion(proceso, recintoId, mesa);
        validarAcceso(mesaId, procesoId, personaId, presidenteRestringido);

        ReporteMesaDTO reporte = new ReporteMesaDTO();
        reporte.setProceso(ProcesoElectoralDTO.fromEntity(proceso));
        MesaDTO mesaDto = MesaDTO.fromEntity(mesa);
        reporte.setMesa(mesaDto);
        reporte.setRecinto(RecintoDTO.fromEntity(mesa.getRecinto()));
        reporte.setCabecera(escrutinioService.buscarCabeceraDTO(mesaId, procesoId));
        reporte.setEscrutinios(listarResultadosCompletos(mesaId, procesoId, mesaDto));
        reporte.setPadron(padronService.listarDTOsPorMesaIdsYProceso(List.of(mesaId), procesoId));
        reporte.setMiembrosJrv(miembroJrvService.listarDTOsPorMesaProceso(mesaId, procesoId));
        reporte.setDocumentos(documentosDisponibles(mesaId, procesoId));
        return reporte;
    }

    public DocumentoDTO generarActaParcial(Integer procesoId, Integer recintoId, Integer mesaId,
            Integer personaId, boolean presidenteRestringido) {
        ReporteMesaDTO reporte = consultar(procesoId, recintoId, mesaId, personaId, presidenteRestringido);
        if (reporte.getCabecera() == null) {
            throw new NegocioException(mensaje("reportesMesa.error.sin.escrutinio"));
        }
        String contexto = hashContextoActa(reporte);
        TipoDocumento tipo = obtenerTipo(Constantes.TIPO_ACTA_PARCIAL_ESCRUTINIO);
        Documentos existente = buscarDocumentoVigente(mesaId, tipo.getId(), contexto);
        if (existente != null) {
            return toDocumentoDisponible(existente);
        }

        LocalDateTime ahora = LocalDateTime.now();
        String codigo = "ACTA-PARCIAL-" + procesoId + "-M" + mesaId + "-"
                + ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + sufijo();
        byte[] contenido;
        try {
            contenido = ReportePFD.generarActaParcial(reporte, codigo, ahora, usuarioActual());
        } catch (Exception e) {
            throw new NegocioException(mensaje("reportesMesa.error.generar.acta"));
        }
        return almacenar(reporte, tipo, codigo, ".pdf", "application/pdf", contenido,
                "actas-escrutinio/parciales", contexto);
    }

    public DocumentoDTO generarPadron(Integer procesoId, Integer recintoId, Integer mesaId,
            Integer personaId, boolean presidenteRestringido) {
        ReporteMesaDTO reporte = consultar(procesoId, recintoId, mesaId, personaId, presidenteRestringido);
        if (!Boolean.TRUE.equals(reporte.getProceso().getActivo())) {
            throw new NegocioException(mensaje("reportesMesa.error.padron.proceso.inactivo"));
        }
        if (reporte.getPadron().isEmpty()) {
            throw new NegocioException(mensaje("reportesMesa.error.sin.padron"));
        }
        String contexto = hashContextoPadron(reporte);
        TipoDocumento tipo = obtenerTipo(Constantes.TIPO_PADRON_ELECTORAL_MESA);
        Documentos existente = buscarDocumentoVigente(mesaId, tipo.getId(), contexto);
        if (existente != null) {
            return toDocumentoDisponible(existente);
        }

        LocalDateTime ahora = LocalDateTime.now();
        String codigo = "PADRON-" + procesoId + "-M" + mesaId + "-"
                + ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + sufijo();
        byte[] contenido = generarExcelPadron(reporte);
        return almacenar(reporte, tipo, codigo, ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", contenido,
                "padrones-mesa", contexto);
    }

    private List<EscrutinioDTO> listarResultadosCompletos(Integer mesaId, Integer procesoId, MesaDTO mesa) {
        List<EscrutinioDTO> existentes = escrutinioService.listarDTOsPorMesaYProceso(mesaId, procesoId);
        Map<Integer, EscrutinioDTO> porCategoria = new HashMap<>();
        for (EscrutinioDTO item : existentes) {
            porCategoria.put(item.getCategoriaId(), item);
        }
        List<EscrutinioDTO> resultado = new ArrayList<>();
        for (CategoriaVotoDTO categoria : categoriaVotoService.listarDTOsOrdenados(procesoId)) {
            EscrutinioDTO item = porCategoria.get(categoria.getId());
            if (item == null) {
                item = new EscrutinioDTO();
                item.setMesa(mesa);
                item.setProcesoId(procesoId);
                item.setCategoriaId(categoria.getId());
                item.setCategoriaNombre(categoria.getNombre());
                item.setTotalVotos(0);
            }
            resultado.add(item);
            porCategoria.remove(categoria.getId());
        }
        // Conserva categorías legacy que ya forman parte de un escrutinio histórico.
        for (EscrutinioDTO item : existentes) {
            if (porCategoria.containsKey(item.getCategoriaId())) {
                resultado.add(item);
            }
        }
        return resultado;
    }

    private List<DocumentoDTO> documentosDisponibles(Integer mesaId, Integer procesoId) {
        List<DocumentoDTO> documentos = documentoService.listarDTOsPorMesaProceso(mesaId, procesoId);
        for (DocumentoDTO documento : documentos) {
            documento.setDisponible(RepositorioDocumentos.estaDisponible(documento.getPath()));
        }
        return documentos;
    }

    private byte[] generarExcelPadron(ReporteMesaDTO reporte) {
        synchronized (ReporteXLSX.class) {
            try {
                ReporteXLSX.nuevoExcel(mensaje("reportesMesa.padron.titulo"));
                LocalDateTime ahora = LocalDateTime.now();
                ReporteXLSX.creaEspacioInformativoPadron(
                        ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss")), usuarioActual(),
                        reporte.getProceso().getNombre(), reporte.getRecinto().getProvinciaNombre(),
                        reporte.getRecinto().getCantonNombre(), reporte.getRecinto().getUbicacionNombre(),
                        reporte.getRecinto().getNombre(), reporte.getMesa().getNombre());
                String[] columnas = {
                    mensaje("reportesMesa.padron.numero"), mensaje("reportesMesa.padron.cedula"),
                    mensaje("reportesMesa.padron.nombres"), mensaje("reportesMesa.padron.iglesia"),
                    mensaje("reportesMesa.padron.comunidad"), mensaje("reportesMesa.padron.estado")
                };
                ReporteXLSX.creaCabeceraTabla(columnas, new int[]{1800, 5000, 9000, 10000, 8000, 4000});
                String[][] datos = new String[reporte.getPadron().size()][columnas.length];
                for (int i = 0; i < reporte.getPadron().size(); i++) {
                    completarFilaPadron(datos[i], reporte.getPadron().get(i), i + 1);
                }
                ReporteXLSX.creaContenidoTabla(datos, columnas);
                ReporteXLSX.setFinalParagraph(reporte.getPadron().size());
                return ReporteXLSX.obtenerContenidoExcel();
            } catch (Exception e) {
                throw new NegocioException(mensaje("reportesMesa.error.generar.padron"));
            }
        }
    }

    private void completarFilaPadron(String[] fila, PadronDTO padron, int numero) {
        fila[0] = String.valueOf(numero);
        if (padron.getIglesiaPersona() != null) {
            if (padron.getIglesiaPersona().getPersona() != null) {
                fila[1] = texto(padron.getIglesiaPersona().getPersona().getDocumento());
                fila[2] = (texto(padron.getIglesiaPersona().getPersona().getNombres()) + " "
                        + texto(padron.getIglesiaPersona().getPersona().getApellidos())).trim();
            }
            if (padron.getIglesiaPersona().getIglesia() != null) {
                fila[3] = texto(padron.getIglesiaPersona().getIglesia().getNombre());
                fila[4] = texto(padron.getIglesiaPersona().getIglesia().getComunidad());
            }
        }
        fila[5] = Boolean.TRUE.equals(padron.getSufrago())
                ? mensaje("reportesMesa.padron.sufrago") : mensaje("reportesMesa.padron.pendiente");
        for (int i = 0; i < fila.length; i++) {
            if (fila[i] == null) {
                fila[i] = "";
            }
        }
    }

    private DocumentoDTO almacenar(ReporteMesaDTO reporte, TipoDocumento tipo, String codigo,
            String extension, String mime, byte[] contenido, String subdirectorio, String contexto) {
        Path archivo = null;
        try {
            archivo = RepositorioDocumentos.escribirAtomico(subdirectorio, codigo + extension, contenido);
            Documentos documento = new Documentos(codigo,
                    RepositorioDocumentos.rutaRelativaParaPersistir(archivo), tipo,
                    reporte.getMesa().getId(), extension, mime, codigo);
            documento.setProceso(procesoFacade.find(reporte.getProceso().getId()));
            documento.setRecinto(mesaFacade.buscarDetallePorId(reporte.getMesa().getId()).getRecinto());
            documento.setMesa(mesaFacade.find(reporte.getMesa().getId()));
            documento.setContextoHash(contexto);
            documento.setHashSha256(RepositorioDocumentos.sha256(contenido));
            Documentos persistido = documentoFacade.create(documento);
            if (persistido == null || persistido.getId() == null) {
                throw new IOException("No se registro la metadata del documento.");
            }
            return toDocumentoDisponible(persistido);
        } catch (Exception e) {
            RepositorioDocumentos.eliminarSilencioso(archivo);
            throw new NegocioException(mensaje("reportesMesa.error.almacenar"));
        }
    }

    private Documentos buscarDocumentoVigente(Integer mesaId, Integer tipoId, String contexto) {
        Documentos existente = documentoFacade.buscarActivoPorEntidadTipoYContexto(mesaId, tipoId, contexto);
        if (existente == null) {
            return null;
        }
        if (RepositorioDocumentos.estaDisponible(existente.getPath())) {
            return existente;
        }
        documentoFacade.delete(existente);
        return null;
    }

    private DocumentoDTO toDocumentoDisponible(Documentos documento) {
        DocumentoDTO dto = DocumentoDTO.fromEntity(documento);
        dto.setDisponible(RepositorioDocumentos.estaDisponible(documento.getPath()));
        return dto;
    }

    private MiembroJRVDTO obtenerDesignacionPresidente(Integer personaId, Integer procesoId) {
        return miembroJrvService.obtenerDesignacionPresidentePorPersonaProceso(personaId, procesoId);
    }

    private void validarAcceso(Integer mesaId, Integer procesoId, Integer personaId,
            boolean presidenteRestringido) {
        if (!presidenteRestringido) {
            return;
        }
        MiembroJRVDTO designacion = obtenerDesignacionPresidente(personaId, procesoId);
        if (designacion == null || designacion.getMesa() == null
                || !mesaId.equals(designacion.getMesa().getId())) {
            throw new NegocioException(mensaje("reportesMesa.error.mesa.no.autorizada"));
        }
    }

    private void validarSeleccion(ProcesoElectoral proceso, Integer recintoId, Mesa mesa) {
        if (proceso == null || recintoId == null || mesa == null || mesa.getRecinto() == null
                || !recintoId.equals(mesa.getRecinto().getId())) {
            throw new NegocioException(mensaje("reportesMesa.error.seleccion"));
        }
    }

    private TipoDocumento obtenerTipo(String nombre) {
        TipoDocumento tipo = tipoDocumentoFacade.buscarActivoPorNombre(nombre);
        if (tipo == null) {
            throw new NegocioException(mensaje("reportesMesa.error.tipo.documento"));
        }
        return tipo;
    }

    private String hashContextoActa(ReporteMesaDTO reporte) {
        StringBuilder fuente = contextoBase(reporte);
        for (EscrutinioDTO item : reporte.getEscrutinios()) {
            fuente.append("|E:").append(item.getCategoriaId()).append(':').append(item.getTotalVotos());
        }
        for (MiembroJRVDTO miembro : reporte.getMiembrosJrv()) {
            fuente.append("|J:").append(miembro.getId()).append(':').append(miembro.getCargoId());
        }
        return hash(fuente.toString());
    }

    private String hashContextoPadron(ReporteMesaDTO reporte) {
        StringBuilder fuente = contextoBase(reporte);
        for (PadronDTO padron : reporte.getPadron()) {
            fuente.append("|P:").append(padron.getId()).append(':').append(padron.getSufrago());
        }
        return hash(fuente.toString());
    }

    private StringBuilder contextoBase(ReporteMesaDTO reporte) {
        return new StringBuilder().append(reporte.getProceso().getId()).append('|')
                .append(reporte.getRecinto().getId()).append('|').append(reporte.getMesa().getId());
    }

    private String hash(String fuente) {
        try {
            return RepositorioDocumentos.sha256(fuente.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new NegocioException(mensaje("reportesMesa.error.almacenar"));
        }
    }

    private String usuarioActual() {
        try {
            String usuario = sessionContext.getCallerPrincipal().getName();
            return usuario != null && !usuario.isBlank() ? usuario : "<desconocido>";
        } catch (Exception e) {
            return "<desconocido>";
        }
    }

    private String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String mensaje(String clave) {
        return Constantes.getMensaje(clave);
    }

    private String texto(String valor) {
        return valor != null ? valor : "";
    }
}
