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
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

import ec.com.antenasur.dto.CategoriaVotoDTO;
import ec.com.antenasur.dto.CertificadoVotacionDTO;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.facade.tec.CronogramaFaseFacade;
import ec.com.antenasur.dto.DocumentoDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.PadronDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.dto.ReporteMesaDTO;
import ec.com.antenasur.dto.MesaDocumentosDTO;
import ec.com.antenasur.enums.TipoDocumentoMesa;
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
	@Inject
	private EmisionAccesoQrService emisionQr;

	@Inject
	private ProcesoElectoralService procesoService;
	@Inject
	private ProcesoElectoralFacade procesoFacade;
	@Inject
	private RecintoService recintoService;
	@Inject
	private MesaService mesaService;
	@Inject
	private MesaFacade mesaFacade;
	@Inject
	private EscrutinioService escrutinioService;
	@Inject
	private CategoriaVotoService categoriaVotoService;
	@Inject
	private PadronService padronService;
	@Inject
	private MiembroJRVService miembroJrvService;
	@Inject
	private DocumentoService documentoService;
	@Inject
	private DocumentoFacade documentoFacade;
	@Inject
	private TipoDocumentoFacade tipoDocumentoFacade;
	@Inject
	private CronogramaFaseFacade cronogramaFacade;
	@Inject
	private AccesoDocumentoMesaService accesoDocumental;
	@Inject
	private DisponibilidadDocumentoMesaService disponibilidad;

	public static final String TIPO_CERTIFICADOS = "CERTIFICADOS DE VOTACION DE MESA";

	@Resource
	private TransactionSynchronizationRegistry transacciones;

	@Resource
	private SessionContext sessionContext;

	public List<ProcesoElectoralDTO> listarProcesos() {
		return procesoService.listarDTOs();
	}

	public List<MesaDocumentosDTO> listarResumenDocumentos(Integer procesoId, Integer cantonId, Integer parroquiaId,
			Integer personaId, boolean presidenteRestringido) {
		ProcesoElectoral activo = procesoService.getActivo();
		if (activo == null || !activo.getId().equals(procesoId)) {
			throw new NegocioException(mensaje("reportesMesa.error.proceso.activo"));
		}
		Integer mesaPermitida = accesoDocumental.mesaPermitida(procesoId);
		var filas = mesaFacade.listarResumenDocumentos(procesoId, cantonId, parroquiaId, mesaPermitida);
		disponibilidad.completar(procesoId, filas);
		return filas;
	}

	public MesaDocumentosDTO consultarResumenDocumento(Integer procesoId, Integer mesaId, Integer personaId,
			boolean presidenteRestringido) {
		ProcesoElectoral activo = procesoService.getActivo();
		if (activo == null || !activo.getId().equals(procesoId) || mesaId == null) {
			throw new NegocioException(mensaje("reportesMesa.error.proceso.activo"));
		}
		accesoDocumental.validar(mesaId, procesoId);
		List<MesaDocumentosDTO> filas = mesaFacade.listarResumenDocumentos(procesoId, null, null, mesaId);
		disponibilidad.completar(procesoId, filas);
		return filas.isEmpty() ? null : filas.get(0);
	}

	public DocumentoDTO generarDocumentoMesa(TipoDocumentoMesa tipo, boolean regenerar, Integer procesoId,
			Integer recintoId, Integer mesaId, Integer personaId, boolean presidenteRestringido) {
		if (tipo == null || !tipo.isDocumentoGenerable()) {
			throw new NegocioException(mensaje("reportesMesa.error.documento.no.generable"));
		}
		return switch (tipo) {
		case PADRON_MESA -> generarPadron(procesoId, recintoId, mesaId, personaId, presidenteRestringido, regenerar);
		case ACTA_PARCIAL -> generarActaParcial(procesoId, recintoId, mesaId, personaId, presidenteRestringido,
				regenerar);
		case CERTIFICADOS_VOTACION -> generarCertificados(procesoId, recintoId, mesaId, personaId,
				presidenteRestringido, regenerar);
		case ACTA_FISICA_ESCRUTINIO, DESIGNACION_MJRV -> throw new NegocioException(
				mensaje("reportesMesa.error.documento.no.generable"));
		};
	}

	public DocumentoDTO obtenerDocumentoActivo(TipoDocumentoMesa tipo, Integer procesoId, Integer recintoId,
			Integer mesaId, Integer personaId, boolean presidenteRestringido) {
		if (tipo == null || !tipo.isDocumentoConsultable()) {
			throw new NegocioException(mensaje("reportesMesa.error.documento.no.generable"));
		}
		disponibilidad.validar(tipo, procesoId, recintoId, mesaId, false);
		ProcesoElectoral proceso = procesoId != null ? procesoFacade.find(procesoId) : null;
		Mesa mesa = mesaId != null ? mesaFacade.buscarDetallePorId(mesaId) : null;
		validarSeleccion(proceso, recintoId, mesa);
		validarAcceso(mesaId, procesoId, personaId, presidenteRestringido);
		Documentos documento = documentoService.buscarActivoPorMesaProcesoTipo(mesaId, procesoId,
				obtenerTipo(tipo.getNombreTipo()).getId());
		if (documento == null || !RepositorioDocumentos.estaDisponible(documento.getPath())) {
			throw new NegocioException(mensaje("reportesMesa.error.documento.no.disponible"));
		}
		documentoService.validarContextoMesa(documento, mesaId, procesoId, recintoId);
		return toDocumentoDisponible(documento);
	}

	public List<MiembroJRVDTO> visualizarJunta(Integer procesoId, Integer recintoId, Integer mesaId) {
		disponibilidad.validar(TipoDocumentoMesa.DESIGNACION_MJRV, procesoId, recintoId, mesaId, false);
		return miembroJrvService.listarDTOsPorMesaProceso(mesaId, procesoId);
	}

	public List<RecintoDTO> listarRecintos(Integer procesoId, Integer personaId, boolean presidenteRestringido) {
		if (procesoId == null) {
			return Collections.emptyList();
		}
		Integer permitida = accesoDocumental.mesaPermitida(procesoId);
		if (permitida != null) {
			return List.of(RecintoDTO.fromEntity(mesaFacade.buscarDetallePorId(permitida).getRecinto()));
		}
		return recintoService.listarDTOsPorProceso(procesoId);
	}

	public List<MesaDTO> listarMesas(Integer procesoId, Integer recintoId, Integer personaId,
			boolean presidenteRestringido) {
		if (procesoId == null || recintoId == null) {
			return Collections.emptyList();
		}
		Integer permitida = accesoDocumental.mesaPermitida(procesoId);
		if (permitida != null) {
			Mesa mesa = mesaFacade.buscarDetallePorId(permitida);
			if (mesa == null || mesa.getRecinto() == null || !recintoId.equals(mesa.getRecinto().getId())) {
				return Collections.emptyList();
			}
			return List.of(MesaDTO.fromEntity(mesa));
		}
		return mesaService.listarDTOsPorRecintoYProceso(recintoId, procesoId);
	}

	public ReporteMesaDTO consultar(Integer procesoId, Integer recintoId, Integer mesaId, Integer personaId,
			boolean presidenteRestringido) {
		ProcesoElectoral proceso = procesoId != null ? procesoFacade.find(procesoId) : null;
		Mesa mesa = mesaId != null ? mesaFacade.buscarDetallePorId(mesaId) : null;
		validarSeleccion(proceso, recintoId, mesa);
		validarAcceso(mesaId, procesoId, personaId, presidenteRestringido);

		ReporteMesaDTO reporte = new ReporteMesaDTO();
		reporte.setProceso(ProcesoElectoralDTO.fromEntity(proceso));
		ec.com.antenasur.model.tec.CronogramaFase faseSufragio = cronogramaFacade.getFasePorTipo(procesoId,
				FaseElectoral.SUFRAGIO);
		reporte.setFechaSufragio(faseSufragio != null ? faseSufragio.getFechaInicio() : null);
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

	public DocumentoDTO generarActaParcial(Integer procesoId, Integer recintoId, Integer mesaId, Integer personaId,
			boolean presidenteRestringido) {
		return generarActaParcial(procesoId, recintoId, mesaId, personaId, presidenteRestringido, false);
	}

	private DocumentoDTO generarActaParcial(Integer procesoId, Integer recintoId, Integer mesaId, Integer personaId,
			boolean presidenteRestringido, boolean regenerar) {
		disponibilidad.validar(TipoDocumentoMesa.ACTA_PARCIAL, procesoId, recintoId, mesaId, true);
		ReporteMesaDTO reporte = consultar(procesoId, recintoId, mesaId, personaId, presidenteRestringido);
		if (!ec.com.antenasur.security.qr.ConfiguracionQr.habilitado())
			throw new NegocioException(mensaje("actaQr.error.emision.deshabilitada"));
		java.net.URI origenQr;
		try {
			origenQr = ec.com.antenasur.security.qr.ConfiguracionQr.origen();
		} catch (IllegalArgumentException e) {
			throw new NegocioException(mensaje("actaQr.error.emision.origen"));
		}
		String contexto = hash(hashContextoActa(reporte) + "|QR2-OBLIGATORIO");
		TipoDocumento tipo = obtenerTipo(Constantes.TIPO_ACTA_PARCIAL_ESCRUTINIO);
		documentoFacade.bloquearMesaParaVersion(mesaId);
		Documentos documentoActivo = documentoService.buscarActivoPorMesaProcesoTipo(mesaId, procesoId, tipo.getId());
		Documentos existente = buscarDocumentoVigente(mesaId, procesoId, tipo.getId(), contexto);
		if (!regenerar && existente != null) {
			return toDocumentoDisponible(existente);
		}

		int version = documentoFacade.siguienteVersionMesaProcesoTipo(mesaId, procesoId, tipo.getId());
		LocalDateTime ahora = LocalDateTime.now();
		String codigo = "ACTA-PARCIAL-" + procesoId + "-R" + recintoId + "-M" + mesaId + "-"
				+ ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + sufijo();
		String folio = String.format("AP-PE%02d-R%03d-M%03d-V%02d", procesoId, recintoId, mesaId, version);
		String codigoBarras = "ACTA_PARCIAL|" + procesoId + "|" + recintoId + "|" + mesaId + "|" + version + "|"
				+ codigo.substring(codigo.lastIndexOf('-') + 1);
		byte[] contenido;
		String tokenQr = ec.com.antenasur.security.qr.TokenActaQr.generar();
		try {
			contenido = ReportePFD.generarFormularioActaParcial(reporte, folio, codigoBarras, ahora, usuarioActual(),
					ec.com.antenasur.security.qr.TokenActaQr.url(origenQr, tokenQr).toASCIIString());
		} catch (Exception e) {
			throw new NegocioException(mensaje("reportesMesa.error.generar.acta"));
		}
		DocumentoDTO guardado = almacenar(reporte, tipo, codigo, ".pdf", "application/pdf", contenido,
				"actas-escrutinio/parciales", contexto, regenerar || documentoActivo != null, version, folio);
		emisionQr.registrarParaDocumento(guardado.getId(), tokenQr);
		return guardado;
	}

	public DocumentoDTO generarPadron(Integer procesoId, Integer recintoId, Integer mesaId, Integer personaId,
			boolean presidenteRestringido) {
		return generarPadron(procesoId, recintoId, mesaId, personaId, presidenteRestringido, false);
	}

	private DocumentoDTO generarPadron(Integer procesoId, Integer recintoId, Integer mesaId, Integer personaId,
			boolean presidenteRestringido, boolean regenerar) {
		disponibilidad.validar(TipoDocumentoMesa.PADRON_MESA, procesoId, recintoId, mesaId, true);
		ReporteMesaDTO reporte = consultar(procesoId, recintoId, mesaId, personaId, presidenteRestringido);
		if (!Boolean.TRUE.equals(reporte.getProceso().getActivo())) {
			throw new NegocioException(mensaje("reportesMesa.error.padron.proceso.inactivo"));
		}
		if (reporte.getPadron().isEmpty()) {
			throw new NegocioException(mensaje("reportesMesa.error.sin.padron"));
		}
		String contexto = hashContextoPadron(reporte);
		TipoDocumento tipo = obtenerTipo(Constantes.TIPO_PADRON_ELECTORAL_MESA);
		documentoFacade.bloquearMesaParaVersion(mesaId);
		Documentos existente = buscarDocumentoVigente(mesaId, procesoId, tipo.getId(), contexto);
		if (!regenerar && existente != null) {
			return toDocumentoDisponible(existente);
		}

		LocalDateTime ahora = LocalDateTime.now();
		String codigo = "PADRON-" + procesoId + "-M" + mesaId + "-"
				+ ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + sufijo();
		byte[] contenido = generarExcelPadron(reporte);
		return almacenar(reporte, tipo, codigo, ".xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", contenido, "padrones-mesa",
				contexto, regenerar);
	}

	public DocumentoDTO generarCertificados(Integer procesoId, Integer recintoId, Integer mesaId, Integer personaId,
			boolean presidenteRestringido) {
		return generarCertificados(procesoId, recintoId, mesaId, personaId, presidenteRestringido, true);
	}

	private DocumentoDTO generarCertificados(Integer procesoId, Integer recintoId, Integer mesaId, Integer personaId,
			boolean presidenteRestringido, boolean regenerar) {
		disponibilidad.validar(TipoDocumentoMesa.CERTIFICADOS_VOTACION, procesoId, recintoId, mesaId, true);
		ProcesoElectoral proceso = procesoId != null ? procesoFacade.find(procesoId) : null;
		Mesa mesa = mesaId != null ? mesaFacade.buscarDetallePorId(mesaId) : null;
		validarSeleccion(proceso, recintoId, mesa);
		validarAcceso(mesaId, procesoId, personaId, presidenteRestringido);
		var personas = padronService.listarCertificados(mesaId, procesoId);
		if (personas.isEmpty()) {
			throw new NegocioException(mensaje("reportesMesa.certificados.vacio"));
		}
		if (personas.stream().map(CertificadoVotacionDTO::personaId).distinct().count() != personas.size()) {
			throw new NegocioException(mensaje("reportesMesa.certificados.duplicados"));
		}
		var fase = cronogramaFacade.getFasePorTipo(procesoId, FaseElectoral.SUFRAGIO);
		if (fase == null || !Boolean.TRUE.equals(fase.getEstado()) || fase.getFechaInicio() == null) {
			throw new NegocioException(mensaje("reportesMesa.certificados.sin.fecha"));
		}
		ReporteMesaDTO contexto = new ReporteMesaDTO();
		contexto.setProceso(ProcesoElectoralDTO.fromEntity(proceso));
		contexto.setMesa(MesaDTO.fromEntity(mesa));
		contexto.setRecinto(RecintoDTO.fromEntity(mesa.getRecinto()));
		TipoDocumento tipo = obtenerTipo(TIPO_CERTIFICADOS);
		documentoFacade.bloquearMesaParaVersion(mesaId);
		String huella = hash(contextoBase(contexto).append('|').append(fase.getFechaInicio().getTime()).append('|')
				.append(personas).toString());
		Documentos vigente = buscarDocumentoVigente(mesaId, procesoId, tipo.getId(), huella);
		if (!regenerar && vigente != null)
			return toDocumentoDisponible(vigente);
		String codigo = "certificados_votacion_" + procesoId + "_mesa_" + mesaId + "_"
				+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "_" + sufijo();
		byte[] contenido;
		try {
			contenido = ReportePFD.generarCertificadosVotacion(contexto, personas, fase.getFechaInicio());
		} catch (Exception e) {
			throw new NegocioException(mensaje("reportesMesa.certificados.error"));
		}
		return almacenar(contexto, tipo, codigo, ".pdf", "application/pdf", contenido, "certificados-votacion", huella,
				regenerar);
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
				item.setCategoriaTipo(categoria.getTipo());
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
				ReporteXLSX.creaEspacioInformativoPadron(ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
						ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss")), usuarioActual(),
						reporte.getProceso().getNombre(), reporte.getRecinto().getProvinciaNombre(),
						reporte.getRecinto().getCantonNombre(), reporte.getRecinto().getUbicacionNombre(),
						reporte.getRecinto().getNombre(), reporte.getMesa().getNombre());
				String[] columnas = { mensaje("reportesMesa.padron.numero"), mensaje("reportesMesa.padron.cedula"),
						mensaje("reportesMesa.padron.nombres"), mensaje("reportesMesa.padron.iglesia"),
						mensaje("reportesMesa.padron.comunidad"), mensaje("reportesMesa.padron.estado") };
				ReporteXLSX.creaCabeceraTabla(columnas, new int[] { 1800, 5000, 9000, 10000, 8000, 4000 });
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
		fila[5] = Boolean.TRUE.equals(padron.getSufrago()) ? mensaje("reportesMesa.padron.sufrago")
				: mensaje("reportesMesa.padron.pendiente");
		for (int i = 0; i < fila.length; i++) {
			if (fila[i] == null) {
				fila[i] = "";
			}
		}
	}

	private DocumentoDTO almacenar(ReporteMesaDTO reporte, TipoDocumento tipo, String codigo, String extension,
			String mime, byte[] contenido, String subdirectorio, String contexto) {
		return almacenar(reporte, tipo, codigo, extension, mime, contenido, subdirectorio, contexto, false);
	}

	private DocumentoDTO almacenar(ReporteMesaDTO reporte, TipoDocumento tipo, String codigo, String extension,
			String mime, byte[] contenido, String subdirectorio, String contexto, boolean nuevaVersion) {
		return almacenar(reporte, tipo, codigo, extension, mime, contenido, subdirectorio, contexto, nuevaVersion, null,
				null);
	}

	private DocumentoDTO almacenar(ReporteMesaDTO reporte, TipoDocumento tipo, String codigo, String extension,
			String mime, byte[] contenido, String subdirectorio, String contexto, boolean nuevaVersion, Integer version,
			String folio) {
		Path archivo = null;
		try {
			archivo = RepositorioDocumentos.escribirAtomico(subdirectorio, codigo + extension, contenido);
			final Path creado = archivo;
			transacciones.registerInterposedSynchronization(new Synchronization() {
				@Override
				public void beforeCompletion() {
				}

				@Override
				public void afterCompletion(int estado) {
					if (estado == Status.STATUS_ROLLEDBACK) {
						RepositorioDocumentos.eliminarSilencioso(creado);
					}
				}
			});
			Documentos documento = new Documentos(codigo, RepositorioDocumentos.rutaRelativaParaPersistir(archivo),
					tipo, reporte.getMesa().getId(), extension, mime, codigo);
			documento.setProceso(procesoFacade.find(reporte.getProceso().getId()));
			documento.setRecinto(mesaFacade.buscarDetallePorId(reporte.getMesa().getId()).getRecinto());
			documento.setMesa(mesaFacade.find(reporte.getMesa().getId()));
			documento.setContextoHash(contexto);
			documento.setHashSha256(RepositorioDocumentos.sha256(contenido));
			documento.setVersion(version);
			documento.setFolio(folio);
			Documentos persistido = documentoService.registrarVersionMesa(documento, reporte.getMesa().getId(),
					reporte.getProceso().getId(), reporte.getRecinto().getId());
			if (persistido == null || persistido.getId() == null) {
				throw new IOException("No se registro la metadata del documento.");
			}
			return toDocumentoDisponible(persistido);
		} catch (Exception e) {
			sessionContext.setRollbackOnly();
			RepositorioDocumentos.eliminarSilencioso(archivo);
			throw new NegocioException(mensaje("reportesMesa.error.almacenar"));
		}
	}

	private Documentos buscarDocumentoVigente(Integer mesaId, Integer procesoId, Integer tipoId, String contexto) {
		Documentos existente = documentoService.buscarActivoPorMesaProcesoTipo(mesaId, procesoId, tipoId);
		if (existente != null && !contexto.equals(existente.getContextoHash()))
			return null;
		if (existente == null) {
			return null;
		}
		documentoService.validarContextoMesa(existente, mesaId, procesoId, existente.getMesa().getRecinto().getId());
		if (RepositorioDocumentos.estaDisponible(existente.getPath())) {
			return existente;
		}
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

	private void validarAcceso(Integer mesaId, Integer procesoId, Integer personaId, boolean presidenteRestringido) {
		accesoDocumental.validar(mesaId, procesoId);
	}

	private void validarSeleccion(ProcesoElectoral proceso, Integer recintoId, Mesa mesa) {
		if (proceso == null || recintoId == null || mesa == null || mesa.getRecinto() == null
				|| !Boolean.TRUE.equals(proceso.getEstado()) || !Boolean.TRUE.equals(mesa.getEstado())
				|| !Boolean.TRUE.equals(mesa.getRecinto().getEstado())
				|| !recintoId.equals(mesa.getRecinto().getId())) {
			throw new NegocioException(mensaje("reportesMesa.error.seleccion"));
		}
		ProcesoElectoral activo = procesoService.getActivo();
		if (activo == null || !activo.getId().equals(proceso.getId())) {
			throw new NegocioException(mensaje("reportesMesa.error.proceso.activo"));
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
		fuente.append("|ACTA-PARCIAL-FORMULARIO-V1");
		fuente.append('|').append(reporte.getFechaSufragio());
		for (MiembroJRVDTO miembro : reporte.getMiembrosJrv()) {
			fuente.append("|J:").append(miembro.getId()).append(':').append(miembro.getCargoId());
			fuente.append(':').append(miembro.getCargoNombre());
			if (miembro.getIglesiaPersona() != null && miembro.getIglesiaPersona().getPersona() != null) {
				fuente.append(':').append(miembro.getIglesiaPersona().getPersona().getNombres()).append(':')
						.append(miembro.getIglesiaPersona().getPersona().getApellidos());
			}
		}
		for (EscrutinioDTO item : reporte.getEscrutinios())
			fuente.append("|C:").append(item.getCategoriaId()).append(':').append(item.getCategoriaNombre()).append(':')
					.append(item.getCategoriaTipo());
		return hash(fuente.toString());
	}

	private String hashContextoPadron(ReporteMesaDTO reporte) {
		StringBuilder fuente = contextoBase(reporte);
		for (PadronDTO padron : reporte.getPadron()) {
			fuente.append("|P:").append(padron.getId()).append(':').append(padron.getSufrago());
			fuente.append(':').append(padron.getIglesiaPersona());
		}
		return hash(fuente.toString());
	}

	private StringBuilder contextoBase(ReporteMesaDTO reporte) {
		return new StringBuilder().append(reporte.getProceso().getId()).append('|').append(reporte.getRecinto().getId())
				.append('|').append(reporte.getMesa().getId()).append('|').append(reporte.getProceso().getNombre())
				.append('|').append(reporte.getMesa().getNombre()).append('|').append(reporte.getRecinto().getNombre())
				.append('|').append(reporte.getRecinto().getProvinciaNombre()).append('|')
				.append(reporte.getRecinto().getCantonNombre()).append('|')
				.append(reporte.getRecinto().getUbicacionNombre());
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
