package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.dto.CronogramaFaseDTO;
import ec.com.antenasur.dto.IglesiaAsignacionDTO;
import ec.com.antenasur.dto.RegistroCivilDTO;
import ec.com.antenasur.dto.UsuarioDTO;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.service.GeograpService;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.IglesiaPersonaService.ResultadoVinculo;
import ec.com.antenasur.service.IglesiaService;
import ec.com.antenasur.service.PersonaService;
import ec.com.antenasur.service.RolService;
import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.SendEmail;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Backing bean de la pantalla {@code asignacionUsuarios.xhtml}: lista todas
 * las iglesias y permite asignar (o reasignar) un Usuario IglesiaAdmin a cada
 * una. La operaciÃ³n estÃ¡ restringida a la fase
 * {@link ec.com.antenasur.enums.FaseElectoral#ASIGNACION_USUARIOS} del
 * cronograma electoral.
 *
 * <p>Diferente de {@link UsuarioControlador} (CRUD genÃ©rico de usuarios) â€” esta
 * pantalla mira el problema desde la iglesia: KPIs de avance, filtro por
 * estado de asignaciÃ³n, validaciÃ³n de fase y vinculaciÃ³n automÃ¡tica vÃ­a
 * {@code IglesiaPersona} cuando la persona aÃºn no es miembro de la iglesia.
 */
@Named
@ViewScoped
@Slf4j
public class AsignacionUsuariosController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String ESTADO_TODAS = "TODAS";
    private static final String ESTADO_CON_ADMIN = "CON_ADMIN";
    private static final String ESTADO_SIN_ADMIN = "SIN_ADMIN";

    @Inject
    private ProcesoBean procesoBean;

    @Inject
    private IglesiaService iglesiaService;

    @Inject
    private GeograpService geograpService;

    @Inject
    private CronogramaService cronogramaService;

    @Inject
    private UsuarioService usuarioService;

    @Inject
    private PersonaService personaService;

    @Inject
    private IglesiaPersonaService iglesiaPersonaService;

    @Inject
    private RolService rolService;

    // â”€â”€ Estado de la tabla â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Getter @Setter
    private List<IglesiaAsignacionDTO> listaIglesiasOriginal;

    @Getter @Setter
    private List<IglesiaAsignacionDTO> listaIglesias;

    @Getter @Setter
    private List<IglesiaAsignacionDTO> listaIglesiasFiltrada;

    /** Filtro de estado: SIN_ADMIN (default), CON_ADMIN, TODAS. */
    @Getter @Setter
    private String estadoFiltro = ESTADO_SIN_ADMIN;

    // â”€â”€ Filtros geogrÃ¡ficos (mismo patrÃ³n que IglesiaController) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Getter @Setter
    private List<Geograp> provincias;

    @Getter @Setter
    private Integer provinciaFiltroId;

    @Getter @Setter
    private List<Geograp> cantonesFiltro;

    @Getter @Setter
    private List<Geograp> parroquias;

    @Getter @Setter
    private Geograp cantonSeleccionado, parroquiaSeleccionado;

    // â”€â”€ Estado del diÃ¡logo de asignaciÃ³n â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Getter @Setter
    private IglesiaAsignacionDTO iglesiaSeleccionada;

    @Getter @Setter
    private UsuarioDTO nuevoAdmin;

    @Getter @Setter
    private RegistroCivilDTO personaRegistroCivil;

    /** true si la persona ya existe en BD; false si proviene del Registro Civil. */
    @Getter @Setter
    private boolean personaExistente;

    // â”€â”€ Cronograma â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Getter
    private CronogramaFaseDTO faseVigente;

    @Getter
    private boolean puedeAsignar;

    /** [total, conAdmin, porcentaje] para la barra de progreso. */
    @Getter
    private int[] progresoAsignacion = {0, 0, 0};

    @PostConstruct
    private void init() {
        try {
            cantonSeleccionado = new Geograp();
            parroquiaSeleccionado = new Geograp();

            // Provincias dinÃ¡micas â€” mismo patrÃ³n que IglesiaController
            Geograp provRef = geograpService.find(7);
            if (provRef != null && provRef.getGeograp() != null) {
                provincias = geograpService.findByFatherId(provRef.getGeograp().getId());
            }
            if (provincias == null) {
                provincias = Collections.emptyList();
            }

            recargarListaCompleta();
            faseVigente = cronogramaService.getFaseVigenteDelProcesoActivo();
            puedeAsignar = cronogramaService.permiteAsignacionUsuarios();
            progresoAsignacion = iglesiaService.calcularProgresoAsignacionUsuarios();
        } catch (Exception e) {
            log.error("Error al inicializar AsignacionUsuariosController", e);
        }
    }

    // â”€â”€ Filtros â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void obtieneCantonesFiltro() {
        cantonesFiltro = null;
        cantonSeleccionado = new Geograp();
        parroquias = null;
        parroquiaSeleccionado = new Geograp();
        if (provinciaFiltroId != null) {
            cantonesFiltro = geograpService.findByFatherId(provinciaFiltroId);
            List<Geograp> parroquiasDeProvincia = geograpService.obtenerParroquiasDeCantones(cantonesFiltro);
            if (parroquiasDeProvincia != null && !parroquiasDeProvincia.isEmpty()) {
                listaIglesiasOriginal = iglesiaService.listarParaAsignacionPorParroquias(parroquiasDeProvincia);
            } else {
                listaIglesiasOriginal = Collections.emptyList();
            }
        } else {
            listaIglesiasOriginal = iglesiaService.listarParaAsignacionUsuarios();
        }
        aplicarFiltroEstado();
    }

    public void obtieneParroquias() {
        if (cantonSeleccionado != null && cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpService.find(cantonSeleccionado.getId());
            parroquias = geograpService.findByFatherId(cantonSeleccionado.getId());
            parroquiaSeleccionado = new Geograp();
            if (parroquias != null && !parroquias.isEmpty()) {
                listaIglesiasOriginal = iglesiaService.listarParaAsignacionPorParroquias(parroquias);
            }
            aplicarFiltroEstado();
        }
    }

    public void obtieneIglesiasPorParroquia() {
        if (parroquiaSeleccionado != null && parroquiaSeleccionado.getId() != null) {
            parroquiaSeleccionado = geograpService.find(parroquiaSeleccionado.getId());
            listaIglesiasOriginal = iglesiaService.listarParaAsignacionPorParroquia(parroquiaSeleccionado);
            aplicarFiltroEstado();
        }
    }

    public void cambiarFiltroEstado() {
        aplicarFiltroEstado();
    }

    public void limpiarFiltros() {
        provinciaFiltroId = null;
        cantonesFiltro = null;
        cantonSeleccionado = new Geograp();
        parroquias = null;
        parroquiaSeleccionado = new Geograp();
        estadoFiltro = ESTADO_SIN_ADMIN;
        recargarListaCompleta();
    }

    private void recargarListaCompleta() {
        listaIglesiasOriginal = iglesiaService.listarParaAsignacionUsuarios();
        aplicarFiltroEstado();
    }

    private void aplicarFiltroEstado() {
        if (listaIglesiasOriginal == null) {
            listaIglesias = Collections.emptyList();
            return;
        }
        if (ESTADO_TODAS.equals(estadoFiltro)) {
            listaIglesias = new ArrayList<>(listaIglesiasOriginal);
            return;
        }
        boolean buscarConAdmin = ESTADO_CON_ADMIN.equals(estadoFiltro);
        List<IglesiaAsignacionDTO> filtrada = new ArrayList<>();
        for (IglesiaAsignacionDTO ig : listaIglesiasOriginal) {
            boolean tiene = Boolean.TRUE.equals(ig.getTieneAdmin());
            if (tiene == buscarConAdmin) {
                filtrada.add(ig);
            }
        }
        listaIglesias = filtrada;
    }

    // â”€â”€ KPIs derivados de la lista completa â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public long getTotalIglesias() {
        return listaIglesiasOriginal == null ? 0 : listaIglesiasOriginal.size();
    }

    public long getTotalConAdmin() {
        if (listaIglesiasOriginal == null) return 0;
        long count = 0;
        for (IglesiaAsignacionDTO i : listaIglesiasOriginal) {
            if (Boolean.TRUE.equals(i.getTieneAdmin())) count++;
        }
        return count;
    }

    public long getTotalSinAdmin() {
        return getTotalIglesias() - getTotalConAdmin();
    }

    // â”€â”€ MensajerÃ­a de fase / permisos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Nombre amigable de la fase del cronograma requerida para esta pantalla. */
    public String getFaseRequerida() {
        return "AsignaciÃ³n de Usuarios";
    }

    /**
     * Mensaje contextual del estado del cronograma. Devuelve {@code null}
     * cuando la asignaciÃ³n estÃ¡ habilitada (la UI no debe mostrar nada).
     * Diferencia tres escenarios para que el usuario sepa exactamente por quÃ©
     * no puede operar y a quiÃ©n pedir acciÃ³n:
     * <ul>
     *   <li>Sin fase vigente â€” el cronograma no tiene fase activa en este momento.</li>
     *   <li>Fase vigente distinta â€” indica cuÃ¡l es la fase actual y cuÃ¡l se requiere.</li>
     * </ul>
     */
    public String getMensajeBloqueoFase() {
        if (puedeAsignar) {
            return null;
        }
        if (faseVigente == null) {
            return "No hay una fase vigente del cronograma electoral. "
                 + "La asignaciÃ³n de administradores solo estÃ¡ disponible durante la fase \""
                 + getFaseRequerida() + "\". Solicite al administrador del sistema la "
                 + "apertura de la fase correspondiente.";
        }
        return "La fase vigente del cronograma es \"" + faseVigente.getTitulo() + "\". "
             + "La asignaciÃ³n de administradores solo estÃ¡ habilitada durante la fase \""
             + getFaseRequerida() + "\".";
    }

    /**
     * Mensaje breve para tooltip de botones cuando la asignaciÃ³n estÃ¡
     * deshabilitada por fase. Devuelve {@code null} cuando estÃ¡ habilitada
     * (los botones tienen su propio tÃ­tulo de acciÃ³n).
     */
    public String getTooltipAccionDeshabilitada() {
        if (puedeAsignar) {
            return null;
        }
        if (faseVigente == null) {
            return "Disponible solo en la fase \"" + getFaseRequerida()
                 + "\". Actualmente no hay fase vigente.";
        }
        return "Disponible solo en la fase \"" + getFaseRequerida()
             + "\". Fase vigente: \"" + faseVigente.getTitulo() + "\".";
    }

    /** Refresca el estado de fase desde la BD; Ãºtil antes de validar al guardar. */
    private void refrescarEstadoFase() {
        faseVigente = cronogramaService.getFaseVigenteDelProcesoActivo();
        puedeAsignar = cronogramaService.permiteAsignacionUsuarios();
    }

    // â”€â”€ DiÃ¡logo de asignaciÃ³n â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Abre el diÃ¡logo en modo "asignar" para una iglesia sin admin. Re-valida
     * la fase al inicio: si entre el load de la pÃ¡gina y este clic la fase
     * cambiÃ³, mostramos el mensaje preciso y NO abrimos el diÃ¡logo.
     */
    public void prepararAsignacion(IglesiaAsignacionDTO iglesia) {
        if (iglesia == null) return;
        refrescarEstadoFase();
        if (!puedeAsignar) {
            JsfUtil.addWarningMessage(getMensajeBloqueoFase());
            this.iglesiaSeleccionada = null;
            this.nuevoAdmin = null;
            return;
        }
        this.iglesiaSeleccionada = iglesia;
        this.nuevoAdmin = new UsuarioDTO();
        this.nuevoAdmin.setIglesiaId(iglesia.getId());
        this.nuevoAdmin.setIglesiaNombre(iglesia.getNombre());
        this.personaRegistroCivil = null;
        this.personaExistente = false;
    }

    /** Abre el diÃ¡logo en modo "reasignar" sobre una iglesia que ya tiene admin. */
    public void prepararReasignacion(IglesiaAsignacionDTO iglesia) {
        prepararAsignacion(iglesia);
    }

    public boolean isReasignacion() {
        return iglesiaSeleccionada != null && Boolean.TRUE.equals(iglesiaSeleccionada.getTieneAdmin());
    }

    /**
     * Listener del campo "cÃ©dula": busca primero en {@code tb_persona} y si no
     * existe consulta el WS REST del Registro Civil. Hidrata
     * {@code nuevoAdmin} con los datos encontrados.
     */
    public void buscarPersonaPorCedula() {
        if (nuevoAdmin == null || nuevoAdmin.getPersonaDocumento() == null
                || nuevoAdmin.getPersonaDocumento().trim().isEmpty()) {
            return;
        }
        String cedula = nuevoAdmin.getPersonaDocumento().trim();

        // 1) BD interna
        Persona p = personaService.finByPersonaDocument(cedula);
        if (p != null) {
            // Regla de negocio: una persona pertenece a UNA sola iglesia.
            // Si ya estÃ¡ vinculada a otra distinta de la seleccionada, se
            // bloquea la asignaciÃ³n con mensaje claro y se limpian los
            // campos para evitar que el usuario lo guarde por inadvertencia.
            Iglesia otraIglesia = obtenerIglesiaDeOtraPersona(p);
            if (otraIglesia != null) {
                limpiarDatosNuevoAdmin();
                JsfUtil.addErrorMessage(
                        "La persona " + p.getNombres()
                        + (p.getApellidos() != null ? " " + p.getApellidos() : "")
                        + " (cÃ©dula " + cedula + ") ya pertenece a la iglesia \""
                        + otraIglesia.getNombre() + "\". "
                        + "No puede ser asignada como administradora de \""
                        + iglesiaSeleccionada.getNombre() + "\". "
                        + "Si requiere reasignarla, primero debe desvincularla de su iglesia actual.");
                return;
            }
            nuevoAdmin.setPersonaId(p.getId());
            nuevoAdmin.setPersonaDocumento(p.getDocumento());
            nuevoAdmin.setPersonaNombres(p.getNombres());
            nuevoAdmin.setPersonaApellidos(p.getApellidos());
            if (nuevoAdmin.getUsername() == null || nuevoAdmin.getUsername().isEmpty()) {
                nuevoAdmin.setUsername(p.getDocumento());
            }
            if (nuevoAdmin.getCorreo() == null || nuevoAdmin.getCorreo().isEmpty()) {
                nuevoAdmin.setCorreo(p.getDocumento() + "@consejodecomunicacion.gob.ec");
            }
            nuevoAdmin.setPermanente(true);
            personaExistente = true;
            personaRegistroCivil = null;
            // Mensaje contextual segÃºn si ya pertenece a la iglesia seleccionada
            // o si estÃ¡ libre (sin iglesia previa).
            // IMPORTANTE: lookup por DOCUMENTO (no por id) â€” tb_persona puede
            // tener filas duplicadas con la misma cÃ©dula, y el vÃ­nculo en
            // tb_iglesia_persona puede apuntar a una distinta de la que devuelve
            // finByPersonaDocument (que ordena por id ASC).
            Iglesia iglesiaActual = iglesiaPersonaService.obtenerIglesiaDePersonaPorDocumento(cedula);
            if (iglesiaActual != null) {
                JsfUtil.addInfoMessage("Persona encontrada â€” ya es miembro de \""
                        + iglesiaActual.getNombre() + "\".");
            } else {
                JsfUtil.addInfoMessage("Persona encontrada â€” sin iglesia asignada. Se vincularÃ¡ a \""
                        + iglesiaSeleccionada.getNombre() + "\" al guardar.");
            }
            return;
        }

        // 2) Fallback Registro Civil
        if (cargarDesdeRegistroCivil(cedula)) {
            nuevoAdmin.setPersonaNombres(personaRegistroCivil.getNombre());
            nuevoAdmin.setPersonaDocumento(personaRegistroCivil.getCedula());
            nuevoAdmin.setUsername(personaRegistroCivil.getCedula());
            nuevoAdmin.setCorreo(personaRegistroCivil.getCedula() + "@consejodecomunicacion.gob.ec");
            nuevoAdmin.setPermanente(true);
            personaExistente = false;
            JsfUtil.addInfoMessage("Datos obtenidos del Registro Civil. Se crearÃ¡ el registro y la vinculaciÃ³n a la iglesia al guardar.");
        } else {
            personaRegistroCivil = null;
            JsfUtil.addWarningMessage("No se encontrÃ³ la persona en el sistema ni en el Registro Civil");
        }
    }

    /**
     * Devuelve la {@link Iglesia} a la que la persona ya pertenece, SOLO si
     * es distinta de la iglesia seleccionada en el diÃ¡logo. Devuelve null en
     * los casos OK: persona sin iglesia previa, o persona que ya estÃ¡ en la
     * misma iglesia que se estÃ¡ asignando.
     *
     * <p>Resuelve por DOCUMENTO (no por id) para tolerar duplicados en
     * {@code tb_persona}: si hay dos filas con la misma cÃ©dula, el vÃ­nculo
     * en {@code tb_iglesia_persona} puede apuntar a un id distinto del que
     * devuelve {@link ec.com.antenasur.service.PersonaService#finByPersonaDocument}.
     */
    private Iglesia obtenerIglesiaDeOtraPersona(Persona p) {
        if (p == null || p.getDocumento() == null || iglesiaSeleccionada == null
                || iglesiaSeleccionada.getId() == null) {
            return null;
        }
        Iglesia actual = iglesiaPersonaService.obtenerIglesiaDePersonaPorDocumento(p.getDocumento());
        if (actual == null || actual.getId() == null) {
            return null;
        }
        return actual.getId().equals(iglesiaSeleccionada.getId()) ? null : actual;
    }

    /**
     * Limpia los datos del admin candidato cuando la bÃºsqueda detecta un
     * conflicto (persona ya pertenece a otra iglesia). Mantiene la cÃ©dula
     * para que el usuario vea quÃ© tipeÃ³ en el contexto del mensaje de error.
     */
    private void limpiarDatosNuevoAdmin() {
        if (nuevoAdmin != null) {
            nuevoAdmin.setPersonaId(null);
            nuevoAdmin.setPersonaNombres(null);
            nuevoAdmin.setPersonaApellidos(null);
            nuevoAdmin.setUsername(null);
            nuevoAdmin.setCorreo(null);
        }
        personaRegistroCivil = null;
        personaExistente = false;
    }

    private boolean cargarDesdeRegistroCivil(String cedula) {
        Client cliente = ClientBuilder.newClient();
        Response respuesta = null;
        try {
            WebTarget target = cliente.target("http://192.168.26.32:8090/WS_REST/datos_regitrocivil/");
            respuesta = target.path(cedula).request().get();
            personaRegistroCivil = new RegistroCivilDTO();
            if (respuesta.getStatus() == 200) {
                personaRegistroCivil = respuesta.readEntity(RegistroCivilDTO.class);
                return personaRegistroCivil != null && personaRegistroCivil.getCedula() != null;
            }
            return false;
        } catch (Exception e) {
            log.warn("WS Registro Civil no disponible o respuesta invÃ¡lida para cÃ©dula {}", cedula, e);
            personaRegistroCivil = null;
            return false;
        } finally {
            if (respuesta != null) try { respuesta.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Persiste la asignaciÃ³n. Reglas:
     * <ol>
     *   <li>Verifica fase ASIGNACION_USUARIOS.</li>
     *   <li>Si hay admin previo en la iglesia, lo desasigna.</li>
     *   <li>Garantiza que la persona estÃ© vinculada vÃ­a {@code IglesiaPersona}
     *       (la crea si no estaba).</li>
     *   <li>Crea el {@link ec.com.antenasur.model.Usuario} con rol IglesiaAdmin
     *       enlazado a la iglesia.</li>
     *   <li>EnvÃ­a correo de bienvenida con la clave temporal.</li>
     * </ol>
     */
    public void asignarAdmin() {
        try {
            // Refresca el estado de fase para que el mensaje refleje la realidad
            // actual aunque el usuario haya tenido la pÃ¡gina abierta mucho tiempo.
            refrescarEstadoFase();
            if (!puedeAsignar) {
                rechazar(getMensajeBloqueoFase());
                return;
            }
            if (iglesiaSeleccionada == null || iglesiaSeleccionada.getId() == null) {
                rechazar("No se identificÃ³ la iglesia destino.");
                return;
            }
            if (nuevoAdmin == null
                    || nuevoAdmin.getPersonaDocumento() == null || nuevoAdmin.getPersonaDocumento().trim().isEmpty()
                    || nuevoAdmin.getPersonaNombres() == null || nuevoAdmin.getPersonaNombres().trim().isEmpty()
                    || nuevoAdmin.getUsername() == null || nuevoAdmin.getUsername().trim().isEmpty()
                    || nuevoAdmin.getCorreo() == null || nuevoAdmin.getCorreo().trim().isEmpty()) {
                rechazar("Complete la cÃ©dula, nombres, usuario y correo del nuevo administrador.");
                return;
            }

            Rol rolIglesiaAdmin = resolverRolIglesiaAdmin();
            if (rolIglesiaAdmin == null) {
                rechazar("No se encontrÃ³ el rol IglesiaAdmin en el catÃ¡logo. Contacte al administrador.");
                return;
            }

            // Defensa en profundidad: si entre el lookup de cÃ©dula y este save
            // la persona quedÃ³ vinculada a otra iglesia (caso raro pero posible
            // bajo concurrencia), rechazamos antes de crear nada en BD.
            Persona personaEnBD = personaService.finByPersonaDocument(
                    nuevoAdmin.getPersonaDocumento().trim());
            if (personaEnBD != null) {
                Iglesia otraIglesia = obtenerIglesiaDeOtraPersona(personaEnBD);
                if (otraIglesia != null) {
                    rechazar("La persona " + personaEnBD.getNombres()
                            + " ya pertenece a la iglesia \"" + otraIglesia.getNombre()
                            + "\". Debe ser desvinculada de esa iglesia antes de "
                            + "asignarla como administradora de \""
                            + iglesiaSeleccionada.getNombre() + "\".");
                    return;
                }
            }

            boolean esReasignacion = Boolean.TRUE.equals(iglesiaSeleccionada.getTieneAdmin());

            // 1) Desasignar admin previo si aplica
            UsuarioDTO previo = null;
            if (esReasignacion) {
                previo = usuarioService.removerAdminDeIglesia(iglesiaSeleccionada.getId());
            }

            // 2) Crear el usuario nuevo (UsuarioService maneja persona + RolUsuario + iglesia)
            nuevoAdmin.setIglesiaId(iglesiaSeleccionada.getId());
            UsuarioDTO creado = usuarioService.crearUsuarioDesdeDTO(nuevoAdmin, rolIglesiaAdmin);
            if (creado == null || creado.getId() == null) {
                rechazar("No se pudo registrar el usuario. Verifique los datos ingresados.");
                return;
            }
            nuevoAdmin = creado;

            // 3) Garantizar el vÃ­nculo IglesiaPersona â€” robusto contra duplicados
            // de tb_persona. Primero verificamos por DOCUMENTO si ya existe un
            // vÃ­nculo a esta iglesia (puede estar registrado contra otro id de
            // persona si la cÃ©dula estÃ¡ duplicada en BD). Solo si no existe se
            // intenta crear uno nuevo.
            Iglesia iglesiaPrevia = iglesiaPersonaService.obtenerIglesiaDePersonaPorDocumento(
                    nuevoAdmin.getPersonaDocumento());
            boolean yaVinculadaAEstaIglesia = iglesiaPrevia != null
                    && iglesiaPrevia.getId() != null
                    && iglesiaPrevia.getId().equals(iglesiaSeleccionada.getId());
            boolean vinculoCreado = false;
            if (!yaVinculadaAEstaIglesia) {
                ResultadoVinculo rv = iglesiaPersonaService.crearVinculoSiNoExiste(
                        iglesiaSeleccionada.getId(), creado.getPersonaId());
                vinculoCreado = rv != null && rv.fueCreado();
            }

            // 4) AuditorÃ­a y correo (best-effort, no rompen el flujo)
            try {
                String accion = esReasignacion ? "REASIGNA" : "ASIGNA";
                procesoBean.registraActividad(accion + " ADMIN IGLESIA " + iglesiaSeleccionada.getNombre()
                        + " -> " + creado.getUsername()
                        + (previo != null ? " (anterior: " + previo.getUsername() + ")" : ""));
            } catch (Exception ignored) { /* nunca interrumpir */ }

            try {
                enviarCorreoBienvenida(creado);
            } catch (Exception e) {
                log.warn("Error al enviar correo de bienvenida a {}", creado.getUsername(), e);
                JsfUtil.addWarningMessage("Usuario creado, pero el correo de bienvenida no pudo enviarse.");
            }

            // 5) Mensajes informativos
            if (esReasignacion) {
                JsfUtil.addSuccessMessage("Iglesia reasignada correctamente. Nuevo administrador: "
                        + creado.getPersonaNombres());
            } else {
                JsfUtil.addSuccessMessage("Administrador asignado correctamente: "
                        + creado.getPersonaNombres());
            }
            if (vinculoCreado) {
                JsfUtil.addInfoMessage("La persona fue vinculada automÃ¡ticamente a la iglesia "
                        + iglesiaSeleccionada.getNombre() + ".");
            }

            // 6) Refrescar UI
            recargarListaCompleta();
            progresoAsignacion = iglesiaService.calcularProgresoAsignacionUsuarios();
            PrimeFaces.current().ajax().update("frmAsignacion", "frmStats", "frmProgreso", "msgs");
        } catch (Exception e) {
            log.error("Error al asignar admin a iglesia id={}",
                    iglesiaSeleccionada != null ? iglesiaSeleccionada.getId() : null, e);
            rechazar("OcurriÃ³ un error inesperado al asignar el administrador.");
        }
    }

    private Rol resolverRolIglesiaAdmin() {
        // Los nombres en BD llevan prefijo (ej. SITEC-IglesiaAdmin), por lo que
        // filtramos por sufijo igual que UsuarioControlador.isRolRequiereIglesia.
        List<Rol> roles = rolService.getRolesAplicativoSeleccion();
        if (roles == null) return null;
        for (Rol r : roles) {
            if (r.getNombre() != null && r.getNombre().endsWith(Constantes.getRolIglesiaAdmin())) {
                return r;
            }
        }
        return null;
    }

    private void enviarCorreoBienvenida(UsuarioDTO usuario) {
        if (usuario == null || usuario.getCorreo() == null || usuario.getCorreo().trim().isEmpty()) {
            return;
        }
        String html = "<p><strong>Estimado/a " + usuario.getPersonaNombres() + "</strong></p>"
                + "<p>Se le ha asignado como administrador de la iglesia "
                + "<strong>" + iglesiaSeleccionada.getNombre() + "</strong>"
                + " en el Sistema Electoral.</p>"
                + "<p>Datos de acceso:</p>"
                + "<p><strong>Usuario:</strong> " + usuario.getUsername() + "</p>"
                + "<p><strong>ContraseÃ±a:</strong> " + usuario.getPersonaDocumento() + "</p>"
                + Constantes.FIRMA_CORREO;
        List<String> destinos = new ArrayList<>();
        destinos.add(usuario.getCorreo());
        try {
			SendEmail.correoAdjunto(destinos, "AsignaciÃ³n como administrador de iglesia", html, Constantes.getPathLogo());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void rechazar(String mensaje) {
        JsfUtil.addErrorMessage(mensaje);
        PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
    }

    public void cancelarAsignacion() {
        iglesiaSeleccionada = null;
        nuevoAdmin = null;
        personaRegistroCivil = null;
        personaExistente = false;
    }
}
