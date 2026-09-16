package ec.com.antenasur.audit;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Catálogo único que traduce el texto técnico guardado en
 * {@code tec.procesos.actividad} a Módulo, Acción, Resultado y Detalle
 * comprensibles. La traducción es solo de lectura: no modifica los registros.
 *
 * <p>Cada {@link Regla} se reconoce por {@link Firma}s (prefijo y fragmentos en
 * orden). La misma firma se usa para mostrar el registro y para filtrarlo en BD
 * con {@code LIKE}, de modo que lo que el usuario ve y lo que filtra coinciden.
 * Las firmas de reglas no auxiliares son mutuamente excluyentes (lo verifica
 * {@code CatalogoActividadesTest}); las reglas auxiliares solo aplican cuando
 * ninguna específica coincide. Lo no reconocido se muestra como
 * {@link #OTRA_ACTIVIDAD} con el texto original como detalle.
 */
public final class CatalogoActividades {

	public static final String EXITOSO = "Exitoso";
	public static final String FALLIDO = "Fallido";
	public static final String EXPIRADO = "Expirado";
	public static final String CANCELADO = "Cancelado";
	public static final String SOLICITADO = "Solicitado";
	public static final String SIN_DATO = "Sin dato";

	public static final String ACCESO = "Acceso";
	public static final String SEGURIDAD = "Seguridad";
	public static final String PERSONAS = "Personas e iglesias";
	public static final String PROCESO_ELECTORAL = "Proceso electoral";
	public static final String RECINTOS = "Recintos y mesas";
	public static final String PADRON = "Padrón";
	public static final String JRV = "JRV";
	public static final String ESCRUTINIO = "Escrutinio";
	public static final String DOCUMENTOS = "Documentos";
	public static final String POSTULACIONES = "Postulaciones";
	public static final String CONFIGURACION = "Configuración";
	public static final String SISTEMA = "Sistema";

	public static final String OTRA_ACTIVIDAD = "Otra actividad";
	private static final String SIN_DETALLE = "Sin detalle";
	private static final int LARGO_DETALLE = 500;

	/** Carácter de escape de LIKE; se evita la barra invertida entre capas. */
	public static final char ESCAPE = '!';

	private static final List<String> ORDEN_MODULOS = List.of(ACCESO, SEGURIDAD, PERSONAS, PROCESO_ELECTORAL,
			RECINTOS, PADRON, JRV, ESCRUTINIO, DOCUMENTOS, POSTULACIONES, CONFIGURACION, SISTEMA);
	private static final List<String> ORDEN_RESULTADOS = List.of(EXITOSO, FALLIDO, EXPIRADO, CANCELADO,
			SOLICITADO, SIN_DATO);

	private static final Pattern REGISTRO = Pattern.compile("REGISTRO\\s*:\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern DETALLE = Pattern.compile("DETALLE\\s*:\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENTIDAD = Pattern.compile("ENTIDAD\\s*:\\s*([^;]+)", Pattern.CASE_INSENSITIVE);

	private CatalogoActividades() {
	}

	// ------------------------------------------------------------------ API

	/** Resultado de la traducción de una actividad almacenada. */
	public record ActividadNormalizada(String modulo, String accion, String resultado, String detalle) {
	}

	/**
	 * Condición de filtro expresada con patrones {@code LIKE} (minúsculas,
	 * escapados con {@link #ESCAPE}) sobre {@code LOWER(actividad)}:
	 * <pre>
	 * (LIKE any especificos)
	 * OR ((LIKE any auxiliares) AND NOT (LIKE any todosEspecificos))
	 * OR (sinClasificar AND (actividad IS NULL OR NOT (LIKE any todos)))
	 * </pre>
	 * Si {@link #vacio()} es verdadero, el filtro no debe devolver filas.
	 */
	public record CriterioFiltro(List<String> especificos, List<String> auxiliares, List<String> todosEspecificos,
			boolean sinClasificar, List<String> todos) {
		public boolean vacio() {
			return especificos.isEmpty() && auxiliares.isEmpty() && !sinClasificar;
		}
	}

	public static ActividadNormalizada normalizar(String actividad) {
		String original = comoTrimSql(actividad);
		String minusculas = original.toLowerCase(Locale.ROOT);
		Regla regla = reglaPara(minusculas);
		if (regla == null) {
			return new ActividadNormalizada(SISTEMA, OTRA_ACTIVIDAD, SIN_DATO, limpiar(original));
		}
		return new ActividadNormalizada(regla.modulo(), regla.accion(), regla.resultado(),
				regla.detalle().formatear(original));
	}

	/** Severidad de {@code p:tag} para un resultado mostrado. */
	public static String severidad(String resultado) {
		if (EXITOSO.equals(resultado)) return "success";
		if (FALLIDO.equals(resultado)) return "danger";
		if (EXPIRADO.equals(resultado) || CANCELADO.equals(resultado)) return "warning";
		if (SOLICITADO.equals(resultado)) return "info";
		return "secondary";
	}

	/** Acciones agrupadas por módulo, en el orden de presentación. */
	public static Map<String, List<String>> accionesPorModulo() {
		Map<String, LinkedHashSet<String>> agrupadas = new LinkedHashMap<>();
		for (String modulo : ORDEN_MODULOS) agrupadas.put(modulo, new LinkedHashSet<>());
		for (Regla regla : REGLAS) agrupadas.get(regla.modulo()).add(regla.accion());
		agrupadas.get(SISTEMA).add(OTRA_ACTIVIDAD);
		Map<String, List<String>> resultado = new LinkedHashMap<>();
		agrupadas.forEach((modulo, acciones) -> {
			if (!acciones.isEmpty()) {
				List<String> ordenadas = new ArrayList<>(acciones);
				ordenadas.sort(String.CASE_INSENSITIVE_ORDER);
				resultado.put(modulo, Collections.unmodifiableList(ordenadas));
			}
		});
		return Collections.unmodifiableMap(resultado);
	}

	public static List<String> modulos() {
		return ORDEN_MODULOS;
	}

	public static List<String> resultados() {
		return ORDEN_RESULTADOS;
	}

	public static CriterioFiltro criterioPorAccion(String accion) {
		return criterio(r -> r.accion().equals(accion), OTRA_ACTIVIDAD.equals(accion));
	}

	public static CriterioFiltro criterioPorModulo(String modulo) {
		return criterio(r -> r.modulo().equals(modulo), SISTEMA.equals(modulo));
	}

	public static CriterioFiltro criterioPorResultado(String resultado) {
		return criterio(r -> r.resultado().equals(resultado), SIN_DATO.equals(resultado));
	}

	/**
	 * Acciones cuyo nombre contiene el texto buscado (sin distinguir tildes ni
	 * mayúsculas), para que la búsqueda general encuentre lo que se muestra.
	 */
	public static CriterioFiltro criterioPorTextoDeAccion(String texto) {
		String buscado = sinTildes(texto);
		if (buscado.isBlank()) return criterio(r -> false, false);
		return criterio(r -> sinTildes(r.accion()).contains(buscado),
				sinTildes(OTRA_ACTIVIDAD).contains(buscado));
	}

	/**
	 * Recorta solo espacios, igual que {@code TRIM} de JPQL/SQL, para que la
	 * clasificación mostrada y la del filtro en BD usen exactamente el mismo texto.
	 */
	public static String comoTrimSql(String texto) {
		if (texto == null) return "";
		int inicio = 0;
		int fin = texto.length();
		while (inicio < fin && texto.charAt(inicio) == ' ') inicio++;
		while (fin > inicio && texto.charAt(fin - 1) == ' ') fin--;
		return texto.substring(inicio, fin);
	}

	// ------------------------------------------------------------ Internos

	static List<Regla> reglas() {
		return REGLAS;
	}

	static Regla reglaPara(String minusculas) {
		Regla auxiliar = null;
		for (Regla regla : REGLAS) {
			if (!regla.coincide(minusculas)) continue;
			if (!regla.auxiliar()) return regla;
			if (auxiliar == null) auxiliar = regla;
		}
		return auxiliar;
	}

	private static CriterioFiltro criterio(Predicate<Regla> seleccion, boolean sinClasificar) {
		List<String> especificos = new ArrayList<>();
		List<String> auxiliares = new ArrayList<>();
		List<String> todosEspecificos = new ArrayList<>();
		List<String> todos = new ArrayList<>();
		for (Regla regla : REGLAS) {
			for (Firma firma : regla.firmas()) {
				String patron = firma.patronLike();
				todos.add(patron);
				if (!regla.auxiliar()) todosEspecificos.add(patron);
				if (seleccion.test(regla)) (regla.auxiliar() ? auxiliares : especificos).add(patron);
			}
		}
		return new CriterioFiltro(List.copyOf(especificos), List.copyOf(auxiliares),
				List.copyOf(todosEspecificos), sinClasificar, List.copyOf(todos));
	}

	/** Firma técnica: prefijo y fragmentos que deben aparecer en ese orden. */
	record Firma(String prefijo, List<String> fragmentos) {
		boolean coincide(String minusculas) {
			if (!minusculas.startsWith(prefijo)) return false;
			int desde = prefijo.length();
			for (String fragmento : fragmentos) {
				int posicion = minusculas.indexOf(fragmento, desde);
				if (posicion < 0) return false;
				desde = posicion + fragmento.length();
			}
			return true;
		}

		String patronLike() {
			StringBuilder patron = new StringBuilder(escapar(prefijo)).append('%');
			for (String fragmento : fragmentos) patron.append(escapar(fragmento)).append('%');
			return patron.toString();
		}

		private static String escapar(String literal) {
			StringBuilder salida = new StringBuilder();
			for (char c : literal.toCharArray()) {
				if (c == ESCAPE || c == '%' || c == '_') salida.append(ESCAPE);
				salida.append(c);
			}
			return salida.toString();
		}
	}

	record Regla(String accion, String modulo, String resultado, List<Firma> firmas, boolean auxiliar,
			FormatoDetalle detalle) {
		boolean coincide(String minusculas) {
			for (Firma firma : firmas) if (firma.coincide(minusculas)) return true;
			return false;
		}
	}

	@FunctionalInterface
	interface FormatoDetalle {
		String formatear(String original);
	}

	// --------------------------------------------------------- Definición

	private static List<Regla> construirReglas() {
		List<Regla> r = new ArrayList<>();

		// Acceso: formato estructurado vigente (LoginController, LoginBean, ProcesoFacade).
		r.add(regla("Inició sesión", ACCESO, EXITOSO, DETALLE_ESTRUCTURADO,
				firma("login |", "resultado: exitoso")));
		r.add(regla("Intentó iniciar sesión", ACCESO, FALLIDO, DETALLE_ESTRUCTURADO,
				firma("login |", "resultado: fallido")));
		r.add(regla("Cerró sesión", ACCESO, EXITOSO, DETALLE_ESTRUCTURADO,
				firma("logout |", "resultado: exitoso", "detalle: cierre de sesi")));
		// LoginBean.passwordChangued() solo se invoca tras un cambio de clave correcto.
		r.add(regla("Cambió contraseña", ACCESO, EXITOSO, DETALLE_ESTRUCTURADO,
				firma("logout |", "resultado: exitoso", "detalle: cierre tras cambio de clave")));
		r.add(regla("Sesión expirada por inactividad", ACCESO, EXPIRADO, DETALLE_ESTRUCTURADO,
				firma("logout |", "resultado: expirado")));
		r.add(regla("Canceló cambio obligatorio de contraseña", ACCESO, CANCELADO, DETALLE_ESTRUCTURADO,
				firma("logout |", "resultado: cancelado")));
		r.add(regla("Solicitó recuperación de contraseña", ACCESO, SOLICITADO, DETALLE_ESTRUCTURADO,
				firma("recuperacion_clave |")));
		// Acceso: textos históricos.
		r.add(regla("Inició sesión", ACCESO, EXITOSO, ORIGINAL, firma("ingresa al sistema")));
		r.add(regla("Cerró sesión", ACCESO, EXITOSO, ORIGINAL, firma("sale del ")));
		r.add(regla("Intentó iniciar sesión", ACCESO, FALLIDO, ORIGINAL, firma("error de ingreso al sistema")));
		r.add(regla("Solicitó recuperación de contraseña", ACCESO, SIN_DATO, ORIGINAL,
				firma("recupera clave olvidado")));
		r.add(regla("Cambió contraseña", ACCESO, SIN_DATO, ORIGINAL, firma("cambia contrase")));

		// Seguridad: registros funcionales vigentes.
		r.add(regla("Asignó administrador de iglesia", SEGURIDAD, EXITOSO, ADMIN_IGLESIA,
				firma("asigna admin iglesia ")));
		r.add(regla("Reasignó administrador de iglesia", SEGURIDAD, EXITOSO, ADMIN_IGLESIA,
				firma("reasigna admin iglesia ")));
		r.add(regla("Notificó creación de usuario", SEGURIDAD, EXITOSO,
				resto("envia correo registro usuario", "Usuario", null), firma("envia correo registro usuario")));
		r.add(regla("Notificó creación de usuario", SEGURIDAD, FALLIDO, DESPUES_DE_DOS_PUNTOS,
				firma("notificaci", "n de registro no enviada")));
		// Seguridad: textos históricos.
		r.add(regla("Registró usuario", SEGURIDAD, SIN_DATO, DESPUES_DE_DOS_PUNTOS, firma("crea usuario")));
		r.add(regla("Reactivó usuario", SEGURIDAD, SIN_DATO, DESPUES_DE_DOS_PUNTOS, firma("reactiva usuario")));
		r.add(regla("Actualizó usuario", SEGURIDAD, SIN_DATO, DESPUES_DE_DOS_PUNTOS, firma("actualiza usuario:")));
		r.add(regla("Actualizó correo de usuario", SEGURIDAD, SIN_DATO, DESPUES_DE_DOS_PUNTOS,
				firma("actualiza correo:")));
		r.add(regla("Desactivó usuario", SEGURIDAD, SIN_DATO, DESPUES_DE_DOS_PUNTOS, firma("desactiva usuario:")));
		r.add(regla("Eliminó usuario", SEGURIDAD, SIN_DATO, DESPUES_DE_DOS_PUNTOS, firma("elimina usuario:")));
		r.add(regla("Intentó eliminar usuario", SEGURIDAD, FALLIDO, ORIGINAL,
				firma("error al eliminar usuario"), firma("error  al eliminar usuario")));
		r.add(regla("Registró persona", PERSONAS, SIN_DATO, DESPUES_DE_DOS_PUNTOS, firma("crea persona:")));

		// Documentos y reportes (okActivityRegister se invoca tras completar la operación).
		r.add(regla("Descargó documento", DOCUMENTOS, EXITOSO,
				resto("descarga documento ", "Documento", "Código"), firma("descarga documento ")));
		r.add(regla("Exportó reporte PDF", DOCUMENTOS, EXITOSO,
				resto("descarga reporte(pdf)", "Reporte", null), firma("descarga reporte(pdf)")));
		r.add(regla("Exportó reporte Excel", DOCUMENTOS, EXITOSO,
				resto("descarga reporte(xls)", "Reporte", null), firma("descarga reporte(xls)")));
		r.add(regla("Exportó reporte PDF", DOCUMENTOS, SIN_DATO, ORIGINAL, firma("descarga pdf, lista procesos")));
		r.add(regla("Exportó reporte Excel", DOCUMENTOS, SIN_DATO, ORIGINAL, firma("descarga excel, selecci")));
		r.add(regla("Intentó exportar reporte PDF", DOCUMENTOS, FALLIDO, ORIGINAL,
				firma("error en descarga pdf")));

		// Escrutinio: acta de mesa (ActaEController).
		r.add(regla("Abrió mesa", ESCRUTINIO, EXITOSO, resto("apertura mesa ", "Mesa", "ID de mesa"),
				firma("apertura mesa ")));
		r.add(regla("Guardó borrador del conteo", ESCRUTINIO, EXITOSO,
				resto("guarda borrador acta mesa ", "Mesa", "ID de mesa"), firma("guarda borrador acta mesa ")));
		r.add(regla("Generó acta de escrutinio", ESCRUTINIO, EXITOSO, resto("genera ", "Acta", "Archivo"),
				firma("genera acta-")));
		r.add(regla("Regeneró acta de escrutinio", ESCRUTINIO, EXITOSO,
				resto("regenera acta pdf ", "Acta", null), firma("regenera acta pdf ")));
		agregarCambiosEstado(r);
		// Escrutinio y padrón: documentos de mesa (ReporteMesaController).
		r.add(regla("Generó acta parcial de escrutinio", ESCRUTINIO, EXITOSO,
				resto("genera acta_parcial", null, "Código"), firma("genera acta_parcial |")));
		r.add(regla("Generó acta parcial de escrutinio", ESCRUTINIO, EXITOSO,
				resto("genera acta parcial de escrutinio", null, "Código"),
				firma("genera acta parcial de escrutinio")));
		r.add(regla("Regeneró acta parcial de escrutinio", ESCRUTINIO, EXITOSO,
				resto("regenera acta_parcial", null, "Código"), firma("regenera acta_parcial |")));
		r.add(regla("Generó padrón de mesa", PADRON, EXITOSO, resto("genera padron_mesa", null, "Código"),
				firma("genera padron_mesa |")));
		r.add(regla("Generó padrón de mesa", PADRON, EXITOSO,
				resto("genera padron electoral de mesa", null, "Código"), firma("genera padron electoral de mesa")));
		r.add(regla("Regeneró padrón de mesa", PADRON, EXITOSO, resto("regenera padron_mesa", null, "Código"),
				firma("regenera padron_mesa |")));
		r.add(regla("Generó certificados de votación", PADRON, EXITOSO,
				resto("genera certificados_votacion", null, "Código"), firma("genera certificados_votacion |")));
		r.add(regla("Generó certificados de votación", PADRON, EXITOSO,
				resto("genera certificados de votacion", null, "Código"),
				firma("genera certificados de votacion")));
		r.add(regla("Regeneró certificados de votación", PADRON, EXITOSO,
				resto("regenera certificados_votacion", null, "Código"),
				firma("regenera certificados_votacion |")));
		r.add(regla("Consultó lugar de votación", PADRON, SIN_DATO,
				resto("busca lugar votacion ", "Búsqueda", null), firma("busca lugar votacion ")));

		// Sistema: textos históricos de la bitácora.
		r.add(regla("Consultó bitácora", SISTEMA, SIN_DATO, ORIGINAL, firma("buscar proceso")));
		r.add(regla("Intentó consultar bitácora", SISTEMA, FALLIDO, ORIGINAL, firma("error buscar proceso")));

		// Cambios automáticos de AbstractFacade sobre entidades @Audited.
		agregarPersistencia(r);
		return Collections.unmodifiableList(r);
	}

	private static void agregarCambiosEstado(List<Regla> r) {
		Map<String, String> acciones = new LinkedHashMap<>();
		acciones.put("cerrado", "Cerró escrutinio");
		acciones.put("anulado", "Anuló escrutinio");
		acciones.put("reabierto", "Reabrió escrutinio");
		acciones.put("observado", "Marcó escrutinio como observado");
		for (String estado : List.of("pendiente", "abierto", "en_conteo", "conteo_registrado")) {
			acciones.put(estado, "Cambió estado del escrutinio");
		}
		acciones.forEach((estado, accion) -> r.add(regla(accion, ESCRUTINIO, EXITOSO, CAMBIO_ESTADO,
				firma("cambio estado escrutinio " + estado + " |"))));
	}

	/** Entidad → sustantivo mostrado, módulo y verbo de creación cuando no es "Registró". */
	private record Entidad(String nombre, String sustantivo, String modulo, String accionCrear) {
	}

	private static void agregarPersistencia(List<Regla> r) {
		List<Entidad> entidades = List.of(
				new Entidad("Usuario", "usuario", SEGURIDAD, null),
				new Entidad("Rol", "rol", SEGURIDAD, null),
				new Entidad("RolUsuario", "rol de usuario", SEGURIDAD, "Asignó rol a usuario"),
				new Entidad("Menu", "opción de menú", SEGURIDAD, null),
				new Entidad("MenuRol", "permiso de menú", SEGURIDAD, "Asignó permiso de menú a rol"),
				new Entidad("Persona", "persona", PERSONAS, null),
				new Entidad("Iglesia", "iglesia", PERSONAS, null),
				new Entidad("IglesiaPersona", "miembro de iglesia", PERSONAS, "Asignó persona a iglesia"),
				new Entidad("ProcesoElectoral", "proceso electoral", PROCESO_ELECTORAL, null),
				new Entidad("Periodo", "periodo", PROCESO_ELECTORAL, null),
				new Entidad("CronogramaFase", "fase del cronograma", PROCESO_ELECTORAL, null),
				new Entidad("Recinto", "recinto", RECINTOS, null),
				new Entidad("Mesa", "mesa", RECINTOS, null),
				new Entidad("Padron", "asignación a mesa", PADRON, "Asignó miembro a mesa"),
				new Entidad("MiembroJRV", "miembro de JRV", JRV, "Designó miembro de JRV"),
				new Entidad("Cargo", "cargo", JRV, null),
				new Entidad("Escrutinio", "votos del escrutinio", ESCRUTINIO, null),
				new Entidad("EscrutinioCabecera", "escrutinio de mesa", ESCRUTINIO, null),
				new Entidad("CategoriaVoto", "categoría de voto", ESCRUTINIO, null),
				new Entidad("Documentos", "documento", DOCUMENTOS, null),
				new Entidad("PlantillaCorreo", "plantilla de correo", DOCUMENTOS, null),
				new Entidad("Correo", "envío de correo", DOCUMENTOS, null),
				new Entidad("Lista", "lista", POSTULACIONES, null),
				new Entidad("Candidato", "candidato", POSTULACIONES, null),
				new Entidad("Tribunal", "autoridad del tribunal", POSTULACIONES, "Designó autoridad del tribunal"),
				new Entidad("CatalogoGeneral", "catálogo", CONFIGURACION, null),
				new Entidad("TipoDocumento", "tipo de documento", CONFIGURACION, null));
		Map<String, String> verbos = new LinkedHashMap<>();
		verbos.put("crea", "Registró");
		verbos.put("actualiza", "Actualizó");
		verbos.put("desactiva", "Desactivó");
		verbos.put("elimina", "Eliminó");
		for (Entidad entidad : entidades) {
			String clave = "entidad: " + entidad.nombre().toLowerCase(Locale.ROOT) + ";";
			verbos.forEach((codigo, verbo) -> {
				String accion = "crea".equals(codigo) && entidad.accionCrear() != null
						? entidad.accionCrear() : verbo + " " + entidad.sustantivo();
				r.add(regla(accion, entidad.modulo(), EXITOSO, registro(entidad.sustantivo()),
						firma(codigo + " |", clave)));
			});
		}
		// Entidades auditadas que se agreguen en el futuro y aún no estén catalogadas.
		verbos.forEach((codigo, verbo) -> r.add(new Regla(verbo + " información", SISTEMA, EXITOSO,
				List.of(firma(codigo + " |", "entidad: ")), true, REGISTRO_CON_ENTIDAD)));
	}

	private static Regla regla(String accion, String modulo, String resultado, FormatoDetalle detalle,
			Firma... firmas) {
		return new Regla(accion, modulo, resultado, List.of(firmas), false, detalle);
	}

	private static Firma firma(String prefijo, String... fragmentos) {
		return new Firma(prefijo, List.of(fragmentos));
	}

	// ------------------------------------------------------------- Detalle

	private static final FormatoDetalle ORIGINAL = CatalogoActividades::limpiar;

	private static final FormatoDetalle DETALLE_ESTRUCTURADO = original -> {
		Matcher m = DETALLE.matcher(original);
		return m.find() ? limpiar(original.substring(m.end())) : limpiar(original);
	};

	private static final FormatoDetalle DESPUES_DE_DOS_PUNTOS = original -> {
		int dosPuntos = original.indexOf(':');
		return dosPuntos < 0 ? limpiar(original) : limpiar(original.substring(dosPuntos + 1));
	};

	private static final FormatoDetalle REGISTRO_CON_ENTIDAD = original -> {
		Matcher entidad = ENTIDAD.matcher(original);
		String nombre = entidad.find() ? entidad.group(1).trim() : "Registro";
		return etiquetar(nombre, valorRegistro(original));
	};

	private static FormatoDetalle registro(String sustantivo) {
		return original -> etiquetar(capitalizar(sustantivo), valorRegistro(original));
	}

	private static String valorRegistro(String original) {
		Matcher m = REGISTRO.matcher(original);
		return m.find() ? original.substring(m.end()).replace("; ", " · ") : "";
	}

	/**
	 * Quita el código técnico inicial y separa "antes | después" en partes
	 * etiquetadas, sin descartar el contenido.
	 */
	private static FormatoDetalle resto(String quitar, String etiquetaAntes, String etiquetaDespues) {
		return original -> {
			String texto = original.regionMatches(true, 0, quitar, 0, quitar.length())
					? original.substring(quitar.length()) : original;
			// Se conserva un "|" inicial: indica que no hay parte "antes".
			texto = texto.replaceFirst("^[\\s:]+", "");
			String[] partes = texto.split("\\s*\\|\\s*", 2);
			List<String> salida = new ArrayList<>();
			if (!partes[0].isBlank()) salida.add(etiquetar(etiquetaAntes, partes[0]));
			if (partes.length > 1 && !partes[1].isBlank()) salida.add(etiquetar(etiquetaDespues, partes[1]));
			return salida.isEmpty() ? SIN_DETALLE : recortar(String.join(" · ", salida));
		};
	}

	private static final FormatoDetalle ADMIN_IGLESIA = original -> {
		String texto = original.replaceFirst("(?i)^(re)?asigna admin iglesia\\s*", "");
		String[] partes = texto.split("\\s*->\\s*", 2);
		String detalle = etiquetar("Iglesia", partes[0]);
		if (partes.length > 1) detalle += " · " + etiquetar("Administrador", partes[1]);
		return recortar(detalle);
	};

	private static final FormatoDetalle CAMBIO_ESTADO = original -> {
		int separador = original.indexOf('|');
		if (separador < 0) return limpiar(original);
		Map<String, String> datos = new LinkedHashMap<>();
		for (String par : original.substring(separador + 1).split(";")) {
			String[] kv = par.split("=", 2);
			if (kv.length == 2) datos.put(kv[0].trim().toUpperCase(Locale.ROOT), kv[1].trim());
		}
		List<String> salida = new ArrayList<>();
		if (!datos.getOrDefault("MESA", "").isBlank()) salida.add("ID de mesa: " + datos.get("MESA"));
		if (!datos.getOrDefault("PROCESO", "").isBlank()) salida.add("ID de proceso: " + datos.get("PROCESO"));
		String anterior = estadoLegible(datos.get("ESTADO_ANTERIOR"));
		String nuevo = estadoLegible(datos.get("ESTADO_NUEVO"));
		if (!nuevo.isBlank()) salida.add(anterior.isBlank() ? "Estado: " + nuevo : "De " + anterior + " a " + nuevo);
		if (!datos.getOrDefault("MOTIVO", "").isBlank()) salida.add("Motivo: " + datos.get("MOTIVO"));
		return salida.isEmpty() ? limpiar(original) : recortar(String.join(" · ", salida));
	};

	private static String estadoLegible(String estado) {
		if (estado == null || estado.isBlank()) return "";
		return capitalizar(estado.replace('_', ' ').toLowerCase(Locale.ROOT));
	}

	private static String etiquetar(String etiqueta, String valor) {
		String limpio = valor == null ? "" : valor.replaceAll("\\s+", " ").trim();
		if (limpio.isEmpty()) return etiqueta == null ? SIN_DETALLE : etiqueta;
		return etiqueta == null ? limpio : etiqueta + ": " + limpio;
	}

	private static String limpiar(String texto) {
		String limpio = texto == null ? "" : texto.replaceAll("\\s*\\|\\s*", " · ").replaceAll("\\s+", " ").trim();
		return limpio.isEmpty() ? SIN_DETALLE : recortar(capitalizar(limpio));
	}

	private static String recortar(String texto) {
		return texto.length() <= LARGO_DETALLE ? texto : texto.substring(0, LARGO_DETALLE) + "…";
	}

	private static String capitalizar(String texto) {
		if (texto == null || texto.isEmpty()) return "";
		return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
	}

	static String sinTildes(String texto) {
		if (texto == null) return "";
		return Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
	}

	// Debe declararse después de los formateadores estáticos que referencia.
	private static final List<Regla> REGLAS = construirReglas();
}
