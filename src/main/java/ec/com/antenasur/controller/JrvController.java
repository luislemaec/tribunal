package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.event.SelectEvent;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.CatalogoGeneralDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.PadronDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.OpcionPadronDTO;
import ec.com.antenasur.dto.EstadoJuntaDTO;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.service.tec.CatalogoGeneralService;
import ec.com.antenasur.service.tec.MesaService;
import ec.com.antenasur.service.tec.MiembroJRVService;
import ec.com.antenasur.service.tec.PadronService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class JrvController implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final String CATALOGO_AUTORIDADES_MESA = "CARGO AUTORIDADES MESA";
	private EstadoJuntaDTO estadoJunta = new EstadoJuntaDTO(false, 0, MiembroJRVService.DIGNIDADES_OBLIGATORIAS);

	@Inject
	private LoginBean loginBean;

	@Inject
	private MiembroJRVService mjrvService;

	@Inject
	private CatalogoGeneralService catalogoService;

	@Inject
	private ProcesoElectoralService procesoElectoralService;

	@Inject
	private MesaService mesaService;

	@Inject
	private PadronService padronService;

	@Getter
	@Setter
	private ProcesoElectoralDTO procesoSeleccionado;

	@Getter
	@Setter
	private RecintoDTO recintoSeleccionado;

	@Getter
	@Setter
	private MesaDTO mesaSeleccionada;

	@Getter
	@Setter
	private Integer iglesiaPersonaSeleccionadaId;

	@Getter
	@Setter
	private Integer cargoSeleccionadoId;

	@Getter
	@Setter
	private MiembroJRVDTO mjrvSeleccionado;

	@Getter
	@Setter
	private List<ProcesoElectoralDTO> procesos;

	@Getter
	private List<MesaDTO> mesasDisponibles = new ArrayList<>();
	@Getter
	private List<MesaDTO> mesasFiltradas = new ArrayList<>();
	@Getter
	private java.util.Map<Integer, ec.com.antenasur.dto.ResumenMesaJrvDTO> resumenMesas = new java.util.HashMap<>();
	@Getter
	private List<OpcionPadronDTO> cantonesMesas = new ArrayList<>();
	@Getter
	private List<OpcionPadronDTO> parroquiasMesas = new ArrayList<>();
	@Getter
	@Setter
	private Integer cantonMesaId;
	@Getter
	@Setter
	private Integer parroquiaMesaId;
	@Getter
	@Setter
	private String busquedaMesa;
	@Getter
	@Setter
	private int primeraFilaMesa;

	@Getter
	@Setter
	private List<PadronDTO> personasDisponibles;

	@Getter
	@Setter
	private List<IglesiaDTO> iglesiasAsignadas;

	@Getter
	@Setter
	private List<MiembroJRVDTO> listaMJRV, listaMJRVSeleccionados;

	@Getter
	@Setter
	private List<CatalogoGeneralDTO> cargos;

	@Getter
	private boolean restringidoAMesaAsignada;

	@Getter
	private boolean juntaRegistradaComoCompletada;

	@PostConstruct
	private void init() {
		procesoSeleccionado = procesoElectoralService.getActivoDTO();
		if (procesoSeleccionado == null) {
			procesoSeleccionado = new ProcesoElectoralDTO();
			procesos = new ArrayList<>();
			JsfUtil.addWarningMessageFromBundle("mjrv.mensaje.sin.proceso.activo");
		} else {
			procesos = new ArrayList<>(Collections.singletonList(procesoSeleccionado));
		}
		cargarMesasSeleccionables();
		personasDisponibles = new ArrayList<>();
		iglesiasAsignadas = new ArrayList<>();
		listaMJRV = new ArrayList<>();
		cargos = catalogoService.listarDTOsPorNombrePadre(CATALOGO_AUTORIDADES_MESA);
		recintoSeleccionado = new RecintoDTO();
		mesaSeleccionada = new MesaDTO();
		aplicarRestriccionMesaAsignada();
	}

	public void onProcesoChange() {
		limpiarSeleccionMesa();
		cantonMesaId = null;
		parroquiaMesaId = null;
		busquedaMesa = null;
		cargarMesasSeleccionables();
		aplicarRestriccionMesaAsignada();
	}

	public void cambiarCantonMesa() {
		parroquiaMesaId = null;
		filtrarMesasSeleccionables();
	}

	public void cambiarParroquiaMesa() {
		filtrarMesasSeleccionables();
	}

	public void buscarMesas() {
		filtrarMesasSeleccionables();
	}

	public void limpiarFiltrosMesas() {
		cantonMesaId = null;
		parroquiaMesaId = null;
		busquedaMesa = null;
		filtrarMesasSeleccionables();
	}

	public void seleccionarMesa(MesaDTO mesa) {
		if (mesa == null || mesa.getId() == null)
			return;
		if (restringidoAMesaAsignada && (mesaSeleccionada == null || !mesa.getId().equals(mesaSeleccionada.getId())))
			return;
		limpiarJuntaActual();
		mesaSeleccionada = mesaService.obtenerDetalleDTOPorId(mesa.getId());
		recintoSeleccionado = mesaSeleccionada != null && mesaSeleccionada.getRecinto() != null
				? mesaSeleccionada.getRecinto()
				: new RecintoDTO();
		cargarDatosMesaSeleccionada();
	}

	/**
	 * Adapta la selección de fila de PrimeFaces al flujo de selección de mesa
	 * existente.
	 */
	public void seleccionarMesaDesdeTabla(SelectEvent<MesaDTO> evento) {
		seleccionarMesa(evento != null ? evento.getObject() : null);
	}

	public void designarMiembro() {
		try {
			Integer procesoId = procesoSeleccionado != null ? procesoSeleccionado.getId() : null;
			Integer mesaId = mesaSeleccionada != null ? mesaSeleccionada.getId() : null;
			mjrvService.designarMiembro(iglesiaPersonaSeleccionadaId, mesaId, procesoId, cargoSeleccionadoId);
			JsfUtil.addSuccessMessageFromBundle("mjrv.mensaje.asignado");
			cargarDatosMesaSeleccionada();
		} catch (NegocioException e) {
			JsfUtil.addErrorMessage(e.getMessage());
		} catch (Exception e) {
			log.error("Error al designar miembro JRV", e);
			JsfUtil.addErrorMessageFromBundle("mjrv.mensaje.error");
		}
	}

	public void quitarMiembro(MiembroJRVDTO miembro) {
		if (miembro == null || miembro.getId() == null) {
			return;
		}
		try {
			if (mjrvService.eliminarPorId(miembro.getId()) != null) {
				JsfUtil.addSuccessMessageFromBundle("mjrv.mensaje.retirado");
			}
			cargarDatosMesaSeleccionada();
		} catch (NegocioException e) {
			JsfUtil.addErrorMessage(e.getMessage());
		} catch (Exception e) {
			log.error("Error al retirar miembro JRV", e);
			JsfUtil.addErrorMessageFromBundle("mjrv.mensaje.error");
		}
	}

	public void seleccionarPersona(PadronDTO padron) {
		if (padron != null && padron.getIglesiaPersona() != null) {
			iglesiaPersonaSeleccionadaId = padron.getIglesiaPersona().getId();
		}
	}

	public void completarJunta() {
		try {
			Integer procesoId = procesoSeleccionado != null ? procesoSeleccionado.getId() : null;
			Integer mesaId = mesaSeleccionada != null ? mesaSeleccionada.getId() : null;
			MiembroJRVDTO presidente = mjrvService.completarJunta(mesaId, procesoId);
			if (presidente != null) {
				JsfUtil.addSuccessMessageFromBundle("mjrv.mensaje.completada");
			}
			mesaSeleccionada = mesaService.obtenerDTOPorId(mesaId);
			cargarDatosMesaSeleccionada();
		} catch (NegocioException e) {
			JsfUtil.addErrorMessage(e.getMessage());
		} catch (Exception e) {
			log.error("Error al completar junta JRV", e);
			JsfUtil.addErrorMessageFromBundle("mjrv.mensaje.error");
		}
	}

	public boolean isEdicionBloqueada() {
		return juntaRegistradaComoCompletada;
	}

	public boolean isAsignacionBloqueada() {
		return !isMesaSeleccionadaValida() || isEdicionBloqueada() || getTotalIglesiasAsignadas() == 0;
	}

	public boolean isPuedeCompletarJunta() {
		return isMesaSeleccionadaValida() && isJuntaCompleta() && !juntaRegistradaComoCompletada;
	}

	public int getTotalIglesiasAsignadas() {
		return iglesiasAsignadas != null ? iglesiasAsignadas.size() : 0;
	}

	public int getTotalDignidadesObligatoriasAsignadas() {
		return estadoJunta.getDignidadesAsignadas();
	}

	public String getIndicadorDignidadesObligatorias() {
		return getTotalDignidadesObligatoriasAsignadas() + " de " + MiembroJRVService.DIGNIDADES_OBLIGATORIAS.size();
	}

	public String getEstadoJuntaTexto() {
		if (juntaRegistradaComoCompletada) {
			return JsfUtil.getProperty("mjrv.estado.completa", true);
		}
		if (isJuntaCompleta()) {
			return JsfUtil.getProperty("mjrv.estado.lista.completar", true);
		}
		if (getTotalMiembrosDesignados() > 0) {
			return JsfUtil.getProperty("mjrv.estado.incompleta", true);
		}
		return JsfUtil.getProperty("mjrv.estado.pendiente", true);
	}

	public String getEstadoJuntaSeverity() {
		if (juntaRegistradaComoCompletada) {
			return "success";
		}
		if (isJuntaCompleta()) {
			return "info";
		}
		return getTotalMiembrosDesignados() > 0 ? "warning" : "secondary";
	}

	public String getEstadoJuntaIcon() {
		if (juntaRegistradaComoCompletada) {
			return "pi pi-check";
		}
		return isJuntaCompleta() ? "pi pi-flag" : "pi pi-clock";
	}

	public boolean isMesaSeleccionadaValida() {
		return mesaSeleccionada != null && mesaSeleccionada.getId() != null;
	}

	public int getTotalPersonasDisponibles() {
		return personasDisponibles != null ? personasDisponibles.size() : 0;
	}

	public int getTotalMiembrosDesignados() {
		return listaMJRV != null ? listaMJRV.size() : 0;
	}

	public int getTotalDignidadesPendientes() {
		return getDignidadesPendientes().size();
	}

	public List<CatalogoGeneralDTO> getDignidadesPendientes() {
		List<CatalogoGeneralDTO> resultado = new ArrayList<>();
		if (cargos == null) {
			return resultado;
		}
		Set<Integer> ocupadas = obtenerCargoIdsOcupados();
		for (CatalogoGeneralDTO cargo : cargos) {
			if (cargo != null && cargo.getId() != null && !ocupadas.contains(cargo.getId())) {
				resultado.add(cargo);
			}
		}
		return resultado;
	}

	public boolean isJuntaCompleta() {
		return estadoJunta.isCompleta();
	}

	public String getMotivoJuntaIncompleta() {
		return Constantes.getMensaje("mjrv.validacion.conformacion") + (estadoJunta.getCargosFaltantes().isEmpty() ? ""
				: " " + Constantes.getMensaje("reportesMesa.regla.cargos",
						String.join(", ", estadoJunta.getCargosFaltantes())));
	}

	private void cargarDatosMesaSeleccionada() {
		limpiarJuntaActual();
		if (!isMesaSeleccionadaValida() || procesoSeleccionado == null || procesoSeleccionado.getId() == null) {
			return;
		}
		List<Integer> mesaIds = new ArrayList<>();
		mesaIds.add(mesaSeleccionada.getId());
		List<PadronDTO> padronMesa = padronService.listarDTOsPorMesaIdsYProceso(mesaIds, procesoSeleccionado.getId());
		listaMJRV = mjrvService.listarDTOsPorMesaProceso(mesaSeleccionada.getId(), procesoSeleccionado.getId());
		estadoJunta = mjrvService.consultarEstadoJunta(mesaSeleccionada.getId(), procesoSeleccionado.getId());
		iglesiasAsignadas = obtenerIglesiasAsignadas(padronMesa);
		resumenMesas.putAll(mjrvService.consultarResumenMesas(procesoSeleccionado.getId(), mesaIds));
		org.primefaces.PrimeFaces.current().ajax().update("frmMJRV:tabsMJRV:tblMesas");
		Set<Integer> designadas = mjrvService.obtenerIglesiaPersonaIdsDesignadas(procesoSeleccionado.getId());
		personasDisponibles = new ArrayList<>();
		for (PadronDTO padron : padronMesa) {
			if (padron.getIglesiaPersona() != null && padron.getIglesiaPersona().getId() != null
					&& Boolean.TRUE.equals(padron.getIglesiaPersona().getHabilitadoPadron())
					&& !designadas.contains(padron.getIglesiaPersona().getId())) {
				personasDisponibles.add(padron);
			}
		}
		juntaRegistradaComoCompletada = mjrvService.juntaCompletadaRegistrada(mesaSeleccionada.getId(),
				procesoSeleccionado.getId());
		iglesiaPersonaSeleccionadaId = null;
		cargoSeleccionadoId = null;
	}

	private void aplicarRestriccionMesaAsignada() {
		restringidoAMesaAsignada = false;
		Integer personaId = loginBean != null && loginBean.getUsuario() != null ? loginBean.getUsuario().getPersonaId()
				: null;
		Integer procesoId = procesoSeleccionado != null ? procesoSeleccionado.getId() : null;
		MiembroJRVDTO designacion = mjrvService.obtenerDesignacionPorPersonaProceso(personaId, procesoId);
		if (designacion == null || designacion.getMesa() == null || designacion.getMesa().getId() == null) {
			return;
		}
		restringidoAMesaAsignada = true;
		mesaSeleccionada = designacion.getMesa();
		seleccionarMesa(designacion.getMesa());
		filtrarMesasSeleccionables();
	}

	private Set<Integer> obtenerCargoIdsOcupados() {
		Set<Integer> ids = new HashSet<>();
		if (listaMJRV == null) {
			return ids;
		}
		for (MiembroJRVDTO miembro : listaMJRV) {
			if (miembro.getCargoId() != null) {
				ids.add(miembro.getCargoId());
			}
		}
		return ids;
	}

	private void limpiarSeleccionMesa() {
		recintoSeleccionado = new RecintoDTO();
		limpiarMesaYJunta();
	}

	private void limpiarMesaYJunta() {
		mesaSeleccionada = new MesaDTO();
		limpiarJuntaActual();
	}

	private void limpiarJuntaActual() {
		estadoJunta = new EstadoJuntaDTO(false, 0, MiembroJRVService.DIGNIDADES_OBLIGATORIAS);
		personasDisponibles = new ArrayList<>();
		listaMJRV = new ArrayList<>();
		listaMJRVSeleccionados = new ArrayList<>();
		iglesiasAsignadas = new ArrayList<>();
		juntaRegistradaComoCompletada = false;
		iglesiaPersonaSeleccionadaId = null;
		cargoSeleccionadoId = null;
	}

	private List<IglesiaDTO> obtenerIglesiasAsignadas(List<PadronDTO> padrones) {
		List<IglesiaDTO> resultado = new ArrayList<>();
		Set<Integer> ids = new HashSet<>();
		if (padrones == null) {
			return resultado;
		}
		for (PadronDTO padron : padrones) {
			if (padron.getIglesiaPersona() != null && padron.getIglesiaPersona().getIglesia() != null
					&& padron.getIglesiaPersona().getIglesia().getId() != null
					&& ids.add(padron.getIglesiaPersona().getIglesia().getId())) {
				resultado.add(padron.getIglesiaPersona().getIglesia());
			}
		}
		return resultado;
	}

	private void cargarMesasSeleccionables() {
		if (procesoSeleccionado == null || procesoSeleccionado.getId() == null) {
			mesasDisponibles = new ArrayList<>();
			mesasFiltradas = new ArrayList<>();
			resumenMesas.clear();
			cantonesMesas = new ArrayList<>();
			parroquiasMesas = new ArrayList<>();
			return;
		}
		mesasDisponibles = new ArrayList<>(mesaService.listarDTOsActivasConUbicacion());
		resumenMesas = mjrvService.consultarResumenMesas(procesoSeleccionado.getId(),
				mesasDisponibles.stream().map(MesaDTO::getId).toList());
		filtrarMesasSeleccionables();
	}

	private void cargarOpcionesMesas() {
		java.util.Map<Integer, String> cantones = new java.util.TreeMap<>();
		java.util.Map<Integer, String> parroquias = new java.util.TreeMap<>();
		for (MesaDTO mesa : mesasDisponibles) {
			RecintoDTO recinto = mesa.getRecinto();
			if (recinto == null)
				continue;
			if (recinto.getCantonId() != null)
				cantones.put(recinto.getCantonId(), recinto.getCantonNombre());
			if (recinto.getUbicacionId() != null
					&& (cantonMesaId == null || cantonMesaId.equals(recinto.getCantonId())))
				parroquias.put(recinto.getUbicacionId(), recinto.getUbicacionNombre());
		}
		cantonesMesas = cantones.entrySet().stream().map(e -> new OpcionPadronDTO(e.getKey(), e.getValue())).toList();
		parroquiasMesas = parroquias.entrySet().stream().map(e -> new OpcionPadronDTO(e.getKey(), e.getValue()))
				.toList();
	}

	private void filtrarMesasSeleccionables() {
		primeraFilaMesa = 0;
		cargarOpcionesMesas();
		String texto = busquedaMesa == null ? "" : busquedaMesa.trim().toLowerCase(java.util.Locale.ROOT);
		Integer mesaRestringida = restringidoAMesaAsignada && mesaSeleccionada != null ? mesaSeleccionada.getId()
				: null;
		mesasFiltradas = mesasDisponibles.stream().filter(mesa -> {
			RecintoDTO recinto = mesa.getRecinto();
			if (recinto == null)
				return false;
			if (mesaRestringida != null && !mesaRestringida.equals(mesa.getId()))
				return false;
			if (cantonMesaId != null && !cantonMesaId.equals(recinto.getCantonId()))
				return false;
			if (parroquiaMesaId != null && !parroquiaMesaId.equals(recinto.getUbicacionId()))
				return false;
			return texto.isEmpty() || contiene(mesa.getNombre(), texto) || contiene(recinto.getNombre(), texto)
					|| contiene(recinto.getCantonNombre(), texto) || contiene(recinto.getUbicacionNombre(), texto);
		}).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
	}

	private static boolean contiene(String valor, String texto) {
		return valor != null && valor.toLowerCase(java.util.Locale.ROOT).contains(texto);
	}
}
