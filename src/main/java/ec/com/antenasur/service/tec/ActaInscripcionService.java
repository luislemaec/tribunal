package ec.com.antenasur.service.tec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.itextpdf.text.pdf.PdfReader;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.CandidatoDTO;
import ec.com.antenasur.dto.EstadoActaInscripcionDTO;
import ec.com.antenasur.dto.ListaDTO;
import ec.com.antenasur.dto.TribunalDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.tec.CatalogoGeneralFacade;
import ec.com.antenasur.facade.tec.DocumentoFacade;
import ec.com.antenasur.facade.tec.ListaFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.facade.tec.TipoDocumentoFacade;
import ec.com.antenasur.itext.ActaInscripcionPdf;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.TipoDocumento;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.RepositorioDocumentos;

@Stateless
public class ActaInscripcionService {

    private static final int CARGO_PADRE_CANDIDATOS = 8;
    private static final long TAMANIO_MAXIMO_FIRMADA = 10L * 1024L * 1024L;

    @Inject private ListaFacade listaFacade;
    @Inject private ProcesoElectoralFacade procesoFacade;
    @Inject private CatalogoGeneralFacade catalogoFacade;
    @Inject private CandidatoService candidatoService;
    @Inject private TribunalService tribunalService;
    @Inject private DocumentoFacade documentoFacade;
    @Inject private TipoDocumentoFacade tipoDocumentoFacade;

    @Resource
    private SessionContext sessionContext;

    public EstadoActaInscripcionDTO evaluar(Integer listaId, Integer procesoId) {
        ContextoActa contexto = cargarContexto(listaId, procesoId, false);
        EstadoActaInscripcionDTO estado = new EstadoActaInscripcionDTO();
        estado.setTotalCargos(contexto.candidatos().size());
        estado.setTotalCandidatos((int) contexto.candidatos().stream().filter(this::estaAsignado).count());
        estado.setTotalAutoridades(contexto.autoridades().size());
        estado.setListaCompleta(estado.getTotalCargos() > 0
                && estado.getTotalCargos() == estado.getTotalCandidatos());
        estado.setAutoridadesDisponibles(!contexto.autoridades().isEmpty()
                && contexto.autoridades().stream().allMatch(a -> a.getIglesiaPersona() != null
                && a.getIglesiaPersona().getPersona() != null));
        estado.setPuedeGenerar(estado.isListaCompleta() && estado.isAutoridadesDisponibles());

        if (estado.getTotalCargos() == 0) {
            estado.setDetalle(mensaje("form.candidatos.acta.status.no.cargos"));
        } else if (!estado.isListaCompleta()) {
            estado.setDetalle(mensaje("form.candidatos.acta.status.incomplete"));
        } else if (!estado.isAutoridadesDisponibles()) {
            estado.setDetalle(mensaje("form.candidatos.acta.status.no.authorities"));
        } else {
            estado.setDetalle(mensaje("form.candidatos.acta.status.ready"));
        }

        if (estado.isPuedeGenerar()) {
            String hash = calcularContextoHash(contexto);
            estado.setContextoHash(hash);
            TipoDocumento generado = obtenerTipo(Constantes.TIPO_ACTA_INSCRIPCION_GENERADA);
            Documentos acta = documentoFacade.buscarActivoPorEntidadTipoYContexto(
                    listaId, generado.getId(), hash);
            if (acta != null) {
                estado.setActaGeneradaId(acta.getId());
                TipoDocumento firmada = obtenerTipo(Constantes.TIPO_ACTA_INSCRIPCION_FIRMADA);
                Documentos suscrita = documentoFacade.buscarFirmadoActivoPorOrigen(acta.getId(), firmada.getId());
                if (suscrita != null) {
                    estado.setActaFirmadaId(suscrita.getId());
                }
            }
        }
        return estado;
    }

    public Documentos generar(Integer listaId, Integer procesoId) {
        ContextoActa contexto = cargarContexto(listaId, procesoId, true);
        validarCompleta(contexto);
        String contextoHash = calcularContextoHash(contexto);
        TipoDocumento tipo = obtenerTipo(Constantes.TIPO_ACTA_INSCRIPCION_GENERADA);
        Documentos existente = documentoFacade.buscarActivoPorEntidadTipoYContexto(
                listaId, tipo.getId(), contextoHash);
        if (existente != null && archivoValido(existente.getPath())) {
            return existente;
        }
        if (existente != null) {
            documentoFacade.delete(existente);
        }

        LocalDateTime ahora = LocalDateTime.now();
        String usuario = usuarioActual();
        String codigoDocumento = UUID.randomUUID().toString();
        byte[] contenido = ActaInscripcionPdf.generar(
                ListaDTO.fromEntity(contexto.lista()), contexto.proceso().getNombre(),
                contexto.candidatos(), contexto.autoridades(), Constantes.getLugarActaInscripcion(), ahora,
                usuario, codigoDocumento);
        String baseNombre = "acta-inscripcion-lista-" + contexto.lista().getId() + "-"
                + ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + sufijoUnico();
        Path destino = escribirArchivo(contenido, "actas-inscripcion/generadas", baseNombre + ".pdf");
        try {
            Documentos documento = new Documentos(baseNombre, rutaPersistencia(destino), tipo, listaId,
                    ".pdf", "application/pdf", codigoDocumento);
            documento.setContextoHash(contextoHash);
            documento.setHashSha256(calcularHash(contenido));
            Documentos persistido = documentoFacade.create(documento);
            if (persistido == null || persistido.getId() == null) {
                throw new IllegalStateException("No se pudo registrar la metadata del acta generada.");
            }
            return persistido;
        } catch (RuntimeException e) {
            RepositorioDocumentos.eliminarSilencioso(destino);
            throw e;
        }
    }

    public Documentos cargarFirmada(Integer listaId, Integer procesoId,
            String nombreOriginal, byte[] contenido) {
        validarPdf(nombreOriginal, contenido);
        ContextoActa contexto = cargarContexto(listaId, procesoId, true);
        validarCompleta(contexto);
        String contextoHash = calcularContextoHash(contexto);
        TipoDocumento tipoGenerado = obtenerTipo(Constantes.TIPO_ACTA_INSCRIPCION_GENERADA);
        Documentos origen = documentoFacade.buscarActivoPorEntidadTipoYContexto(
                listaId, tipoGenerado.getId(), contextoHash);
        if (origen == null || !archivoValido(origen.getPath())) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.generate.first"));
        }
        if (!listaId.equals(origen.getEntidadId())) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.wrong.list"));
        }
        TipoDocumento tipoFirmado = obtenerTipo(Constantes.TIPO_ACTA_INSCRIPCION_FIRMADA);
        if (documentoFacade.buscarFirmadoActivoPorOrigen(origen.getId(), tipoFirmado.getId()) != null) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.duplicate"));
        }

        String nombreSeguro = nombreSinExtension(nombreOriginal);
        String baseNombre = "acta-inscripcion-firmada-lista-" + listaId + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + sufijoUnico() + "-" + nombreSeguro;
        Path destino = escribirArchivo(contenido, "actas-inscripcion/firmadas", baseNombre + ".pdf");
        try {
            Documentos documento = new Documentos(baseNombre, rutaPersistencia(destino), tipoFirmado, listaId,
                    ".pdf", "application/pdf", UUID.randomUUID().toString());
            documento.setDocumentoOrigen(origen);
            documento.setContextoHash(contextoHash);
            documento.setHashSha256(calcularHash(contenido));
            Documentos persistido = documentoFacade.create(documento);
            if (persistido == null || persistido.getId() == null) {
                throw new IllegalStateException("No se pudo registrar la metadata del acta firmada.");
            }
            return persistido;
        } catch (RuntimeException e) {
            RepositorioDocumentos.eliminarSilencioso(destino);
            throw e;
        }
    }

    public List<Documentos> listarDocumentos(Integer listaId) {
        List<Documentos> resultado = new ArrayList<>();
        if (listaId == null) {
            return resultado;
        }
        agregar(resultado, documentoFacade.getDocumentosPorEntidadYTipoDoc(listaId,
                obtenerTipo(Constantes.TIPO_ACTA_INSCRIPCION_GENERADA).getId()));
        agregar(resultado, documentoFacade.getDocumentosPorEntidadYTipoDoc(listaId,
                obtenerTipo(Constantes.TIPO_ACTA_INSCRIPCION_FIRMADA).getId()));
        resultado.sort(Comparator.comparing(Documentos::getId).reversed());
        return resultado;
    }

    private ContextoActa cargarContexto(Integer listaId, Integer procesoId, boolean estricto) {
        Lista lista = listaId != null ? listaFacade.find(listaId) : null;
        ProcesoElectoral proceso = procesoId != null ? procesoFacade.find(procesoId) : null;
        if (estricto && (lista == null || !Boolean.TRUE.equals(lista.getEstado()))) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.select.list"));
        }
        if (estricto && (proceso == null || !Boolean.TRUE.equals(proceso.getActivo()))) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.no.process"));
        }
        if (lista == null || proceso == null) {
            return new ContextoActa(lista, proceso, new ArrayList<>(), new ArrayList<>());
        }
        List<CatalogoGeneral> cargos = catalogoFacade.listaCatalogoHijo(CARGO_PADRE_CANDIDATOS);
        List<CandidatoDTO> candidatos = candidatoService.listarDTOsPorListaConCargos(
                listaId, procesoId, cargos != null ? cargos : new ArrayList<>());
        List<TribunalDTO> autoridades = tribunalService.listarDTOsActivosPorProceso(procesoId);
        return new ContextoActa(lista, proceso, candidatos, autoridades);
    }

    private void validarCompleta(ContextoActa contexto) {
        if (contexto.candidatos().isEmpty() || contexto.candidatos().stream().anyMatch(c -> !estaAsignado(c))) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.incomplete"));
        }
        if (contexto.autoridades().isEmpty()
                || contexto.autoridades().stream().anyMatch(a -> a.getIglesiaPersona() == null
                || a.getIglesiaPersona().getPersona() == null)) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.authorities"));
        }
    }

    private boolean estaAsignado(CandidatoDTO candidato) {
        return candidato != null && candidato.getId() != null
                && candidato.getIglesiaPersona() != null
                && candidato.getIglesiaPersona().getPersona() != null;
    }

    private String calcularContextoHash(ContextoActa contexto) {
        StringBuilder fuente = new StringBuilder();
        fuente.append(contexto.proceso().getId()).append('|')
                .append(contexto.lista().getId()).append('|')
                .append(contexto.lista().getNombre()).append('|')
                .append(contexto.lista().getNumero()).append('|')
                .append(contexto.lista().getSlogan());
        for (CandidatoDTO candidato : contexto.candidatos()) {
            fuente.append("|C:").append(candidato.getCargoId()).append(':').append(candidato.getId())
                    .append(':').append(candidato.getIglesiaPersona().getPersona().getId())
                    .append(':').append(candidato.getIglesiaPersona().getPersona().getNombres())
                    .append(':').append(candidato.getIglesiaPersona().getPersona().getApellidos());
        }
        for (TribunalDTO autoridad : contexto.autoridades()) {
            fuente.append("|A:").append(autoridad.getCargoId()).append(':').append(autoridad.getId())
                    .append(':').append(autoridad.getIglesiaPersona().getPersona().getId())
                    .append(':').append(autoridad.getIglesiaPersona().getPersona().getNombres())
                    .append(':').append(autoridad.getIglesiaPersona().getPersona().getApellidos());
        }
        return calcularHash(fuente.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void validarPdf(String nombre, byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.empty"));
        }
        if (contenido.length > TAMANIO_MAXIMO_FIRMADA) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.max.size"));
        }
        if (nombre == null || !nombre.toLowerCase().endsWith(".pdf")) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.only.pdf"));
        }
        try {
            PdfReader lector = new PdfReader(contenido);
            if (lector.getNumberOfPages() < 1) {
                throw new IOException("PDF sin paginas");
            }
            lector.close();
        } catch (Exception e) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.invalid.pdf"));
        }
    }

    private Path escribirArchivo(byte[] contenido, String subdirectorio, String nombreArchivo) {
        try {
            return RepositorioDocumentos.escribirAtomico(subdirectorio, nombreArchivo, contenido);
        } catch (IOException e) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.storage"));
        }
    }

    private String rutaPersistencia(Path archivo) {
        try {
            return RepositorioDocumentos.rutaRelativaParaPersistir(archivo);
        } catch (IOException e) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.storage"));
        }
    }

    private TipoDocumento obtenerTipo(String nombre) {
        TipoDocumento tipo = tipoDocumentoFacade.buscarActivoPorNombre(nombre);
        if (tipo == null) {
            throw new NegocioException(mensaje("form.candidatos.acta.error.type", nombre));
        }
        return tipo;
    }

    private String usuarioActual() {
        try {
            String nombre = sessionContext.getCallerPrincipal().getName();
            return nombre == null || nombre.isBlank() ? "<desconocido>" : nombre;
        } catch (Exception e) {
            return "<desconocido>";
        }
    }

    private String calcularHash(byte[] contenido) {
        try {
            return RepositorioDocumentos.sha256(contenido);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo calcular la huella del documento.", e);
        }
    }

    private boolean archivoValido(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return false;
        }
        try {
            return RepositorioDocumentos.estaDisponible(ruta);
        } catch (Exception e) {
            return false;
        }
    }

    private String nombreSinExtension(String nombre) {
        String base = nombre == null ? "acta" : nombre.replaceFirst("(?i)\\.pdf$", "");
        base = base.replaceAll("[^A-Za-z0-9_-]", "-").replaceAll("-+", "-");
        return base.isBlank() ? "acta" : base.substring(0, Math.min(base.length(), 60));
    }

    private String sufijoUnico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void agregar(List<Documentos> destino, List<Documentos> origen) {
        if (origen != null) {
            destino.addAll(origen);
        }
    }

    private String mensaje(String clave, Object... argumentos) {
        return Constantes.getMensaje(clave, argumentos);
    }

    private record ContextoActa(Lista lista, ProcesoElectoral proceso,
            List<CandidatoDTO> candidatos, List<TribunalDTO> autoridades) {
    }
}
