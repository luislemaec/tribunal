package ec.com.antenasur.service.tec;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.LugarVotacionPublicoDTO;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.facade.tec.CronogramaFaseFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.facade.tec.VwLugarVotacionFacade;
import ec.com.antenasur.model.tec.CronogramaFase;
import lombok.extern.slf4j.Slf4j;

/**
 * Consulta pública (sin sesión) del lugar de votación por número de cédula.
 *
 * <p>Reglas de privacidad: solo cédula exacta de 10 dígitos (sin búsqueda por nombre), solo el
 * padrón vigente del proceso electoral activo, nombre enmascarado, sin cédula completa ni iglesia
 * en la respuesta y registro en el log con la cédula enmascarada. La bitácora guarda solo el
 * primer bloqueo por abuso de cada IP y minuto (una entrada por consulta la desbordaría el día del
 * sufragio).
 *
 * <p>Personas y no robots: desafío de prueba de trabajo verificado en el servidor
 * ({@link DesafioAntiRobot}), campo trampa y tiempo mínimo. Carga: límites por IP y global
 * ({@link LimiteConsultaLugarVotacion}), caché breve por cédula ({@link CacheConsultaLugarVotacion}),
 * página sin sesión e índice por cédula (V13).
 */
@Slf4j
@Stateless
@PermitAll
public class ConsultaLugarVotacionService {

    private static final Pattern CEDULA = Pattern.compile("\\d{10}");

    public enum Estado { ENCONTRADO, NO_ENCONTRADO, CEDULA_INVALIDA, LIMITE, OCUPADO, VERIFICACION, SIN_PROCESO }

    public record Resultado(Estado estado, LugarVotacionPublicoDTO lugar) {
        static Resultado de(Estado estado) {
            return new Resultado(estado, null);
        }
    }

    /**
     * Respuesta del navegador al desafío anti-robot y el campo trampa (invisible para una persona;
     * si llega con contenido, lo llenó un robot).
     */
    public record Verificacion(String challenge, String salt, String firma, String numero, String trampa) {
    }

    /** Desafío anti-robot nuevo para la página. */
    public DesafioAntiRobot.Desafio nuevoDesafio() {
        return desafio.crear();
    }

    @Inject
    private VwLugarVotacionFacade lugarFacade;

    @Inject
    private ProcesoElectoralFacade procesoFacade;

    @Inject
    private CronogramaFaseFacade cronogramaFacade;

    @Inject
    private ProcesoService procesoService;

    @Inject
    private LimiteConsultaLugarVotacion limite;

    @Inject
    private DesafioAntiRobot desafio;

    @Inject
    private CacheConsultaLugarVotacion cache;

    /**
     * Orden de las capas: límites (antes de cualquier trabajo), verificación anti-robot y campo
     * trampa, formato de la cédula, caché y, por último, la base.
     */
    public Resultado consultar(String cedulaIngresada, String ip, Verificacion verificacion) {
        int consultas = limite.registrar(ip);
        if (!LimiteConsultaLugarVotacion.permitida(consultas)) {
            if (LimiteConsultaLugarVotacion.primerRechazo(consultas)) {
                log.warn("Consulta pública de lugar de votación: límite superado por la IP {}", ip);
                procesoService.registrarConsultaLugarVotacionBloqueada(ip);
            }
            return Resultado.de(Estado.LIMITE);
        }
        if (!limite.permitirGlobal()) {
            return Resultado.de(Estado.OCUPADO);
        }
        if (verificacion == null || (verificacion.trampa() != null && !verificacion.trampa().isBlank())
                || !desafio.verificar(verificacion.challenge(), verificacion.salt(), verificacion.firma(),
                        verificacion.numero())) {
            log.info("Consulta pública de lugar de votación rechazada por la verificación anti-robot desde {}", ip);
            return Resultado.de(Estado.VERIFICACION);
        }
        String cedula = normalizarCedula(cedulaIngresada);
        if (cedula == null) {
            return Resultado.de(Estado.CEDULA_INVALIDA);
        }
        Resultado guardado = cache.obtener(cedula);
        if (guardado != null) {
            return guardado;
        }
        if (procesoFacade.getActivo() == null) {
            return Resultado.de(Estado.SIN_PROCESO);
        }
        List<Object[]> filas = lugarFacade.buscarLugarProcesoActivo(cedula);
        log.info("Consulta pública de lugar de votación: cédula {} desde {} -> {}", enmascararCedula(cedula), ip,
                filas.isEmpty() ? "sin resultado" : "encontrada");
        Resultado resultado;
        if (filas.isEmpty()) {
            resultado = Resultado.de(Estado.NO_ENCONTRADO);
        } else {
            if (filas.size() > 1) {
                // No debería ocurrir (una persona, un padrón por proceso): se informa el primero y se avisa.
                log.warn("La cédula {} figura más de una vez en el padrón del proceso activo",
                        enmascararCedula(cedula));
            }
            Object[] f = filas.get(0);
            resultado = new Resultado(Estado.ENCONTRADO, new LugarVotacionPublicoDTO(
                    enmascararNombre((String) f[0], (String) f[1]), (String) f[3], fechaSufragio((Integer) f[2]),
                    (String) f[5], (String) f[4], (String) f[6], (String) f[7]));
        }
        cache.guardar(cedula, resultado);
        return resultado;
    }

    /** Fecha de la fase SUFRAGIO vigente del proceso, o null si no está configurada. */
    private java.util.Date fechaSufragio(Integer procesoId) {
        CronogramaFase fase = cronogramaFacade.getFasePorTipo(procesoId, FaseElectoral.SUFRAGIO);
        return fase != null && Boolean.TRUE.equals(fase.getEstado()) ? fase.getFechaInicio() : null;
    }

    /** Cédula sin espacios ni guiones; null si no son exactamente 10 dígitos. */
    static String normalizarCedula(String valor) {
        if (valor == null) {
            return null;
        }
        String limpia = valor.replaceAll("[\\s-]", "");
        return CEDULA.matcher(limpia).matches() ? limpia : null;
    }

    /** Solo los 3 últimos dígitos, para el log: «*******234». */
    static String enmascararCedula(String cedula) {
        if (cedula == null || cedula.length() <= 3) {
            return "***";
        }
        return "*".repeat(cedula.length() - 3) + cedula.substring(cedula.length() - 3);
    }

    /**
     * Inicial de cada palabra de nombres y apellidos, el resto con asteriscos («J*** P****»): el
     * elector reconoce sus iniciales y un tercero no obtiene el nombre completo.
     */
    static String enmascararNombre(String nombres, String apellidos) {
        String completo = ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos)).trim();
        if (completo.isEmpty()) {
            return "";
        }
        StringBuilder salida = new StringBuilder();
        for (String palabra : completo.toUpperCase(Locale.ROOT).split("\\s+")) {
            if (salida.length() > 0) {
                salida.append(' ');
            }
            salida.append(palabra.charAt(0)).append("*".repeat(Math.max(1, palabra.length() - 1)));
        }
        return salida.toString();
    }
}
