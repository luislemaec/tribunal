package ec.com.antenasur.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import ec.com.antenasur.audit.CatalogoActividades.CriterioFiltro;
import static ec.com.antenasur.audit.CatalogoActividades.*;

class CatalogoActividadesTest {

    private record Caso(String tecnico, String accion, String modulo, String resultado) {
    }

    /** Valores que TEC escribe hoy y formatos históricos encontrados en el repositorio. */
    private static final List<Caso> CASOS = List.of(
            // Acceso vigente
            new Caso("LOGIN | MÓDULO: ACCESO; RESULTADO: EXITOSO; DETALLE: Inicio de sesión", "Inició sesión", ACCESO, EXITOSO),
            new Caso("LOGIN | MÓDULO: ACCESO; RESULTADO: FALLIDO; DETALLE: Credenciales rechazadas", "Intentó iniciar sesión", ACCESO, FALLIDO),
            new Caso("LOGOUT | MÓDULO: ACCESO; RESULTADO: EXITOSO; DETALLE: Cierre de sesión", "Cerró sesión", ACCESO, EXITOSO),
            new Caso("LOGOUT | MÓDULO: ACCESO; RESULTADO: EXITOSO; DETALLE: Cierre tras cambio de clave", "Cambió contraseña", ACCESO, EXITOSO),
            new Caso("LOGOUT | MÓDULO: ACCESO; RESULTADO: EXPIRADO; DETALLE: Sesión expirada", "Sesión expirada por inactividad", ACCESO, EXPIRADO),
            new Caso("LOGOUT | MÓDULO: ACCESO; RESULTADO: CANCELADO; DETALLE: Cambio obligatorio de clave cancelado", "Canceló cambio obligatorio de contraseña", ACCESO, CANCELADO),
            new Caso("RECUPERACION_CLAVE | MÓDULO: ACCESO; RESULTADO: SOLICITADO; DETALLE: Solicitud de recuperación de clave", "Solicitó recuperación de contraseña", ACCESO, SOLICITADO),
            // Acceso histórico
            new Caso("INGRESA AL SISTEMA CORRECTAMENTE", "Inició sesión", ACCESO, EXITOSO),
            new Caso("SALE DEL SISTEMA TEC", "Cerró sesión", ACCESO, EXITOSO),
            new Caso("ERROR DE INGRESO AL SISTEMA - Usuario inactivo", "Intentó iniciar sesión", ACCESO, FALLIDO),
            new Caso("RECUPERA CLAVE OLVIDADO", "Solicitó recuperación de contraseña", ACCESO, SIN_DATO),
            new Caso("CAMBIA CONTRASEÑA", "Cambió contraseña", ACCESO, SIN_DATO),
            new Caso("CAMBIA CONTRASEÃ‘A", "Cambió contraseña", ACCESO, SIN_DATO),
            // Persistencia automática (AbstractFacade)
            new Caso("CREA | MÓDULO: PERSONAS E IGLESIAS; ENTIDAD: Persona; REGISTRO: ID 55", "Registró persona", PERSONAS, EXITOSO),
            new Caso("ACTUALIZA | MÓDULO: PERSONAS E IGLESIAS; ENTIDAD: Persona; REGISTRO: ID 55", "Actualizó persona", PERSONAS, EXITOSO),
            new Caso("ACTUALIZA | MÓDULO: PERSONAS E IGLESIAS; ENTIDAD: Iglesia; REGISTRO: Iglesia Central (ID 3)", "Actualizó iglesia", PERSONAS, EXITOSO),
            new Caso("CREA | MÓDULO: PERSONAS E IGLESIAS; ENTIDAD: IglesiaPersona; REGISTRO: Persona 0102030405; Iglesia Central", "Asignó persona a iglesia", PERSONAS, EXITOSO),
            new Caso("DESACTIVA | MÓDULO: PERSONAS E IGLESIAS; ENTIDAD: IglesiaPersona; REGISTRO: ID 9", "Desactivó miembro de iglesia", PERSONAS, EXITOSO),
            new Caso("CREA | MÓDULO: SEGURIDAD; ENTIDAD: Usuario; REGISTRO: jperez (ID 9)", "Registró usuario", SEGURIDAD, EXITOSO),
            new Caso("ACTUALIZA | MÓDULO: SEGURIDAD; ENTIDAD: Usuario; REGISTRO: jperez (ID 9)", "Actualizó usuario", SEGURIDAD, EXITOSO),
            new Caso("DESACTIVA | MÓDULO: SEGURIDAD; ENTIDAD: Usuario; REGISTRO: jperez (ID 9)", "Desactivó usuario", SEGURIDAD, EXITOSO),
            new Caso("CREA | MÓDULO: SEGURIDAD; ENTIDAD: RolUsuario; REGISTRO: ID 4", "Asignó rol a usuario", SEGURIDAD, EXITOSO),
            new Caso("ELIMINA | MÓDULO: SEGURIDAD; ENTIDAD: RolUsuario; REGISTRO: ID 4", "Eliminó rol de usuario", SEGURIDAD, EXITOSO),
            new Caso("CREA | MÓDULO: SEGURIDAD; ENTIDAD: Rol; REGISTRO: SITEC-Tribunal (ID 2)", "Registró rol", SEGURIDAD, EXITOSO),
            new Caso("CREA | MÓDULO: SEGURIDAD; ENTIDAD: MenuRol; REGISTRO: ID 7", "Asignó permiso de menú a rol", SEGURIDAD, EXITOSO),
            new Caso("CREA | MÓDULO: PADRÓN; ENTIDAD: Padron; REGISTRO: ID 77", "Asignó miembro a mesa", PADRON, EXITOSO),
            new Caso("DESACTIVA | MÓDULO: PADRÓN; ENTIDAD: Padron; REGISTRO: ID 77", "Desactivó asignación a mesa", PADRON, EXITOSO),
            new Caso("CREA | MÓDULO: JRV; ENTIDAD: MiembroJRV; REGISTRO: ID 8", "Designó miembro de JRV", JRV, EXITOSO),
            new Caso("ACTUALIZA | MÓDULO: ESCRUTINIO; ENTIDAD: EscrutinioCabecera; REGISTRO: ID 5", "Actualizó escrutinio de mesa", ESCRUTINIO, EXITOSO),
            new Caso("CREA | MÓDULO: ESCRUTINIO; ENTIDAD: Escrutinio; REGISTRO: ID 6", "Registró votos del escrutinio", ESCRUTINIO, EXITOSO),
            new Caso("CREA | MÓDULO: DOCUMENTOS; ENTIDAD: Documentos; REGISTRO: ACTA-5 (ID 1)", "Registró documento", DOCUMENTOS, EXITOSO),
            new Caso("CREA | MÓDULO: DOCUMENTOS; ENTIDAD: PlantillaCorreo; REGISTRO: ID 1", "Registró plantilla de correo", DOCUMENTOS, EXITOSO),
            new Caso("CREA | MÓDULO: DOCUMENTOS; ENTIDAD: Correo; REGISTRO: ID 1", "Registró envío de correo", DOCUMENTOS, EXITOSO),
            new Caso("ACTUALIZA | MÓDULO: PROCESO ELECTORAL; ENTIDAD: CronogramaFase; REGISTRO: ID 3", "Actualizó fase del cronograma", PROCESO_ELECTORAL, EXITOSO),
            new Caso("CREA | MÓDULO: RECINTOS Y MESAS; ENTIDAD: Mesa; REGISTRO: MESA 01 (ID 12)", "Registró mesa", RECINTOS, EXITOSO),
            new Caso("CREA | MÓDULO: POSTULACIONES; ENTIDAD: Tribunal; REGISTRO: ID 2", "Designó autoridad del tribunal", POSTULACIONES, EXITOSO),
            new Caso("CREA | MÓDULO: CONFIGURACIÓN; ENTIDAD: CatalogoGeneral; REGISTRO: ID 2", "Registró catálogo", CONFIGURACION, EXITOSO),
            new Caso("CREA | MÓDULO: CONFIGURACIÓN; ENTIDAD: EntidadFutura; REGISTRO: ID 1", "Registró información", SISTEMA, EXITOSO),
            new Caso("ELIMINA | MÓDULO: CONFIGURACIÓN; ENTIDAD: EntidadFutura; REGISTRO: ID 1", "Eliminó información", SISTEMA, EXITOSO),
            // Seguridad vigente
            new Caso("ASIGNA ADMIN IGLESIA Iglesia Central -> 0102030405", "Asignó administrador de iglesia", SEGURIDAD, EXITOSO),
            new Caso("REASIGNA ADMIN IGLESIA Iglesia Central -> 0102030405 (anterior: 0999999999)", "Reasignó administrador de iglesia", SEGURIDAD, EXITOSO),
            new Caso("ENVIA CORREO REGISTRO USUARIO: jperez", "Notificó creación de usuario", SEGURIDAD, EXITOSO),
            new Caso("NOTIFICACIÓN DE REGISTRO NO ENVIADA: usuario sin correo jperez", "Notificó creación de usuario", SEGURIDAD, FALLIDO),
            // Seguridad histórica
            new Caso("CREA USUARIO: jperez ROL: SITEC-Tribunal", "Registró usuario", SEGURIDAD, SIN_DATO),
            new Caso("CREA USUARIO SIN CORREO: jperez", "Registró usuario", SEGURIDAD, SIN_DATO),
            new Caso("REACTIVA USUARIO: jperez", "Reactivó usuario", SEGURIDAD, SIN_DATO),
            new Caso("ACTUALIZA USUARIO: jperez", "Actualizó usuario", SEGURIDAD, SIN_DATO),
            new Caso("ACTUALIZA CORREO: jperez", "Actualizó correo de usuario", SEGURIDAD, SIN_DATO),
            new Caso("DESACTIVA USUARIO: jperez", "Desactivó usuario", SEGURIDAD, SIN_DATO),
            new Caso("ELIMINA USUARIO: jperez", "Eliminó usuario", SEGURIDAD, SIN_DATO),
            new Caso("ERROR  AL ELIMINAR USUARIO", "Intentó eliminar usuario", SEGURIDAD, FALLIDO),
            new Caso("CREA PERSONA: 0102030405", "Registró persona", PERSONAS, SIN_DATO),
            // Documentos y reportes
            new Caso("DESCARGA DOCUMENTO Acta parcial.pdf | ACTA-PARCIAL-5-R3-M12", "Descargó documento", DOCUMENTOS, EXITOSO),
            new Caso("DESCARGA REPORTE(PDF) ACTIVIDAD INTERNA | NÚMERO DE REGISTROS: 10", "Exportó reporte PDF", DOCUMENTOS, EXITOSO),
            new Caso("DESCARGA REPORTE(XLS) ACTIVIDAD INTERNA | NÚMERO DE REGISTROS: 10", "Exportó reporte Excel", DOCUMENTOS, EXITOSO),
            new Caso("DESCARGA PDF, LISTA PROCESOS", "Exportó reporte PDF", DOCUMENTOS, SIN_DATO),
            new Caso("DESCARGA EXCEL, SELECCIÓN DE MEDIOS", "Exportó reporte Excel", DOCUMENTOS, SIN_DATO),
            new Caso("ERROR EN DESCARGA PDF, LISTA PROCESOS", "Intentó exportar reporte PDF", DOCUMENTOS, FALLIDO),
            // Escrutinio
            new Caso("APERTURA MESA MESA 01 | 12", "Abrió mesa", ESCRUTINIO, EXITOSO),
            new Caso("GUARDA BORRADOR ACTA MESA MESA 01 | 12", "Guardó borrador del conteo", ESCRUTINIO, EXITOSO),
            new Caso("GENERA ACTA-5-M12-202611301800 | ACTA-5-M12-202611301800.pdf", "Generó acta de escrutinio", ESCRUTINIO, EXITOSO),
            new Caso("REGENERA ACTA PDF ACTA-5-M12-202611301800 | MESA 12", "Regeneró acta de escrutinio", ESCRUTINIO, EXITOSO),
            new Caso("CAMBIO ESTADO ESCRUTINIO CERRADO | MESA=12;PROCESO=5;USUARIO=u;FECHA=202611301800;ESTADO_ANTERIOR=CONTEO_REGISTRADO;ESTADO_NUEVO=CERRADO;MOTIVO=", "Cerró escrutinio", ESCRUTINIO, EXITOSO),
            new Caso("CAMBIO ESTADO ESCRUTINIO ANULADO | MESA=12;PROCESO=5;ESTADO_NUEVO=ANULADO;MOTIVO=Error", "Anuló escrutinio", ESCRUTINIO, EXITOSO),
            new Caso("CAMBIO ESTADO ESCRUTINIO REABIERTO | MESA=12", "Reabrió escrutinio", ESCRUTINIO, EXITOSO),
            new Caso("CAMBIO ESTADO ESCRUTINIO OBSERVADO | MESA=12", "Marcó escrutinio como observado", ESCRUTINIO, EXITOSO),
            new Caso("CAMBIO ESTADO ESCRUTINIO EN_CONTEO | MESA=12", "Cambió estado del escrutinio", ESCRUTINIO, EXITOSO),
            new Caso("GENERA ACTA_PARCIAL | ACTA-PARCIAL-5-R3-M12", "Generó acta parcial de escrutinio", ESCRUTINIO, EXITOSO),
            new Caso("REGENERA ACTA_PARCIAL | ACTA-PARCIAL-5-R3-M12", "Regeneró acta parcial de escrutinio", ESCRUTINIO, EXITOSO),
            new Caso("GENERA ACTA PARCIAL DE ESCRUTINIO | ACTA-PARCIAL-5", "Generó acta parcial de escrutinio", ESCRUTINIO, EXITOSO),
            // Padrón
            new Caso("GENERA PADRON_MESA | PADRON-5-M12", "Generó padrón de mesa", PADRON, EXITOSO),
            new Caso("REGENERA PADRON_MESA | PADRON-5-M12", "Regeneró padrón de mesa", PADRON, EXITOSO),
            new Caso("GENERA PADRON ELECTORAL DE MESA | PADRON-5", "Generó padrón de mesa", PADRON, EXITOSO),
            new Caso("GENERA CERTIFICADOS_VOTACION | CERT-5", "Generó certificados de votación", PADRON, EXITOSO),
            new Caso("REGENERA CERTIFICADOS_VOTACION | CERT-5", "Regeneró certificados de votación", PADRON, EXITOSO),
            new Caso("GENERA CERTIFICADOS DE VOTACION | CERT-5", "Generó certificados de votación", PADRON, EXITOSO),
            new Caso("BUSCA LUGAR VOTACION 0102030405", "Consultó lugar de votación", PADRON, SIN_DATO),
            // Sistema histórico
            new Caso("BUSCAR PROCESO", "Consultó bitácora", SISTEMA, SIN_DATO),
            new Caso("ERROR BUSCAR PROCESO", "Intentó consultar bitácora", SISTEMA, FALLIDO),
            // No reconocidos: nunca se inventa la acción
            // "_" es comodín en LIKE: sin escape, esto coincidiría con "genera acta_parcial |".
            new Caso("GENERA ACTAXPARCIAL | parecido pero distinto", OTRA_ACTIVIDAD, SISTEMA, SIN_DATO),
            new Caso("GENERA ACTA_FISICA_ESCRUTINIO | X", OTRA_ACTIVIDAD, SISTEMA, SIN_DATO),
            new Caso("LOGOUT | MÓDULO: ACCESO; RESULTADO: EXITOSO; DETALLE: otro texto", OTRA_ACTIVIDAD, SISTEMA, SIN_DATO),
            new Caso("ALGO QUE TEC NO RECONOCE", OTRA_ACTIVIDAD, SISTEMA, SIN_DATO),
            new Caso("", OTRA_ACTIVIDAD, SISTEMA, SIN_DATO),
            new Caso(null, OTRA_ACTIVIDAD, SISTEMA, SIN_DATO));

    @Test void cadaValorTecnicoSeTraduceALaDescripcionEsperada() {
        for (Caso caso : CASOS) {
            var n = normalizar(caso.tecnico());
            assertEquals(caso.accion(), n.accion(), "Acción de: " + caso.tecnico());
            assertEquals(caso.modulo(), n.modulo(), "Módulo de: " + caso.tecnico());
            assertEquals(caso.resultado(), n.resultado(), "Resultado de: " + caso.tecnico());
            assertNotNull(n.detalle());
            assertFalse(n.detalle().isBlank());
        }
    }

    @Test void lasFirmasEspecificasSonMutuamenteExcluyentes() {
        for (Caso caso : CASOS) {
            if (caso.tecnico() == null) continue;
            String texto = minusculas(caso.tecnico());
            long especificas = reglas().stream().filter(r -> !r.auxiliar() && r.coincide(texto)).count();
            assertTrue(especificas <= 1, "Más de una regla específica para: " + caso.tecnico());
        }
    }

    @Test void elPatronLikeEquivaleALaFirmaJava() {
        for (CatalogoActividades.Regla regla : reglas()) {
            for (CatalogoActividades.Firma firma : regla.firmas()) {
                Pattern like = comoRegex(firma.patronLike());
                for (Caso caso : CASOS) {
                    if (caso.tecnico() == null) continue;
                    String texto = minusculas(caso.tecnico());
                    assertEquals(firma.coincide(texto), like.matcher(texto).matches(),
                            "Firma " + firma + " vs " + caso.tecnico());
                }
            }
        }
    }

    /** Lo que el usuario ve y lo que el filtro devuelve en BD deben coincidir. */
    @Test void filtrarPorAccionModuloYResultadoDevuelveExactamenteLoMostrado() {
        List<String> acciones = new ArrayList<>();
        accionesPorModulo().values().forEach(acciones::addAll);
        for (String accion : acciones) {
            verificarFiltro(criterioPorAccion(accion), n -> n.accion().equals(accion), "acción " + accion);
        }
        for (String modulo : modulos()) {
            verificarFiltro(criterioPorModulo(modulo), n -> n.modulo().equals(modulo), "módulo " + modulo);
        }
        for (String resultado : resultados()) {
            verificarFiltro(criterioPorResultado(resultado), n -> n.resultado().equals(resultado),
                    "resultado " + resultado);
        }
    }

    @Test void valorDeFiltroDesconocidoNoDevuelveFilas() {
        assertTrue(criterioPorAccion("DROP TABLE").vacio());
        assertTrue(criterioPorModulo("CREA").vacio());
        assertTrue(criterioPorResultado("EXITOSO").vacio());
    }

    @Test void laBusquedaGeneralEncuentraLaAccionMostradaSinTildes() {
        CriterioFiltro criterio = criterioPorTextoDeAccion("registro persona");
        assertFalse(criterio.vacio());
        assertTrue(selecciona(criterio, "CREA | MÓDULO: PERSONAS E IGLESIAS; ENTIDAD: Persona; REGISTRO: ID 1"));
        assertTrue(selecciona(criterio, "CREA PERSONA: 0102030405"));
        assertFalse(selecciona(criterio, "CREA | MÓDULO: SEGURIDAD; ENTIDAD: Usuario; REGISTRO: ID 1"));
        assertTrue(criterioPorTextoDeAccion("   ").vacio());
    }

    @Test void cadaAccionPerteneceAUnSoloModulo() {
        Map<String, String> moduloPorAccion = new HashMap<>();
        for (CatalogoActividades.Regla regla : reglas()) {
            String previo = moduloPorAccion.putIfAbsent(regla.accion(), regla.modulo());
            assertTrue(previo == null || previo.equals(regla.modulo()),
                    "La acción '" + regla.accion() + "' aparece en " + previo + " y " + regla.modulo());
        }
    }

    @Test void elDetalleIndicaSobreQueRegistroSeActuoSinPerderContenido() {
        assertEquals("Persona: ID 55",
                normalizar("CREA | MÓDULO: PERSONAS E IGLESIAS; ENTIDAD: Persona; REGISTRO: ID 55").detalle());
        assertEquals("Miembro de iglesia: Persona 0102030405 · Iglesia Central",
                normalizar("CREA | MÓDULO: X; ENTIDAD: IglesiaPersona; REGISTRO: Persona 0102030405; Iglesia Central").detalle());
        assertEquals("EntidadFutura: ID 1",
                normalizar("CREA | MÓDULO: X; ENTIDAD: EntidadFutura; REGISTRO: ID 1").detalle());
        assertEquals("Iglesia: Iglesia Central · Administrador: 0102030405 (anterior: 0999999999)",
                normalizar("REASIGNA ADMIN IGLESIA Iglesia Central -> 0102030405 (anterior: 0999999999)").detalle());
        assertEquals("Mesa: MESA 01 · ID de mesa: 12", normalizar("APERTURA MESA MESA 01 | 12").detalle());
        assertEquals("Documento: Acta parcial.pdf · Código: ACTA-PARCIAL-5",
                normalizar("DESCARGA DOCUMENTO Acta parcial.pdf | ACTA-PARCIAL-5").detalle());
        assertEquals("Acta: ACTA-5-M12 · Archivo: ACTA-5-M12.pdf",
                normalizar("GENERA ACTA-5-M12 | ACTA-5-M12.pdf").detalle());
        assertEquals("Código: ACTA-PARCIAL-5-R3-M12",
                normalizar("GENERA ACTA_PARCIAL | ACTA-PARCIAL-5-R3-M12").detalle());
        assertEquals("ID de mesa: 12 · ID de proceso: 5 · De Conteo registrado a Cerrado",
                normalizar("CAMBIO ESTADO ESCRUTINIO CERRADO | MESA=12;PROCESO=5;USUARIO=u;FECHA=x;"
                        + "ESTADO_ANTERIOR=CONTEO_REGISTRADO;ESTADO_NUEVO=CERRADO;MOTIVO=").detalle());
        assertEquals("Inicio de sesión",
                normalizar("LOGIN | MÓDULO: ACCESO; RESULTADO: EXITOSO; DETALLE: Inicio de sesión").detalle());
        assertEquals("Usuario: jperez", normalizar("ENVIA CORREO REGISTRO USUARIO: jperez").detalle());
        assertEquals("Usuario sin correo jperez",
                normalizar("NOTIFICACIÓN DE REGISTRO NO ENVIADA: usuario sin correo jperez").detalle());
        assertEquals("ALGO QUE TEC NO RECONOCE · con datos", normalizar("ALGO QUE TEC NO RECONOCE | con datos").detalle());
        assertEquals("Sin detalle", normalizar(null).detalle());
    }

    @Test void espaciosExternosSeTratanIgualQueTrimSql() {
        assertEquals("Inició sesión", normalizar("   INGRESA AL SISTEMA CORRECTAMENTE  ").accion());
        assertEquals("abc", comoTrimSql("  abc "));
        assertEquals("\tabc", comoTrimSql(" \tabc"));
        assertEquals("", comoTrimSql(null));
    }

    @Test void severidadesDelResultado() {
        assertEquals("success", severidad(EXITOSO));
        assertEquals("danger", severidad(FALLIDO));
        assertEquals("warning", severidad(EXPIRADO));
        assertEquals("warning", severidad(CANCELADO));
        assertEquals("info", severidad(SOLICITADO));
        assertEquals("secondary", severidad(SIN_DATO));
    }

    // ------------------------------------------------------------ Apoyo

    private interface Condicion {
        boolean cumple(CatalogoActividades.ActividadNormalizada n);
    }

    private static void verificarFiltro(CriterioFiltro criterio, Condicion esperado, String nombre) {
        for (Caso caso : CASOS) {
            boolean mostrado = esperado.cumple(normalizar(caso.tecnico()));
            assertEquals(mostrado, selecciona(criterio, caso.tecnico()),
                    "Filtro por " + nombre + " no coincide con lo mostrado para: " + caso.tecnico());
        }
    }

    /** Réplica en Java de la condición JPQL generada por ProcesoFacade. */
    private static boolean selecciona(CriterioFiltro c, String actividad) {
        if (c.vacio()) return false;
        String texto = actividad == null ? null : minusculas(actividad);
        boolean incluido = texto != null && algun(c.especificos(), texto);
        incluido |= texto != null && algun(c.auxiliares(), texto) && !algun(c.todosEspecificos(), texto);
        incluido |= c.sinClasificar() && (texto == null || !algun(c.todos(), texto));
        return incluido;
    }

    private static boolean algun(List<String> patrones, String texto) {
        return patrones.stream().anyMatch(p -> comoRegex(p).matcher(texto).matches());
    }

    private static String minusculas(String actividad) {
        return comoTrimSql(actividad).toLowerCase(Locale.ROOT);
    }

    /** Interpreta LIKE con ESCAPE '!' igual que PostgreSQL. */
    private static Pattern comoRegex(String like) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < like.length(); i++) {
            char c = like.charAt(i);
            if (c == ESCAPE && i + 1 < like.length()) {
                regex.append(Pattern.quote(String.valueOf(like.charAt(++i))));
            } else if (c == '%') {
                regex.append(".*");
            } else if (c == '_') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(regex.toString(), Pattern.DOTALL);
    }
}
