package ec.com.antenasur.controller;

import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import ec.com.antenasur.util.RepositorioDocumentos;
import org.primefaces.model.DefaultStreamedContent;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.dto.DocumentoDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.dto.ReporteMesaDTO;
import ec.com.antenasur.dto.MesaDocumentosDTO;
import ec.com.antenasur.dto.EstadoDocumentoMesaDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.OpcionPadronDTO;
import ec.com.antenasur.enums.TipoDocumentoMesa;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.service.tec.ReporteMesaService;
import ec.com.antenasur.service.tec.ActaFisicaEscrutinioService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.StreamedContent;

@Named("reporteMesaController")
@ViewScoped
@Slf4j
public class ReporteMesaController implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private ReporteMesaService reporteMesaService;
	@Inject
	private LoginBean loginBean;
	@Inject
	private ProcesoBean procesoBean;
	@Inject
	private DocumentoBean documentoBean;
	@Inject
	private ActaFisicaEscrutinioService actaFisicaEscrutinioService;
	@Inject
	private ec.com.antenasur.service.tec.DisponibilidadDocumentoMesaService disponibilidadDocumental;

	@Getter
	private List<ProcesoElectoralDTO> procesos = new ArrayList<>();
	@Getter
	private List<RecintoDTO> recintos = new ArrayList<>();
	@Getter
	private List<MesaDTO> mesas = new ArrayList<>();
	@Getter
	private ReporteMesaDTO reporte;
	@Getter
	private DocumentoDTO certificados;
	@Getter
	private DocumentoDTO actaFisicaVisualizada;
	@Getter
	private List<EscrutinioDTO> resultadosActaFinal = new ArrayList<>();
	@Getter
	private ec.com.antenasur.dto.RevisionActaFinalDTO revisionActaFinal = new ec.com.antenasur.dto.RevisionActaFinalDTO();
	@Getter
	private List<ec.com.antenasur.dto.MiembroJRVDTO> juntaVisualizada = new ArrayList<>();
	@Getter
	private String motivoActaFinal;
	private byte[] contenidoActaFisica;
	private boolean puedeValidarActaFinal;
	@Getter
	private Integer mesaActaFinalId;
	@Getter
	private Integer recintoActaFinalId;

	@Getter
	@Setter
	private Integer procesoId;
	@Getter
	@Setter
	private Integer recintoId;
	@Getter
	@Setter
	private Integer mesaId;
	@Getter
	private boolean presidenteRestringido;
	@Getter
	private ProcesoElectoralDTO procesoActivo;
	@Getter
	private List<MesaDocumentosDTO> resumenesDocumentales = new ArrayList<>();
	@Getter
	private List<OpcionPadronDTO> cantonesDocumentales = new ArrayList<>();
	@Getter
	private List<OpcionPadronDTO> parroquiasDocumentales = new ArrayList<>();
	@Getter
	@Setter
	private Integer cantonDocumentalId;
	@Getter
	@Setter
	private Integer parroquiaDocumentalId;
	@Getter
	@Setter
	private String estadoDocumental;
	@Getter
	@Setter
	private String busquedaDocumental;
	@Getter
	@Setter
	private int primeraFilaDocumental;

	@PostConstruct
	public void init() {
		presidenteRestringido = tieneRol("SITEC-Presidente-mesa");
		List<ProcesoElectoralDTO> disponibles = reporteMesaService.listarProcesos();
		ProcesoElectoralDTO activo = null;
		for (ProcesoElectoralDTO proceso : disponibles) {
			if (Boolean.TRUE.equals(proceso.getActivo())) {
				activo = proceso;
				break;
			}
		}
		if (presidenteRestringido) {
			if (activo != null) {
				procesos = List.of(activo);
			}
		} else {
			procesos = disponibles;
		}
		if (activo != null) {
			procesoActivo = activo;
			procesoId = activo.getId();
			cargarResumenesDocumentales();
		} else {
			JsfUtil.addWarningMessage(Constantes.getMensaje("reportesMesa.error.proceso.activo"));
		}
	}

	public void cambiarCantonDocumental() {
		parroquiaDocumentalId = null;
		cargarResumenesDocumentales();
	}

	public void cambiarParroquiaDocumental() {
		cargarResumenesDocumentales();
	}

	public void cambiarEstadoDocumental() {
		cargarResumenesDocumentales();
	}

	public void buscarDocumentos() {
		cargarResumenesDocumentales();
	}

	public void limpiarFiltrosDocumentales() {
		cantonDocumentalId = null;
		parroquiaDocumentalId = null;
		estadoDocumental = null;
		busquedaDocumental = null;
		cargarResumenesDocumentales();
	}

	public void generarDocumentoMesa(Integer mesa, Integer recinto, String tipo, boolean regenerar) {
		try {
			TipoDocumentoMesa tipoDocumento = TipoDocumentoMesa.valueOf(tipo);
			DocumentoDTO documento = reporteMesaService.generarDocumentoMesa(tipoDocumento, regenerar, procesoId,
					recinto, mesa, personaId(), presidenteRestringido);
			procesoBean.okActivityRegister((regenerar ? "REGENERA " : "GENERA ") + tipoDocumento.name(),
					documento.getCodigo());
			actualizarResumenDocumental(mesa);
			JsfUtil.addSuccessMessage(Constantes
					.getMensaje(regenerar ? "reportesMesa.documento.regenerado" : "reportesMesa.documento.generado"));
		} catch (IllegalArgumentException e) {
			JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.error.documento.no.generable"));
		} catch (NegocioException e) {
			JsfUtil.addErrorMessage(e.getMessage());
		} catch (Exception e) {
			log.error("ERROR GENERAR DOCUMENTO DE MESA", e);
			JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.error.almacenar"));
		}
	}

	/**
	 * Genera de una sola acción los documentos que faltan en la mesa.
	 *
	 * <p>No cambia ninguna regla: recorre solo los tipos generables y delega en el
	 * mismo {@code generarDocumentoMesa} del servicio que usa la acción individual,
	 * que revalida dependencias y permisos. Los documentos que ya existen no se
	 * regeneran (no se crean versiones nuevas ni se altera el QR) y los bloqueados
	 * se omiten conservando su motivo.
	 */
	public void generarDocumentosMesa(Integer mesa, Integer recinto) {
		List<String> generados = new ArrayList<>();
		List<String> existentes = new ArrayList<>();
		List<String> omitidos = new ArrayList<>();
		for (TipoDocumentoMesa tipo : TipoDocumentoMesa.values()) {
			if (!tipo.isDocumentoGenerable()) {
				continue;
			}
			EstadoDocumentoMesaDTO estado = estadoDocumental(mesa, tipo);
			if (estado == null) {
				continue;
			}
			String nombre = nombreCorto(tipo);
			if (estado.isPuedeVisualizar() || estado.isPuedeRegenerar()) {
				existentes.add(nombre);
				continue;
			}
			if (!estado.isPuedeGenerar()) {
				omitidos.add(motivoOmision(nombre, estado.getMotivoBloqueo()));
				continue;
			}
			try {
				DocumentoDTO documento = reporteMesaService.generarDocumentoMesa(tipo, false, procesoId, recinto, mesa,
						personaId(), presidenteRestringido);
				procesoBean.okActivityRegister("GENERA " + tipo.name(), documento.getCodigo());
				generados.add(nombre);
			} catch (NegocioException e) {
				omitidos.add(motivoOmision(nombre, e.getMessage()));
			} catch (Exception e) {
				log.error("ERROR GENERAR DOCUMENTOS DE MESA {}", mesa, e);
				omitidos.add(motivoOmision(nombre, Constantes.getMensaje("reportesMesa.error.almacenar")));
			}
		}
		actualizarResumenDocumental(mesa);
		informarGeneracionConjunta(generados, existentes, omitidos);
	}

	/** Estado ya calculado por el servicio de disponibilidad para esa mesa y tipo. */
	private EstadoDocumentoMesaDTO estadoDocumental(Integer mesa, TipoDocumentoMesa tipo) {
		for (MesaDocumentosDTO fila : resumenesDocumentales) {
			if (fila.getMesaId() != null && fila.getMesaId().equals(mesa)) {
				return fila.getEstados().get(tipo.name());
			}
		}
		return null;
	}

	private String nombreCorto(TipoDocumentoMesa tipo) {
		return switch (tipo) {
		case PADRON_MESA -> Constantes.getMensaje("reportesMesa.documento.padron.corto");
		case ACTA_PARCIAL -> Constantes.getMensaje("reportesMesa.documento.acta.corto");
		case CERTIFICADOS_VOTACION -> Constantes.getMensaje("reportesMesa.documento.certificados.corto");
		case ACTA_FISICA_ESCRUTINIO -> Constantes.getMensaje("reportesMesa.documento.actaFisica.corto");
		case DESIGNACION_MJRV -> Constantes.getMensaje("reportesMesa.documento.mjrv.corto");
		};
	}

	private String motivoOmision(String nombre, String motivo) {
		return motivo == null || motivo.isBlank() ? nombre : nombre + ": " + motivo;
	}

	private void informarGeneracionConjunta(List<String> generados, List<String> existentes, List<String> omitidos) {
		if (!generados.isEmpty()) {
			JsfUtil.addSuccessMessage(Constantes.getMensaje("reportesMesa.generar.todos.generados",
					String.join(", ", generados)));
		}
		if (!existentes.isEmpty()) {
			JsfUtil.addInfoMessage(Constantes.getMensaje("reportesMesa.generar.todos.existentes",
					String.join(", ", existentes)));
		}
		if (!omitidos.isEmpty()) {
			JsfUtil.addWarningMessage(Constantes.getMensaje("reportesMesa.generar.todos.omitidos",
					String.join(" | ", omitidos)));
		}
		if (generados.isEmpty() && existentes.isEmpty() && omitidos.isEmpty()) {
			JsfUtil.addInfoMessage(Constantes.getMensaje("reportesMesa.generar.todos.sin.documentos"));
		}
	}

	/**
	 * Descarga en un ZIP los documentos vigentes de la mesa.
	 *
	 * <p>Cada archivo se resuelve con el mismo {@code obtenerDocumentoActivo} de la
	 * descarga individual, que valida dependencias, acceso y disponibilidad. Un
	 * documento bloqueado o inexistente simplemente no se incluye: no se fuerza
	 * ninguna regla ni se generan documentos.
	 */
	public StreamedContent descargarDocumentosMesa(Integer mesa, Integer recinto, String nombreMesa) {
		List<String> incluidos = new ArrayList<>();
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(salida, StandardCharsets.UTF_8)) {
			for (TipoDocumentoMesa tipo : TipoDocumentoMesa.values()) {
				if (!tipo.isDocumentoConsultable()) {
					continue;
				}
				try {
					DocumentoDTO documento = reporteMesaService.obtenerDocumentoActivo(tipo, procesoId, recinto, mesa,
							personaId(), presidenteRestringido);
					byte[] contenido;
					try (InputStream entrada = RepositorioDocumentos.abrirLectura(documento.getPath())) {
						contenido = entrada.readAllBytes();
					}
					zip.putNextEntry(new ZipEntry(nombreEnZip(tipo, documento)));
					zip.write(contenido);
					zip.closeEntry();
					incluidos.add(nombreCorto(tipo));
				} catch (NegocioException e) {
					log.debug("Documento {} no disponible para la mesa {}: {}", tipo, mesa, e.getMessage());
				}
			}
		} catch (Exception e) {
			log.error("ERROR AL EMPAQUETAR DOCUMENTOS DE LA MESA {}", mesa, e);
			JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.descargar.todos.error"));
			return null;
		}
		if (incluidos.isEmpty()) {
			JsfUtil.addWarningMessage(Constantes.getMensaje("reportesMesa.descargar.todos.sin.documentos"));
			return null;
		}
		procesoBean.okActivityRegister("DESCARGA DOCUMENTOS DE MESA " + (nombreMesa == null ? mesa : nombreMesa),
				String.join(", ", incluidos));
		byte[] paquete = salida.toByteArray();
		return DefaultStreamedContent.builder().contentType("application/zip")
				.name(nombrePaquete(nombreMesa)).contentLength((long) paquete.length)
				.stream(() -> new ByteArrayInputStream(paquete)).build();
	}

	/** Nombre legible dentro del ZIP, conservando la extensión real del archivo. */
	private String nombreEnZip(TipoDocumentoMesa tipo, DocumentoDTO documento) {
		String base = nombreCorto(tipo).replace(' ', '-');
		String extension = documento.getExtension() == null ? "" : documento.getExtension().trim();
		if (!extension.isEmpty() && !extension.startsWith(".")) {
			extension = "." + extension;
		}
		if (extension.isEmpty() && documento.getNombre() != null && documento.getNombre().contains(".")) {
			extension = documento.getNombre().substring(documento.getNombre().lastIndexOf('.'));
		}
		String codigo = documento.getCodigo() == null || documento.getCodigo().isBlank() ? ""
				: "-" + documento.getCodigo();
		return (base + codigo + extension).replaceAll("[\\\\/:*?\"<>|]", "_");
	}

	private String nombrePaquete(String nombreMesa) {
		String mesa = nombreMesa == null || nombreMesa.isBlank() ? "mesa" : nombreMesa.trim().replace(' ', '-');
		return ("documentos-" + mesa + ".zip").replaceAll("[\\\\/:*?\"<>|]", "_");
	}

	public StreamedContent descargarDocumentoMesa(Integer mesa, Integer recinto, String tipo) {
		try {
			DocumentoDTO documento = reporteMesaService.obtenerDocumentoActivo(TipoDocumentoMesa.valueOf(tipo),
					procesoId, recinto, mesa, personaId(), presidenteRestringido);
			return documentoBean.obtenerArchivo(documento);
		} catch (Exception e) {
			log.warn("No fue posible descargar documento de mesa {}", mesa);
			JsfUtil.addErrorMessage(e instanceof NegocioException ? e.getMessage()
					: Constantes.getMensaje("reportesMesa.error.documento.no.disponible"));
			return null;
		}
	}

	public void visualizarActaFisica(Integer mesa, Integer recinto) {
		actaFisicaVisualizada = null;
		contenidoActaFisica = null;
		mesaActaFinalId = null;
		recintoActaFinalId = null;
		puedeValidarActaFinal = false;
		motivoActaFinal = null;
		revisionActaFinal = new ec.com.antenasur.dto.RevisionActaFinalDTO();
		resultadosActaFinal = new ArrayList<>();
		try {
			actaFisicaVisualizada = reporteMesaService.obtenerDocumentoActivo(TipoDocumentoMesa.ACTA_FISICA_ESCRUTINIO,
					procesoId, recinto, mesa, personaId(), presidenteRestringido);
			ReporteMesaDTO datos = reporteMesaService.consultar(procesoId, recinto, mesa, personaId(),
					presidenteRestringido);
			resultadosActaFinal = datos != null && datos.getEscrutinios() != null
					? new ArrayList<>(datos.getEscrutinios())
					: new ArrayList<>();
			mesaActaFinalId = mesa;
			recintoActaFinalId = recinto;
			revisionActaFinal.setResultados(resultadosActaFinal);
			var imagen = documentoBean.obtenerArchivo(actaFisicaVisualizada);
			if (imagen == null)
				throw new NegocioException(Constantes.getMensaje("reportesMesa.actaFisica.error.visualizar"));
			try (var contenido = imagen.getStream().get()) {
				contenidoActaFisica = contenido.readAllBytes();
			}
			try {
				disponibilidadDocumental.validarFinal(procesoId, mesa);
				puedeValidarActaFinal = !ActaFisicaEscrutinioService.VALIDADA
						.equals(actaFisicaVisualizada.getEstadoRevision());
			} catch (NegocioException e) {
				motivoActaFinal = e.getMessage();
			}
			org.primefaces.PrimeFaces.current().ajax().addCallbackParam("documentoDisponible", true);
		} catch (NegocioException e) {
			actaFisicaVisualizada = null;
			resultadosActaFinal = new ArrayList<>();
			JsfUtil.addWarningMessage(e.getMessage());
		} catch (Exception e) {
			actaFisicaVisualizada = null;
			log.warn("No fue posible visualizar el acta fisica de la mesa {}", mesa, e);
			JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.actaFisica.error.visualizar"));
		}
	}

	public void visualizarJunta(Integer mesa, Integer recinto) {
		juntaVisualizada = new ArrayList<>();
		try {
			juntaVisualizada = reporteMesaService.visualizarJunta(procesoId, recinto, mesa);
			org.primefaces.PrimeFaces.current().ajax().addCallbackParam("documentoDisponible", true);
		} catch (NegocioException e) {
			JsfUtil.addWarningMessage(e.getMessage());
		}
	}

	/**
	 * Renderiza la instantanea autorizada sin retener entidades ni consultar JPA
	 * desde un getter.
	 */
	public StreamedContent getImagenActaFisica() {
		if (contenidoActaFisica == null)
			return null;
		return org.primefaces.model.DefaultStreamedContent.builder().contentType("image/jpeg")
				.stream(() -> new java.io.ByteArrayInputStream(contenidoActaFisica)).build();
	}

	public boolean isPuedeValidarActaFinal() {
		return puedeValidarActaFinal;
	}

	public long getSubtotalValidosActaFinal() {
		return revisionActaFinal.getValidos();
	}

	public long getVotosBlancosActaFinal() {
		return revisionActaFinal.getBlancos();
	}

	public long getVotosNulosActaFinal() {
		return revisionActaFinal.getNulos();
	}

	public long getTotalVotosActaFinal() {
		return revisionActaFinal.getTotal();
	}

	public boolean isResultadosActaFinalCuadrados() {
		return revisionActaFinal.isCuadrada();
	}

	public void guardarActaFinal() {
		try {
			if (!isPuedeValidarActaFinal() || actaFisicaVisualizada == null || mesaActaFinalId == null) {
				throw new NegocioException(Constantes.getMensaje("reportesMesa.actaFisica.error.validar"));
			}
			actaFisicaEscrutinioService.validarActaFinal(actaFisicaVisualizada.getId(), mesaActaFinalId, procesoId,
					revisionActaFinal);
			actaFisicaVisualizada.setEstadoRevision(ActaFisicaEscrutinioService.VALIDADA);
			puedeValidarActaFinal = false;
			actualizarResumenDocumental(mesaActaFinalId);
			JsfUtil.addSuccessMessage(Constantes.getMensaje("reportesMesa.actaFisica.validada"));
		} catch (NegocioException e) {
			JsfUtil.addErrorMessage(e.getMessage());
		} catch (Exception e) {
			log.error("ERROR AL VALIDAR ACTA FINAL DE MESA {}", mesaActaFinalId, e);
			JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.actaFisica.error.validar"));
		}
	}

	public void cambiarResultadosActaFinal() {
		revisionActaFinal.setRevisada(false);
	}

	public void actualizarDisponibilidadDocumental() {
		int primera = primeraFilaDocumental;
		cargarResumenesDocumentales();
		if (primera < resumenesDocumentales.size())
			primeraFilaDocumental = primera;
	}

	private void cargarResumenesDocumentales() {
		primeraFilaDocumental = 0;
		if (procesoId == null) {
			resumenesDocumentales = new ArrayList<>();
			return;
		}
		try {
			resumenesDocumentales = new ArrayList<>(reporteMesaService.listarResumenDocumentos(procesoId, null, null,
					personaId(), presidenteRestringido));
			cargarOpcionesDocumentales();
			resumenesDocumentales
					.removeIf(fila -> (cantonDocumentalId != null && !cantonDocumentalId.equals(fila.getCantonId()))
							|| (parroquiaDocumentalId != null && !parroquiaDocumentalId.equals(fila.getParroquiaId())));
			if (estadoDocumental != null) {
				resumenesDocumentales.removeIf(fila -> !estadoDocumental.equals(fila.getEstadoDocumental()));
			}
			if (busquedaDocumental != null && !busquedaDocumental.isBlank()) {
				String texto = busquedaDocumental.trim().toLowerCase(java.util.Locale.ROOT);
				resumenesDocumentales.removeIf(
						fila -> !(texto(fila.getRecinto()).contains(texto) || texto(fila.getMesa()).contains(texto)));
			}
		} catch (NegocioException e) {
			resumenesDocumentales = new ArrayList<>();
			JsfUtil.addWarningMessage(e.getMessage());
		}
	}

	private void actualizarResumenDocumental(Integer mesaId) {
		MesaDocumentosDTO actualizado = reporteMesaService.consultarResumenDocumento(procesoId, mesaId, personaId(),
				presidenteRestringido);
		if (actualizado == null)
			return;
		for (int i = 0; i < resumenesDocumentales.size(); i++) {
			if (mesaId.equals(resumenesDocumentales.get(i).getMesaId())) {
				if (estadoDocumental != null && !estadoDocumental.equals(actualizado.getEstadoDocumental()))
					resumenesDocumentales.remove(i);
				else
					resumenesDocumentales.set(i, actualizado);
				if (primeraFilaDocumental >= resumenesDocumentales.size())
					primeraFilaDocumental = 0;
				return;
			}
		}
	}

	private void cargarOpcionesDocumentales() {
		java.util.Map<Integer, String> cantones = new java.util.TreeMap<>();
		java.util.Map<Integer, String> parroquias = new java.util.TreeMap<>();
		for (MesaDocumentosDTO fila : resumenesDocumentales) {
			if (fila.getCantonId() != null)
				cantones.put(fila.getCantonId(), fila.getCanton());
			if (fila.getParroquiaId() != null
					&& (cantonDocumentalId == null || cantonDocumentalId.equals(fila.getCantonId())))
				parroquias.put(fila.getParroquiaId(), fila.getParroquia());
		}
		cantonesDocumentales = cantones.entrySet().stream().map(e -> new OpcionPadronDTO(e.getKey(), e.getValue()))
				.toList();
		parroquiasDocumentales = parroquias.entrySet().stream().map(e -> new OpcionPadronDTO(e.getKey(), e.getValue()))
				.toList();
	}

	private String texto(String valor) {
		return valor == null ? "" : valor.toLowerCase(java.util.Locale.ROOT);
	}

	public void cargarRecintos() {
		recintoId = null;
		mesaId = null;
		reporte = null;
		recintos = new ArrayList<>(reporteMesaService.listarRecintos(procesoId, personaId(), presidenteRestringido));
		mesas = new ArrayList<>();
		if (presidenteRestringido && recintos.size() == 1) {
			recintoId = recintos.get(0).getId();
			cargarMesas();
		}
	}

	public void cargarMesas() {
		mesaId = null;
		reporte = null;
		mesas = new ArrayList<>(
				reporteMesaService.listarMesas(procesoId, recintoId, personaId(), presidenteRestringido));
		if (presidenteRestringido && mesas.size() == 1) {
			mesaId = mesas.get(0).getId();
			consultar();
		}
	}

	public void consultar() {
		certificados = null;
		reporte = null;
		if (procesoId == null || recintoId == null || mesaId == null) {
			JsfUtil.addWarningMessage(Constantes.getMensaje("reportesMesa.error.seleccion"));
			return;
		}
		try {
			reporte = reporteMesaService.consultar(procesoId, recintoId, mesaId, personaId(), presidenteRestringido);
		} catch (NegocioException e) {
			JsfUtil.addErrorMessage(e.getMessage());
		} catch (Exception e) {
			log.error("ERROR CONSULTAR REPORTE DE MESA", e);
			JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.error.consulta"));
		}
	}

	public void generarActaParcial() {
		try {
			DocumentoDTO documento = reporteMesaService.generarActaParcial(procesoId, recintoId, mesaId, personaId(),
					presidenteRestringido);
			consultar();
			procesoBean.okActivityRegister("GENERA ACTA PARCIAL DE ESCRUTINIO", documento.getCodigo());
			JsfUtil.addSuccessMessage(Constantes.getMensaje("reportesMesa.exito.acta", documento.getNombre()));
		} catch (NegocioException e) {
			JsfUtil.addErrorMessage(e.getMessage());
		} catch (Exception e) {
			log.error("ERROR GENERAR ACTA PARCIAL", e);
			JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.error.generar.acta"));
		}
	}

	public void generarPadron() {
		try {
			DocumentoDTO documento = reporteMesaService.generarPadron(procesoId, recintoId, mesaId, personaId(),
					presidenteRestringido);
			consultar();
			procesoBean.okActivityRegister("GENERA PADRON ELECTORAL DE MESA", documento.getCodigo());
			JsfUtil.addSuccessMessage(Constantes.getMensaje("reportesMesa.exito.padron", documento.getNombre()));
		} catch (NegocioException e) {
			JsfUtil.addErrorMessage(e.getMessage());
		} catch (Exception e) {
			log.error("ERROR GENERAR PADRON DE MESA", e);
			JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.error.generar.padron"));
		}
	}

	public void generarCertificados() {
		certificados = null;
		try {
			certificados = reporteMesaService.generarCertificados(procesoId, recintoId, mesaId, personaId(),
					presidenteRestringido);
			reporte.getDocumentos().removeIf(d -> certificados.getTipoDocumentoId().equals(d.getTipoDocumentoId()));
			reporte.getDocumentos().add(0, certificados);
			procesoBean.okActivityRegister("GENERA CERTIFICADOS DE VOTACION", certificados.getCodigo());
			JsfUtil.addSuccessMessage(Constantes.getMensaje("reportesMesa.certificados.exito"));
		} catch (NegocioException e) {
			JsfUtil.addErrorMessage(e.getMessage());
		} catch (Exception e) {
			log.error("ERROR GENERAR CERTIFICADOS DE VOTACION", e);
			JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.certificados.error"));
		}
	}

	private Integer personaId() {
		return loginBean != null && loginBean.getUsuario() != null ? loginBean.getUsuario().getPersonaId() : null;
	}

	private boolean tieneRol(String rol) {
		return loginBean != null && loginBean.getRoles() != null && loginBean.getRoles().contains(rol);
	}
}
