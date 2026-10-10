package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.AyudaContactoDTO;
import ec.com.antenasur.dto.AyudaPreguntaAdminDTO;
import ec.com.antenasur.dto.AyudaRespuestaDTO;
import ec.com.antenasur.dto.AyudaSinRespuestaDTO;
import ec.com.antenasur.dto.CronogramaFaseDTO;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.tec.AyudaContactoFacade;
import ec.com.antenasur.facade.tec.AyudaFacade;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.tec.AyudaContacto;
import ec.com.antenasur.model.tec.AyudaPregunta;
import ec.com.antenasur.security.menu.AutorizacionMenuService;
import ec.com.antenasur.security.menu.PaginasMenu;
import lombok.extern.slf4j.Slf4j;

/**
 * Chatbot de ayuda por reglas: sugerencias según rol, pantalla y fase vigente;
 * búsqueda en el índice de texto; valoración; registro anónimo de consultas sin
 * respuesta; canales oficiales de contacto; y mantenimiento del contenido.
 *
 * <p>El rol se resuelve aquí con el principal EJB, no con lo que envía la vista.</p>
 */
@Stateless
@Slf4j
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal", "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa" })
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal", "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa" })
public class AyudaService {

    public static final int MAX_RESULTADOS = 5;
    public static final int MAX_SUGERENCIAS = 6;
    public static final int MIN_LONGITUD_CONSULTA = 3;
    public static final int MAX_LONGITUD_CONSULTA = 150;
    public static final int DIAS_RETENCION_SIN_RESPUESTA = 90;

    @Resource
    private SessionContext sessionContext;

    @Inject
    private AyudaFacade ayudaFacade;

    @Inject
    private AyudaContactoFacade ayudaContactoFacade;

    @Inject
    private CronogramaService cronogramaService;

    @Inject
    private AutorizacionMenuService autorizacionMenuService;

    // ───────────────────────────────────────────────────────────── Widget

    public List<AyudaRespuestaDTO> sugerencias(String pagina) {
        return aRespuestas(ayudaFacade.sugerencias(rolesDelUsuario(), paginaSegura(pagina), faseVigente(),
                MAX_SUGERENCIAS));
    }

    /**
     * Busca primero con todas las palabras y, si no hay resultados, con cualquiera de
     * ellas. Si tampoco encuentra nada y {@code registrarSinRespuesta}, registra la consulta
     * (anónima y enmascarada). La búsqueda mientras se escribe no registra: guardaría
     * palabras a medio escribir.
     */
    public List<AyudaRespuestaDTO> buscar(String texto, String pagina, boolean registrarSinRespuesta) {
        String normalizado = TextoAyuda.normalizar(texto);
        if (normalizado.length() < MIN_LONGITUD_CONSULTA) {
            return List.of();
        }
        if (normalizado.length() > MAX_LONGITUD_CONSULTA) {
            normalizado = normalizado.substring(0, MAX_LONGITUD_CONSULTA);
        }
        List<String> roles = rolesDelUsuario();
        String paginaSegura = paginaSegura(pagina);
        String fase = faseVigente();
        List<Object[]> filas = ayudaFacade.buscar(normalizado, false, roles, paginaSegura, fase, MAX_RESULTADOS);
        if (filas.isEmpty()) {
            String alternativa = TextoAyuda.consultaAlternativa(normalizado);
            if (!alternativa.isEmpty()) {
                filas = ayudaFacade.buscar(alternativa, true, roles, paginaSegura, fase, MAX_RESULTADOS);
            }
        }
        if (filas.isEmpty() && registrarSinRespuesta) {
            registrarSinRespuesta(texto, paginaSegura);
        }
        return aRespuestas(filas);
    }

    /**
     * Respuesta de una pregunta elegida con un clic. Se vuelve a comprobar en el servidor
     * que sea visible para el rol: el id llega desde el navegador.
     */
    public AyudaRespuestaDTO respuesta(Integer preguntaId) {
        if (preguntaId == null) {
            return null;
        }
        List<AyudaRespuestaDTO> respuestas = aRespuestas(ayudaFacade.visiblePorId(preguntaId, rolesDelUsuario()));
        return respuestas.isEmpty() ? null : respuestas.get(0);
    }

    public void valorar(Integer preguntaId, boolean util) {
        if (preguntaId != null) {
            ayudaFacade.valorar(preguntaId, util);
        }
    }

    public List<AyudaContactoDTO> contactos() {
        return ayudaFacade.contactosActivos().stream().map(AyudaService::aDTO).toList();
    }

    private void registrarSinRespuesta(String texto, String pagina) {
        String seguro = TextoAyuda.paraSinRespuesta(texto);
        if (seguro.length() < MIN_LONGITUD_CONSULTA) {
            return;
        }
        try {
            ayudaFacade.registrarSinRespuesta(seguro, pagina);
        } catch (RuntimeException e) {
            // El registro es secundario: nunca debe impedir mostrar el contacto.
            log.warn("No se pudo registrar la consulta de ayuda sin respuesta", e);
        }
    }

    /** Roles SITEC activos del principal EJB (el filtro por rol se decide en el servidor). */
    private List<String> rolesDelUsuario() {
        List<String> roles = ayudaFacade.rolesActivos().stream().map(Rol::getNombre)
                .filter(sessionContext::isCallerInRole).toList();
        // IN () no es válido en SQL: una lista sin coincidencias deja solo las preguntas para todos.
        return roles.isEmpty() ? List.of("-") : roles;
    }

    private String faseVigente() {
        try {
            CronogramaFaseDTO fase = cronogramaService.getFaseVigenteDelProcesoActivo();
            return fase != null && fase.getFase() != null ? fase.getFase().name() : "";
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** Solo nombres de página válidos (PaginasMenu); cualquier otra cosa se trata como «sin página». */
    private static String paginaSegura(String pagina) {
        String normalizada = PaginasMenu.normalizar(pagina);
        return normalizada == null ? "" : normalizada;
    }

    /** El enlace «Ir a la pantalla» solo se entrega si el usuario puede abrir esa página. */
    private List<AyudaRespuestaDTO> aRespuestas(List<Object[]> filas) {
        List<String> permitidas;
        try {
            permitidas = autorizacionMenuService.paginasActuales();
        } catch (RuntimeException e) {
            permitidas = List.of();
        }
        List<AyudaRespuestaDTO> resultado = new ArrayList<>();
        for (Object[] fila : filas) {
            String enlace = (String) fila[3];
            String permitido = enlace != null && PaginasMenu.permite(permitidas, enlace) ? PaginasMenu.normalizar(enlace)
                    : null;
            resultado.add(new AyudaRespuestaDTO(((Number) fila[0]).intValue(), (String) fila[1], (String) fila[2],
                    permitido));
        }
        return resultado;
    }

    // ──────────────────────────────────────────────────────── Mantenimiento

    @RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
    /** Lista mutable: p:dataTable con sortBy la ordena en el propio objeto (SortFeature). */
    public List<AyudaPreguntaAdminDTO> listarPreguntas() {
        return ayudaFacade.listarTodas().stream().map(AyudaService::aDTO)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
    public List<Rol> rolesDisponibles() {
        return ayudaFacade.rolesActivos();
    }

    @RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
    public AyudaPreguntaAdminDTO guardarPregunta(AyudaPreguntaAdminDTO dto) {
        validar(dto);
        AyudaPregunta entidad = dto.getId() == null ? new AyudaPregunta() : ayudaFacade.buscarPorId(dto.getId());
        if (entidad == null) {
            throw new NegocioException("La pregunta ya no existe. Actualice la lista.");
        }
        entidad.setPregunta(dto.getPregunta().trim());
        entidad.setRespuesta(dto.getRespuesta().trim());
        entidad.setPalabrasClave(vacioANulo(dto.getPalabrasClave()));
        entidad.setPagina(PaginasMenu.normalizar(dto.getPagina()));
        entidad.setFase(dto.getFase() == null || dto.getFase().isBlank() ? null : FaseElectoral.valueOf(dto.getFase()));
        entidad.setEnlacePagina(PaginasMenu.normalizar(dto.getEnlacePagina()));
        entidad.setOrden(dto.getOrden() == null ? 100 : dto.getOrden());
        entidad.setEstado(dto.isActivo());
        entidad.setTextoBusqueda(TextoAyuda.textoBusqueda(entidad.getPregunta(), entidad.getPalabrasClave(),
                entidad.getRespuesta()));
        entidad.getRoles().clear();
        entidad.getRoles().addAll(ayudaFacade.rolesPorIds(dto.getRolIds()));
        AyudaPregunta guardada = entidad.getId() == null ? ayudaFacade.create(entidad) : ayudaFacade.edit(entidad);
        return aDTO(guardada);
    }

    @RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
    public List<AyudaSinRespuestaDTO> listarSinRespuesta() {
        return ayudaFacade.listarSinRespuesta();
    }

    @RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
    public void descartarSinRespuesta(Integer id) {
        ayudaFacade.eliminarSinRespuesta(id);
    }

    @RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
    public List<AyudaContactoDTO> listarContactos() {
        return ayudaFacade.contactosTodos().stream().map(AyudaService::aDTO).toList();
    }

    @RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
    public void guardarContacto(AyudaContactoDTO dto) {
        if (dto == null || dto.getTipo() == null || !List.of(AyudaContacto.TELEFONO, AyudaContacto.WHATSAPP,
                AyudaContacto.CORREO).contains(dto.getTipo())) {
            throw new NegocioException("Seleccione el tipo de contacto.");
        }
        String valor = dto.getValor() == null ? "" : dto.getValor().trim();
        boolean valido = AyudaContacto.CORREO.equals(dto.getTipo()) ? valor.matches("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+")
                : valor.matches("\\+?[\\d ]{7,20}");
        if (!valido) {
            throw new NegocioException(AyudaContacto.CORREO.equals(dto.getTipo())
                    ? "Ingrese un correo válido." : "Ingrese un número válido, con código de país (p. ej. +593...).");
        }
        AyudaContacto entidad = dto.getId() == null ? new AyudaContacto() : ayudaContactoFacade.find(dto.getId());
        if (entidad == null) {
            throw new NegocioException("El contacto ya no existe. Actualice la lista.");
        }
        entidad.setTipo(dto.getTipo());
        entidad.setValor(valor);
        entidad.setEtiqueta(vacioANulo(dto.getEtiqueta()));
        entidad.setHorario(vacioANulo(dto.getHorario()));
        entidad.setOrden(dto.getOrden() == null ? 100 : dto.getOrden());
        entidad.setEstado(dto.isActivo());
        if (entidad.getId() == null) {
            ayudaContactoFacade.create(entidad);
        } else {
            ayudaContactoFacade.edit(entidad);
        }
    }

    private static void validar(AyudaPreguntaAdminDTO dto) {
        if (dto == null || dto.getPregunta() == null || dto.getPregunta().isBlank()) {
            throw new NegocioException("La pregunta es obligatoria.");
        }
        if (dto.getRespuesta() == null || dto.getRespuesta().isBlank()) {
            throw new NegocioException("La respuesta es obligatoria.");
        }
        for (String pagina : new String[] { dto.getPagina(), dto.getEnlacePagina() }) {
            if (pagina != null && !pagina.isBlank() && PaginasMenu.normalizar(pagina) == null) {
                throw new NegocioException("La pantalla «" + pagina + "» no es válida. Use el nombre de la página, "
                        + "por ejemplo iglesias.jsf.");
            }
        }
        if (dto.getFase() != null && !dto.getFase().isBlank()) {
            try {
                FaseElectoral.valueOf(dto.getFase());
            } catch (IllegalArgumentException e) {
                throw new NegocioException("La fase seleccionada no es válida.");
            }
        }
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static AyudaPreguntaAdminDTO aDTO(AyudaPregunta p) {
        AyudaPreguntaAdminDTO dto = new AyudaPreguntaAdminDTO();
        dto.setId(p.getId());
        dto.setPregunta(p.getPregunta());
        dto.setRespuesta(p.getRespuesta());
        dto.setPalabrasClave(p.getPalabrasClave());
        dto.setPagina(p.getPagina());
        dto.setFase(p.getFase() == null ? null : p.getFase().name());
        dto.setEnlacePagina(p.getEnlacePagina());
        dto.setOrden(p.getOrden());
        dto.setActivo(Boolean.TRUE.equals(p.getEstado()));
        dto.setRolIds(p.getRoles().stream().map(Rol::getId).collect(Collectors.toList()));
        dto.setRolesTexto(p.getRoles().stream().map(Rol::getNombre).sorted().collect(Collectors.joining(", ")));
        dto.setUtilSi(p.getUtilSi() == null ? 0 : p.getUtilSi());
        dto.setUtilNo(p.getUtilNo() == null ? 0 : p.getUtilNo());
        return dto;
    }

    private static AyudaContactoDTO aDTO(AyudaContacto c) {
        AyudaContactoDTO dto = new AyudaContactoDTO();
        dto.setId(c.getId());
        dto.setTipo(c.getTipo());
        dto.setValor(c.getValor());
        dto.setEtiqueta(c.getEtiqueta());
        dto.setHorario(c.getHorario());
        dto.setOrden(c.getOrden());
        dto.setActivo(Boolean.TRUE.equals(c.getEstado()));
        return dto;
    }
}
