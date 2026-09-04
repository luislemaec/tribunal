package ec.com.antenasur.service.tec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.EstadoActaActualizacionDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.TribunalDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.tec.DocumentoFacade;
import ec.com.antenasur.facade.tec.TipoDocumentoFacade;
import ec.com.antenasur.itext.ActaActualizacionMiembrosPdf;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.TipoDocumento;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.IglesiaService;
import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.RepositorioDocumentos;

/** Reglas de generacion y custodia del acta de actualizacion por iglesia. */
@Stateless
@DeclareRoles("SITEC-IglesiaAdmin")
@RolesAllowed("SITEC-IglesiaAdmin")
public class ActaActualizacionMiembrosService {

    private static final int CARGO_PRESIDENTE_TRIBUNAL = 3;
    private static final int CARGO_SECRETARIO_TRIBUNAL = 5;
    private static final String PROPIEDAD_SECRETO_QR = "tec.documentos.qr.secret";

    @Inject private UsuarioService usuarioService;
    @Inject private IglesiaService iglesiaService;
    @Inject private IglesiaPersonaService iglesiaPersonaService;
    @Inject private ProcesoElectoralService procesoElectoralService;
    @Inject private TribunalService tribunalService;
    @Inject private DocumentoFacade documentoFacade;
    @Inject private TipoDocumentoFacade tipoDocumentoFacade;

    @Resource
    private SessionContext sessionContext;

    public EstadoActaActualizacionDTO evaluarParaUsuarioActual(Integer iglesiaId) {
        ContextoActa contexto = cargarContexto(iglesiaId, false);
        boolean puedeGenerar = contexto.progreso()[0] > 0
                && contexto.progreso()[0] == contexto.progreso()[1]
                && contexto.proceso() != null;
        Integer documentoId = null;
        if (puedeGenerar) {
            TipoDocumento tipo = tipoDocumentoFacade.buscarActivoPorNombre(
                    Constantes.TIPO_ACTA_ACTUALIZACION_MIEMBROS);
            if (tipo != null) {
                Documentos existente = documentoFacade.buscarActivoPorEntidadTipoYContexto(
                        contexto.iglesia().getId(), tipo.getId(), contexto.hashContexto());
                if (existente != null && RepositorioDocumentos.estaDisponible(existente.getPath())) {
                    documentoId = existente.getId();
                }
            }
        }
        return new EstadoActaActualizacionDTO(contexto.progreso()[0], contexto.progreso()[1],
                puedeGenerar, documentoId);
    }

    public Documentos generarParaUsuarioActual(Integer iglesiaId) {
        ContextoActa contexto = cargarContexto(iglesiaId, true);
        validarActualizacionCompleta(contexto.progreso());
        TipoDocumento tipo = obtenerTipo();
        Documentos existente = documentoFacade.buscarActivoPorEntidadTipoYContexto(
                contexto.iglesia().getId(), tipo.getId(), contexto.hashContexto());
        if (existente != null && RepositorioDocumentos.estaDisponible(existente.getPath())) {
            return existente;
        }
        if (existente != null) {
            documentoFacade.delete(existente);
        }

        Firmantes firmantes = resolverFirmantes(contexto.proceso().getId(), contexto.usuario());
        LocalDateTime fechaGeneracion = LocalDateTime.now();
        String codigo = "ACTA-ACTUALIZACION-P" + contexto.proceso().getId() + "-I"
                + contexto.iglesia().getId() + "-"
                + fechaGeneracion.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);
        String payloadQr = construirPayloadQr(codigo, contexto.iglesia().getId(), contexto.proceso().getId(),
                fechaGeneracion, contexto.hashContexto());
        byte[] contenido = ActaActualizacionMiembrosPdf.generar(
                IglesiaDTO.fromEntity(contexto.iglesia()), contexto.proceso().getNombre(), contexto.miembros(),
                firmantes.presidenteTribunal(), firmantes.secretarioTribunal(),
                firmantes.administradorIglesia(), fechaGeneracion, codigo, payloadQr);

        String nombreArchivo = "acta-actualizacion-iglesia-" + contexto.iglesia().getId() + "-"
                + fechaGeneracion.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
        Path archivo = escribirArchivo(contenido, nombreArchivo);
        try {
            Documentos documento = new Documentos(nombreArchivo.replaceFirst("(?i)\\.pdf$", ""),
                    RepositorioDocumentos.rutaRelativaParaPersistir(archivo), tipo,
                    contexto.iglesia().getId(), ".pdf", "application/pdf", codigo);
            documento.setProceso(contexto.proceso());
            documento.setContextoHash(contexto.hashContexto());
            documento.setHashSha256(RepositorioDocumentos.sha256(contenido));
            Documentos persistido = documentoFacade.create(documento);
            if (persistido == null || persistido.getId() == null) {
                throw new IllegalStateException("No se pudo registrar la metadata del acta.");
            }
            return persistido;
        } catch (RuntimeException | IOException e) {
            RepositorioDocumentos.eliminarSilencioso(archivo);
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.generar"));
        }
    }

    public Documentos obtenerDocumentoParaUsuarioActual(Integer iglesiaId, Integer documentoId) {
        ContextoActa contexto = cargarContexto(iglesiaId, false);
        if (documentoId == null) {
            return null;
        }
        Documentos documento = documentoFacade.find(documentoId);
        if (documento == null || !contexto.iglesia().getId().equals(documento.getEntidadId())
                || documento.getTipoDocumento() == null
                || !Constantes.TIPO_ACTA_ACTUALIZACION_MIEMBROS.equalsIgnoreCase(
                        documento.getTipoDocumento().getNombre())) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.no.autorizada"));
        }
        return documento;
    }

    private ContextoActa cargarContexto(Integer iglesiaId, boolean requiereProceso) {
        Usuario usuario = obtenerUsuarioIglesiaAdmin(iglesiaId);
        Iglesia iglesia = iglesiaId != null ? iglesiaService.find(iglesiaId) : null;
        if (iglesia == null || !Boolean.TRUE.equals(iglesia.getEstado())) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.iglesia"));
        }
        ProcesoElectoral proceso = procesoElectoralService.getActivo();
        if (requiereProceso && (proceso == null || proceso.getId() == null)) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.proceso"));
        }
        int[] progreso = iglesiaPersonaService.calcularProgresoActualizacion(iglesiaId);
        List<IglesiaPersonaDTO> miembros = iglesiaPersonaService.listarDTOsPorIglesia(iglesiaId);
        return new ContextoActa(usuario, iglesia, proceso, progreso, miembros,
                calcularHashContexto(proceso, iglesia, miembros));
    }

    private Usuario obtenerUsuarioIglesiaAdmin(Integer iglesiaId) {
        if (iglesiaId == null || sessionContext == null || !sessionContext.isCallerInRole("SITEC-IglesiaAdmin")) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.no.autorizada"));
        }
        String username = sessionContext.getCallerPrincipal() != null
                ? sessionContext.getCallerPrincipal().getName() : null;
        Usuario usuario = username != null ? usuarioService.findByUsuarioName(username) : null;
        if (usuario == null || usuario.getIglesia() == null || usuario.getIglesia().getId() == null
                || !iglesiaId.equals(usuario.getIglesia().getId())) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.no.autorizada"));
        }
        return usuario;
    }

    private void validarActualizacionCompleta(int[] progreso) {
        if (progreso == null || progreso.length < 2 || progreso[0] <= 0 || progreso[0] != progreso[1]) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.incompleta"));
        }
    }

    private TipoDocumento obtenerTipo() {
        TipoDocumento tipo = tipoDocumentoFacade.buscarActivoPorNombre(
                Constantes.TIPO_ACTA_ACTUALIZACION_MIEMBROS);
        if (tipo == null) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.tipo"));
        }
        return tipo;
    }

    private Firmantes resolverFirmantes(Integer procesoId, Usuario usuario) {
        List<TribunalDTO> autoridades = tribunalService.listarDTOsActivosPorProceso(procesoId);
        TribunalDTO presidente = buscarAutoridad(autoridades, CARGO_PRESIDENTE_TRIBUNAL);
        TribunalDTO secretario = buscarAutoridad(autoridades, CARGO_SECRETARIO_TRIBUNAL);
        if (presidente == null || secretario == null || usuario.getPersonsa() == null
                || texto(usuario.getPersonsa().getNombres()).isBlank()) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.firmantes"));
        }
        return new Firmantes(nombrePersona(presidente), nombrePersona(secretario),
                texto(usuario.getPersonsa().getNombres()));
    }

    private TribunalDTO buscarAutoridad(List<TribunalDTO> autoridades, Integer cargoId) {
        if (autoridades == null) {
            return null;
        }
        return autoridades.stream().filter(autoridad -> autoridad != null
                && cargoId.equals(autoridad.getCargoId())
                && autoridad.getIglesiaPersona() != null
                && autoridad.getIglesiaPersona().getPersona() != null
                && !texto(autoridad.getIglesiaPersona().getPersona().getNombres()).isBlank())
                .findFirst().orElse(null);
    }

    private String nombrePersona(TribunalDTO autoridad) {
        return texto(autoridad.getIglesiaPersona().getPersona().getNombres());
    }

    private String calcularHashContexto(ProcesoElectoral proceso, Iglesia iglesia,
            List<IglesiaPersonaDTO> miembros) {
        StringBuilder fuente = new StringBuilder();
        fuente.append(proceso != null ? proceso.getId() : "").append('|')
                .append(iglesia.getId()).append('|').append(texto(iglesia.getNombre()));
        if (miembros != null) {
            for (IglesiaPersonaDTO miembro : miembros) {
                fuente.append("|M:").append(miembro != null ? miembro.getId() : "")
                        .append(':').append(miembro != null ? miembro.getFechaActualiza() : "");
            }
        }
        try {
            return RepositorioDocumentos.sha256(fuente.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.generar"));
        }
    }

    private String construirPayloadQr(String codigo, Integer iglesiaId, Integer procesoId,
            LocalDateTime fecha, String hashContexto) {
        String secreto = obtenerSecretoQr();
        String payload = "v=1|a=" + codigo + "|i=" + iglesiaId + "|p=" + procesoId
                + "|t=" + fecha.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + "|h=" + hashContexto;
        String codificado = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return codificado + "." + firmar(codificado, secreto);
    }

    private String obtenerSecretoQr() {
        String secreto = System.getProperty(PROPIEDAD_SECRETO_QR);
        if (secreto == null || secreto.isBlank()) {
            secreto = System.getenv("TEC_DOCUMENTOS_QR_SECRET");
        }
        if (secreto == null || secreto.isBlank()) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.qr.secreto"));
        }
        return secreto;
    }

    private String firmar(String contenido, String secreto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.qr.secreto"));
        }
    }

    private Path escribirArchivo(byte[] contenido, String nombreArchivo) {
        try {
            return RepositorioDocumentos.escribirAtomico("actas-actualizacion-miembros", nombreArchivo, contenido);
        } catch (IOException e) {
            throw new NegocioException(Constantes.getMensaje("actaActualizacion.error.generar"));
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private record ContextoActa(Usuario usuario, Iglesia iglesia, ProcesoElectoral proceso,
            int[] progreso, List<IglesiaPersonaDTO> miembros, String hashContexto) {
    }

    private record Firmantes(String presidenteTribunal, String secretarioTribunal,
            String administradorIglesia) {
    }
}
