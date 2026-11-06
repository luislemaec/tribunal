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
	/** Recintos derivados de las mesas ya cargadas; no supone ninguna consulta extra. */
	@Getter
	private List<OpcionPadronDTO> recintosMesas = new ArrayList<>();
	@Getter
	@Setter
	private Integer cantonMesaId;
	@Getter
	@Setter
	private Integer parroquiaMesaId;
	@Getter
	@Setter
	private Integer recintoMesaId;
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

	/** Motivo de la reapertura de una junta completada (obligatorio). */
	@Getter
	@Setter
	private String motivoReapertura;

	/**
	 * «Reabrir junta»: junta completada y usuario Administrador o Tribunal. El servicio vuelve a
	 * exigir el rol, el motivo y que el escrutinio de la mesa no haya iniciado.
	 */
	public boolean isPuedeReabrirJunta() {
		if (!isMesaSeleccionadaValida() || !juntaRegistradaComoCompletada || restringidoAMesaAsignada
				|| loginBean == null || loginBean.getRoles() == null) {
			return false;
		}
		return loginBean.getRoles().contains("SITEC-" + Constantes.getRolAdministrador())
				|| loginBean.getRoles().contains("SITEC-" + Constantes.getRolTribunal());
	}

	public void prepararReapertura() {
		motivoReapertura = null;
	}

	public void reabrirJunta() {
		try {
			Integer procesoId = procesoSeleccionado != null ? procesoSeleccionado.getId() : null;
			Integer mesaId = mesaSeleccionada != null ? mesaSeleccionada.getId() : null;
			mjrvService.reabrirJunta(mesaId, procesoId, motivoReapertura, JsfUtil.getIPAddress());
			JsfUtil.addSuccessMessageFromBundle("mjrv.reabrir.ok");
			motivoReapertura = null;
			mesaSeleccionada = mesaService.obtenerDTOPorId(mesaId);
			cargarDatosMesaSeleccionada();
			org.primefaces.PrimeFaces.current().ajax().addCallbackParam("juntaReabierta", true);
		} catch (NegocioException e) {
			JsfUtil.addErrorMessage(e.getMessage());
		} catch (Exception e) {
			log.error("Error al reabrir junta JRV", e);
			JsfUtil.addErrorMessageFromBundle("mjrv.mensaje.error");
		}
	}

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
		recintoMesaId = null;
		busquedaMesa = null;
		cargarMesasSeleccionables();
		aplicarRestriccionMesaAsignada();
	}

	/** Cambiar de cantón invalida la parroquia y el recinto elegidos. */
	public void cambiarCantonMesa() {
		parroquiaMesaId = null;
		recintoMesaId = null;
		filtrarMesasSeleccionables();
	}

	/** Cambiar de parroquia invalida el recinto elegido. */
	public void cambiarParroquiaMesa() {
		recintoMesaId = null;
		filtrarMesasSeleccionables();
	}

	public void cambiarRecintoMesa() {
		filtrarMesasSeleccionables();
	}

	public void buscarMesas() {
		filtrarMesasSeleccionables();
	}

	public void limpiarFiltrosMesas() {
		cantonMesaId = null;
		parroquiaMesaId = null;
		recintoMesaId = null;
		busquedaMesa = null;
		estadoJrvFiltro = ESTADO_SIN_COMPLETAR;
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
			MiembroJRVService.ResultadoRetiro retiro = mjrvService.retirarMiembro(miembro.getId());
			if (retiro != null && retiro.miembro() != null) {
				JsfUtil.addSuccessMessageFromBundle("mjrv.mensaje.retirado");
				// Presidente retirado: se informa el acceso que perdió su cuenta.
				switch (retiro.acceso()) {
				case USUARIO -> JsfUtil.addInfoMessageFromBundle("mjrv.retiro.usuario.desactivado", retiro.usuario());
				case ROL -> JsfUtil.addInfoMessageFromBundle("mjrv.retiro.rol.retirado", retiro.usuario());
				default -> { }
				}
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

	/**
	 * Estado de la junta de la mesa seleccionada, con los datos recién consultados. Usa los
	 * mismos estados, textos, colores e íconos que la tabla de mesas y el filtro (claveEstado,
	 * severidadEstado, iconoEstado), para que la pantalla no muestre dos nombres para lo mismo.
	 */
	private String estadoJuntaActual() {
		if (juntaRegistradaComoCompletada) {
			return ESTADO_COMPLETADO;
		}
		if (isJuntaCompleta()) {
			return ESTADO_LISTA;
		}
		return getTotalMiembrosDesignados() > 0 ? ESTADO_PARCIAL : ESTADO_PENDIENTE;
	}

	public String getEstadoJuntaTexto() {
		return JsfUtil.getProperty(claveEstado(estadoJuntaActual()), true);
	}

	public String getEstadoJuntaSeverity() {
		return severidadEstado(estadoJuntaActual());
	}

	public String getEstadoJuntaIcon() {
		return iconoEstado(estadoJuntaActual());
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
		// El estado «Completado» depende del responsable (junta registrada): tras completar o
		// reabrir, la fila del listado toma el de la mesa recién consultada. La fila no desaparece
		// hasta el próximo cambio de filtro, para no perder de vista la mesa en la que se trabaja.
		if (mesasDisponibles != null) {
			for (MesaDTO mesa : mesasDisponibles) {
				if (mesa.getId() != null && mesa.getId().equals(mesaSeleccionada.getId())) {
					mesa.setResponsable(mesaSeleccionada.getResponsable());
				}
			}
		}
		recalcularAvanceJrv();
		org.primefaces.PrimeFaces.current().ajax().update("frmMJRV:tabsMJRV:tblMesas", "frmMJRV:pnlAvanceJrv");
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
			recintosMesas = new ArrayList<>();
			recalcularAvanceJrv();
			return;
		}
		mesasDisponibles = new ArrayList<>(mesaService.listarDTOsActivasConUbicacion());
		resumenMesas = mjrvService.consultarResumenMesas(procesoSeleccionado.getId(),
				mesasDisponibles.stream().map(MesaDTO::getId).toList());
		// Solo mesas del proceso: con padrón (iglesias asignadas) o con miembros ya designados, para
		// no ocultar datos. En una mesa sin padrón no se puede conformar la junta. La mesa asignada
		// al usuario restringido se conserva siempre.
		Integer mesaAsignada = restringidoAMesaAsignada && mesaSeleccionada != null ? mesaSeleccionada.getId() : null;
		mesasDisponibles.removeIf(mesa -> {
			if (mesa.getId() != null && mesa.getId().equals(mesaAsignada))
				return false;
			ec.com.antenasur.dto.ResumenMesaJrvDTO resumen = resumenMesas.get(mesa.getId());
			return resumen == null || (resumen.getIglesiasAsignadas() == 0 && resumen.getMiembrosAsignados() == 0);
		});
		recalcularAvanceJrv();
		filtrarMesasSeleccionables();
	}

	/**
	 * Deriva en memoria las opciones de los filtros a partir de las mesas ya cargadas,
	 * encadenadas: la parroquia respeta el cantón elegido y el recinto respeta ambos.
	 */
	private void cargarOpcionesMesas() {
		java.util.Map<Integer, String> cantones = new java.util.TreeMap<>();
		java.util.Map<Integer, String> parroquias = new java.util.TreeMap<>();
		java.util.Map<Integer, String> recintos = new java.util.TreeMap<>();
		for (MesaDTO mesa : mesasDisponibles) {
			RecintoDTO recinto = mesa.getRecinto();
			if (recinto == null)
				continue;
			if (recinto.getCantonId() != null)
				cantones.put(recinto.getCantonId(), recinto.getCantonNombre());
			boolean cantonCoincide = cantonMesaId == null || cantonMesaId.equals(recinto.getCantonId());
			if (recinto.getUbicacionId() != null && cantonCoincide)
				parroquias.put(recinto.getUbicacionId(), recinto.getUbicacionNombre());
			boolean parroquiaCoincide = parroquiaMesaId == null || parroquiaMesaId.equals(recinto.getUbicacionId());
			if (recinto.getId() != null && cantonCoincide && parroquiaCoincide)
				recintos.put(recinto.getId(), recinto.getNombre());
		}
		cantonesMesas = cantones.entrySet().stream().map(e -> new OpcionPadronDTO(e.getKey(), e.getValue())).toList();
		parroquiasMesas = parroquias.entrySet().stream().map(e -> new OpcionPadronDTO(e.getKey(), e.getValue()))
				.toList();
		recintosMesas = recintos.entrySet().stream().map(e -> new OpcionPadronDTO(e.getKey(), e.getValue())).toList();
	}

	/**
	 * Avance de conformación de juntas del proceso vigente, como el avance de Padrón: global
	 * (no depende de los filtros) y calculado en memoria sobre las mesas con padrón ya cargadas
	 * y su resumen, sin consultas adicionales. Se recalcula al cargar las mesas y tras cada
	 * cambio en la junta de la mesa seleccionada.
	 */
	@Getter
	private int avanceTotalMesas, avanceCompletadas, avanceListas, avanceParciales, avancePendientes,
			avancePorcentaje;

	private void recalcularAvanceJrv() {
		avanceTotalMesas = avanceCompletadas = avanceListas = avanceParciales = avancePendientes = 0;
		if (mesasDisponibles != null) {
			for (MesaDTO mesa : mesasDisponibles) {
				avanceTotalMesas++;
				switch (estadoJrv(mesa)) {
				case ESTADO_COMPLETADO -> avanceCompletadas++;
				case ESTADO_LISTA -> avanceListas++;
				case ESTADO_PARCIAL -> avanceParciales++;
				default -> avancePendientes++;
				}
			}
		}
		avancePorcentaje = avanceTotalMesas == 0 ? 0 : (int) Math.floor(avanceCompletadas * 100.0 / avanceTotalMesas);
	}

	/** Mesas con trabajo iniciado sin completar: parciales y listas para completar. */
	public int getAvanceEnProceso() {
		return avanceParciales + avanceListas;
	}

	public boolean isAvanceCompleto() {
		return avanceTotalMesas > 0 && avanceCompletadas == avanceTotalMesas;
	}

	/** Filtro «Estado de JRV»: por defecto «Sin completar» (sin miembros, parciales y listas para completar). */
	public static final String ESTADO_SIN_COMPLETAR = "SIN_COMPLETAR";
	public static final String ESTADO_PENDIENTE = "PENDIENTE";
	public static final String ESTADO_PARCIAL = "PARCIAL";
	public static final String ESTADO_LISTA = "LISTA";
	public static final String ESTADO_COMPLETADO = "COMPLETADO";
	public static final String ESTADO_TODOS = "TODOS";

	@Getter
	@Setter
	private String estadoJrvFiltro = ESTADO_SIN_COMPLETAR;

	public void cambiarEstadoJrv() {
		filtrarMesasSeleccionables();
	}

	/**
	 * Estado de la junta de la mesa: COMPLETADO solo si tiene las cuatro dignidades y además
	 * fue registrada como completada (con Presidente responsable, como en Miembros asignados);
	 * con las dignidades pero sin completar, LISTA; con algunos miembros, PARCIAL; sin miembros,
	 * PENDIENTE.
	 */
	public String estadoJrv(MesaDTO mesa) {
		ec.com.antenasur.dto.ResumenMesaJrvDTO resumen = mesa == null ? null : resumenMesas.get(mesa.getId());
		if (resumen == null || resumen.getMiembrosAsignados() == 0) {
			return ESTADO_PENDIENTE;
		}
		if (resumen.isCompleta()) {
			return mesa.getResponsable() != null && !mesa.getResponsable().isBlank() ? ESTADO_COMPLETADO : ESTADO_LISTA;
		}
		return ESTADO_PARCIAL;
	}

	public String estadoJrvClave(MesaDTO mesa) {
		return claveEstado(estadoJrv(mesa));
	}

	public String estadoJrvSeveridad(MesaDTO mesa) {
		return severidadEstado(estadoJrv(mesa));
	}

	public String estadoJrvIcono(MesaDTO mesa) {
		return iconoEstado(estadoJrv(mesa));
	}

	// Única tabla estado → texto, color e ícono: la usan la tabla de mesas, el filtro «Estado de
	// JRV» (mismas claves en sus opciones) y la etiqueta de la mesa seleccionada.

	private static String claveEstado(String estado) {
		return switch (estado) {
		case ESTADO_COMPLETADO -> "mjrv.mesas.estado.completado";
		case ESTADO_LISTA -> "mjrv.estado.lista.completar";
		case ESTADO_PARCIAL -> "mjrv.mesas.estado.parcial";
		default -> "mjrv.mesas.estado.pendiente";
		};
	}

	private static String severidadEstado(String estado) {
		return switch (estado) {
		case ESTADO_COMPLETADO -> "success";
		case ESTADO_LISTA -> "info";
		case ESTADO_PARCIAL -> "warning";
		default -> "secondary";
		};
	}

	private static String iconoEstado(String estado) {
		return switch (estado) {
		case ESTADO_COMPLETADO -> "pi pi-check";
		case ESTADO_LISTA -> "pi pi-flag";
		case ESTADO_PARCIAL -> "pi pi-clock";
		default -> "pi pi-user-minus";
		};
	}

	private boolean cumpleEstadoJrv(MesaDTO mesa) {
		String estado = estadoJrv(mesa);
		return switch (estadoJrvFiltro == null ? ESTADO_SIN_COMPLETAR : estadoJrvFiltro) {
		case ESTADO_TODOS -> true;
		case ESTADO_COMPLETADO -> ESTADO_COMPLETADO.equals(estado);
		// Cada opción individual equivale a una etiqueta de la tabla; «Sin completar» agrupa las
		// tres que no son Completado (sin miembros, parcial y lista para completar).
		case ESTADO_PENDIENTE, ESTADO_PARCIAL, ESTADO_LISTA -> estadoJrvFiltro.equals(estado);
		default -> !ESTADO_COMPLETADO.equals(estado);
		};
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
			// La mesa del Presidente se muestra siempre, sea cual sea su estado.
			if (mesaRestringida == null && !cumpleEstadoJrv(mesa))
				return false;
			if (cantonMesaId != null && !cantonMesaId.equals(recinto.getCantonId()))
				return false;
			if (parroquiaMesaId != null && !parroquiaMesaId.equals(recinto.getUbicacionId()))
				return false;
			if (recintoMesaId != null && !recintoMesaId.equals(recinto.getId()))
				return false;
			return texto.isEmpty() || contiene(mesa.getNombre(), texto) || contiene(recinto.getNombre(), texto)
					|| contiene(recinto.getCantonNombre(), texto) || contiene(recinto.getUbicacionNombre(), texto);
		}).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
	}

	private static boolean contiene(String valor, String texto) {
		return valor != null && valor.toLowerCase(java.util.Locale.ROOT).contains(texto);
	}
}
