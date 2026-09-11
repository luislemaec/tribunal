# Diagnostico de 403 en acceso QR

No consumir tokens por GET. El canje automatico usa POST/CSRF; una vista previa
que ejecute JavaScript si puede consumirlo. No copiar tokens a logs.

## Flujo anterior al INSERT

1. Nginx puede responder antes de WildFly. La plantilla convierte query a fragmento.
2. LoginFilter -> ControlHttpSesionQr: una sesion QR previa se revalida y puede
   rechazar antes del servlet. Codigo FILTRO_SESION_PREVIA.
3. AccesoActaServlet.doGet: QR_DESHABILITADO, HTTPS_NO_RECONOCIDO,
   ORIGEN_CONFIGURADO_INVALIDO, HOST_PUERTO_NO_AUTORIZADO o GET_RUTA producen 403.
   GET_LIMITE_INTENTOS produce 429; GET_FORMATO_TOKEN produce 400.
   No consulta qr_id ni requiere un rol autenticado en este punto.
4. GET prepara formulario oculto y CSRF; no inserta sesion_qr_acta ni marca CANJEADO.
5. POST /acceso-acta/canjear exige transporte, ruta exacta, ausencia de query,
   Origin HTTPS esperado, sesion previa, CSRF y marcador de envio del formulario.
   JavaScript envia automaticamente; ese marcador no representa consentimiento humano.
6. CanjeAccesoQrService.canjear bloquea por hash y llama ValidacionAccesoQrService.
   Este valida vigencia, estado, documento unico activo y archivo disponible,
   version/contexto/tipo, proceso activo, mesa/recinto activos, Presidente unico
   vigente, persona y relacion activas, usuario unico y no expirado, rol y JRV
   COMPLETA (no basta con tener un Presidente), y fase SUFRAGIO vigente.
7. Genera puente BCrypt efimero; invalida puentes anteriores del usuario; consume
   QR; inserta sesion; audita. La transaccion confirma todo conjuntamente.
8. Luego renueva sesion HTTP, autentica mediante Elytron y confirma el puente.

No hay contador maximo de usos configurable: EMITIDO permite un canje;
CANJEADO impide otro. El limitador es por IP: 60 GET y 10 POST por minuto,
con capacidad acotada; responde 429, no 403. El proxy tiene su propio limite.

## Interpretacion

- Al rechazar una sesion QR, ControlHttpSesionQr cierra identidad/sesion y
  redirige a /login.jsf, sin continuar FilterChain. LoginFilter usa getSession(false)
  y nunca forward para login/permisos: HTTP 303 para navegacion normal y respuesta
  parcial JSF redirect para AJAX. Un POST antiguo de login sin sesion tambien
  redirige a GET. Si logout del contenedor falla, se responde 403 sin redirigir
  para no crear un bucle con una identidad QR que no pudo retirarse.

- El monitor de navegacion convierte segundos a milisegundos con *1000 y solo
  cierra por idle, nunca por active. Antes usaba *100: una sesion de 900 segundos
  se cerraba a los 90 segundos. Sesion causa=IDLE_MONITOR identifica ese camino;
  SESION_HTTP_DESTRUIDA confirma la destruccion, pero no indica por si sola su causa.
- HHH000444 informa follow-on locking; no es por si mismo una expiracion de sesion.

- POST_ORIGIN: la pagina limpia de canje automatico debe usar Referrer-Policy:
  same-origin. No imponer no-referrer desde Nginx sobre esa pagina: los POST
  nativos pueden enviar Origin: null con esa politica. Redirecciones con token
  y errores conservan no-referrer. Nunca aceptar Origin null como solucion.

- Un GET 403 con sesiones vacias no demuestra un problema en el registro QR.
- CANJE_EJB + EJBAccessException identifica autorizacion del contenedor; no
  sustituirla con PermitAll masivo. Revisar el metodo exacto informado por WildFly.
- BUSCAR_Y_BLOQUEAR_TOKEN / VALIDACION_ELECTORAL / HASH_PUENTE /
  INVALIDAR_PUENTES / CONSUMIR_QR / INSERTAR_SESION / AUDITAR_CANJE permiten
  localizar excepciones antes del commit. Solo se registran clases de excepcion,
  nunca mensajes de proveedor, tokens, hashes, URL o contrasenas.
- Un rollback deja la tabla vacia incluso si se intento INSERT; no asumir que
  necesariamente fallo antes de ejecutar esa sentencia.
- LOGIN_ELYTRON o CONFIRMAR_PUENTE ocurre despues del canje, no durante GET.

Para qr_id=3 se necesita el nuevo codigo `QR causa=...` del servidor desplegado,
el metodo HTTP y la respuesta. No enviar token, query completa ni cookies.
El standalone.xml local no demuestra la configuracion efectiva de produccion:
propiedades JVM/CLI pueden sobrescribirlo. Consultar valores efectivos y el
listener de Undertow; nunca confiar directamente en cabeceras aportadas por el
cliente ni desactivar la validacion HTTPS para resolver un proxy mal configurado.

Validacion integrada pendiente: GET prepara canje sin consumir; POST automatico
crea sesion y CANJEADO; Elytron autentica; segundo canje se rechaza.
