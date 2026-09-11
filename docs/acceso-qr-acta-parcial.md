# Acceso QR del Presidente de Mesa

Estado al 2026-09-11: implementacion integrada en codigo, deshabilitada por defecto.
No se aplicaron migraciones ni configuracion al servidor. Falta aceptacion integrada.

## Flujo implementado

- Acta Parcial: URL HTTPS /acceso-acta?token=TOKEN, 32 bytes SecureRandom, Base64 URL canonico.
  BD almacena SHA-256 hexadecimal. El PDF contiene la credencial: controlar copias y respaldos.
- GET no consume: redirige la consulta estricta a un fragmento temporal; JavaScript lo elimina del historial. El canje es POST automatico con CSRF, sin boton ni confirmacion humana. Los QR anteriores con fragmento siguen siendo compatibles.
- Un lector con vista previa que ejecute JavaScript puede consumir el token: la navegacion automatica conserva uso unico y no autoriza reutilizacion. JavaScript es obligatorio; token invalido/BFCache no se reenvian.
- La emision exige tec.qr.enabled=true y tec.qr.public.base-url HTTPS valido. Si falta configuracion, QR o firmantes Presidente/Secretario, no se guarda un PDF sin QR. El generador rechaza documentos de mas de una pagina.
- Aplicar la proteccion de logs del proxy ANTES de habilitar: una URL con token en query puede quedar registrada por infraestructura externa/APM. No registrar URL completa, cabecera Location ni cuerpos del canje.
  No guardar tokens en localStorage, logs, mensajes ni cuerpos registrados por APM.
- Canje REQUIRES_NEW con bloqueo pesimista: documento/version/contexto activos y archivo presente,
  proceso activo, SUFRAGIO, mesa/recinto activos, JRV completa, Presidente y usuario/rol vigentes.
- Se crea prueba interna de sesion y secreto BCrypt efimero de 30 segundos, nunca enviado al cliente.
  No cambia la contrasena normal. request.login autentica ese secreto en TribunalQrRealm.
- Elytron conserva TECQR:username como principal; el post-realm transformer cambia solo el nombre
  consultado al realm. Se comprueban principal y roles en Servlet y EJB antes de cargar LoginBean.
- Confirmar borra el hash BCrypt efimero. Se renueva JSESSIONID. Sesion de 15 minutos de inactividad,
  restringida a actaE.jsf y recursos, sin permisos administrativos aunque el usuario normal los tenga.
- Cada peticion y comando electoral revalida concesion, mesa y proceso. El interceptor deniega
  comandos no autorizados, detalles ajenos, categorias repetidas y cierre sin conteo completo/cuadre.
- Regeneracion revoca QR/sesiones anteriores en la transaccion del documento nuevo.
  Rollback conserva el documento anterior y limpia el archivo nuevo.
- Auditoria: emision, canje, autenticacion, rechazos de token y cierre de sesion.
  Rechazos HTTP previos al canje no identifican un QR; revisar metricas del proxy sin registrar cuerpos.

La ventana es [inicio, fin) de SUFRAGIO intersectada con la vigencia original del token.
No se extiende automaticamente a ESCRUTINIO. Un token consumido no se recupera:
si se pierde la sesion, el Tribunal debe regenerar el acta y distribuir la nueva version.
El QR es una credencial al portador: una copia puede usarse primero por otra persona.
No constituye por si solo una prueba fisica de identidad.

## Componentes

- V4__credenciales_qr_acta_parcial.sql: credenciales, concesiones, auditoria e indices.
- V5__canje_qr_elytron.sql: campos BCrypt efimeros y vista tec.credencial_qr_elytron.
- security/qr: token, endpoint, filtro HTTP, cookies, rutas e interceptor electoral.
- AccesoQrActaFacade y servicios *AccesoQrService: persistencia, validacion y transacciones.
- AlcanceSesionQrService: identidad Elytron + concesion server-side + mesa/proceso.
- ReporteMesaService/ReportePFD: generacion y revocacion integradas.
- ActaEController: inicializacion QR sin cargas gerenciales y contexto unico.
- DocumentoService/AccesoDocumentoMesaService/DocumentoBean: control documental.
- docs/deployment/instalar-qr-elytron.cli: instalacion MANUAL, nunca desde el WAR.

## Activacion en pruebas

1. Respaldar BD y standalone.xml. Mantener tec.qr.enabled=false.
2. Aplicar V4/V5 con Flyway existente. Revisar el schema real de flyway_schema_history.
   No usar repair ni modificar migraciones aplicadas. Verificar permisos de tablas, secuencias
   y vista del datasource; no conceder permisos a PUBLIC.
3. Revisar TribunalSecurityDomain: default-realm TribunalRealm, sin realm-mapper ni
   post-realm-principal-transformer. No debe existir un role-mapper global que amplie roles QR.
   Reservar TECQR:; no deben existir usernames normales con ese prefijo.
4. Ejecutar instalar-qr-elytron.cli con jboss-cli --connect --file=...
   Linux: $JBOSS_HOME/bin/jboss-cli.sh. Windows: %JBOSS_HOME%/bin/jboss-cli.bat.
   El script conserva realms actuales con list-add y no toca TribunalRealm/FORM/datasource.
   Falla si recursos ya existen. Revisar resultados; no repetir ni sobrescribir mappers.
5. Realizar reload controlado y probar primero login FORM/BCrypt y roles normales.
6. Aplicar deployment/qr-proxy.md y revisar tribunal.nginx.conf.
   Bloquear acceso externo a 8080 antes de confiar en forwarded headers.
   Verificar request.isSecure(), IP real, host y puerto 443 via proxy HTTPS.
7. Configurar antes del despliegue:

   -Dtec.qr.enabled=true
   -Dtec.qr.public.base-url=https://tribunal.conpociiech.org
   -Duser.timezone=America/Guayaquil

   Pruebas deben tener su propio origen HTTPS. Origen raiz sin path/query.
   WAR publicado como ROOT. rpm.files.path sigue independiente de Windows/Linux.
   Sincronizar reloj del servidor y BD.
8. Desplegar, revisar cookies Secure/HttpOnly/SameSite=Lax y rotacion JSESSIONID.
   El modo QR fuerza cookies seguras en toda la aplicacion: probar tambien FORM por HTTPS.
9. Regenerar una Acta Parcial con JRV completa y cronograma vigente.
   Los documentos anteriores no adquieren QR retroactivamente.

## Aceptacion integrada pendiente

| Caso | Resultado obligatorio |
| --- | --- |
| QR valido | Principal TECQR:usuario en Servlet/EJB, roles Presidente y TEC-QR |
| Login normal | Principal, roles y contrasena originales |
| Doble POST concurrente | Solo un canje; segundo rechazado y auditado |
| Expirado/fuera de SUFRAGIO | Sin autenticacion ni escrituras |
| Documento antiguo/regenerado | QR anterior y sesion rechazados |
| Presidente cambiado/usuario inactivo | Rechazo en siguiente peticion y escritura |
| Manipular token/mesa/proceso/detalle | Rechazo sin modificar datos ajenos |
| CSRF ausente/otro origen | HTTP 403 sin consumir token |
| Otra pantalla/alias JSF | Denegado y sesion invalidada |
| Conteo incompleto/descuadrado | Cierre rechazado en backend |
| Fallo PDF/persistencia | Rollback; documento/QR anterior conservado |
| Logout/inactividad | Concesion revocada; token no reutilizable |
| Revocacion concurrente | Verificar bloqueos y rollback en PostgreSQL |

Consultar auditoria sin exponer hashes:
SELECT qr_id, proce_id, mesa_id, estado, doc_version FROM tec.acceso_qr_acta;
SELECT qr_id, mesa_id, usu_id, ocurrido_en, resultado FROM tec.auditoria_acceso_qr;
SELECT table_schema FROM information_schema.tables WHERE table_name = 'flyway_schema_history';

## Pruebas locales

- mvn -DskipTests compile: correcto.
- mvn -Pqr-tests test: 22 pruebas, cero fallos. No sustituye pruebas JTA/PostgreSQL.
- mvn -Pqr-tests "-Dtest=*Test" test: 68 pruebas, cero fallos, incluyendo regresion documental/JRV.
- VerificarIdentidadQr.java con Elytron 2.8.2 de WildFly 39.0.1: principal QR y separacion
  de roles comprobados en un dominio en memoria. No verifica propagacion en un deployment.
- PdfAccesoQrCheck: cuerpo del PDF en una pagina con 2/5/8/9 listas.
  Ejecutar con classpath Maven mas el jar Jakarta Faces del servidor.
  Cabecera/pie requieren FacesContext; falta revision visual y escaneo movil en el deployment.
- No se ejecutaron migraciones, despliegue, concurrencia PostgreSQL ni pruebas de navegador.

El limitador en memoria es por nodo. Para mas nodos, usar limitacion compartida en proxy.
Definir retencion de auditoria y distribucion controlada de actas impresas.

## Desactivacion

Definir tec.qr.enabled=false y reiniciar/redeploy controlado: QR deja de autorizarse;
FORM continua. No borrar tablas ni documentos. Retirar realm solo restaurando configuracion
respaldada; no asumir valores anteriores.

Referencia: https://docs.wildfly.org/39/WildFly_Elytron_Security.html
