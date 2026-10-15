# API móvil de TEC — autenticación (v1)

Contrato entre la App CONPOCIIECH (Flutter) y TEC. Base: `https://tribunal.conpociiech.org/api/v1`
(pruebas: `https://tribunal.local/api/v1`). Solo HTTPS, JSON UTF-8.

TEC sigue siendo la fuente de verdad: la App no valida reglas de negocio y **cada petición se
autoriza en el servidor** con los mismos `@RolesAllowed` de la web.

## Diseño

| Aspecto | Decisión |
| --- | --- |
| Credenciales | Se validan contra el mismo realm de Elytron (`TribunalSecurityDomain`, BCrypt). La App nunca guarda la contraseña |
| Tokens | Opacos: 32 bytes aleatorios en Base64 URL. En BD solo su SHA-256 hexadecimal (`tec.sesion_movil`, migración `V7`) |
| Acceso | Válido 5 minutos desde su emisión. Cabecera `Authorization: Bearer <accessToken>` |
| Inactividad | 15 minutos sin peticiones cierran la sesión (igual que la web): ni acceso ni refresh son válidos |
| Refresh | Rotativo y de un solo uso. Reutilizar uno ya rotado revoca la sesión (token copiado) |
| Vida máxima | 12 horas desde el inicio de sesión; después se exige iniciar sesión de nuevo |
| Identidad | Por cada petición válida, TEC crea la identidad del usuario en Elytron (`ServerAuthenticationContext.authorize`) y ejecuta la petición con ella (`SecurityIdentity.runAs`): roles, principal y alcance son los mismos que en la web. No requiere cambios en `standalone.xml` |
| Revocación | Logout; cambio o restablecimiento de clave (revoca todas las sesiones del usuario); usuario desactivado (se valida en cada petición); los roles se leen del realm en cada petición |
| Intentos | Login limitado por IP (10/min) y por usuario (5/min); excedido → 429 |
| Auditoría | `tec.procesos` con el formato de la web (`LOGIN \| …`, `LOGOUT \| …`): aparece en la bitácora como «Inició sesión» / «Cerró sesión». Nunca se registran tokens ni claves |
| Sesión HTTP | La API no crea `HttpSession` ni cookies |

## Errores

Cuerpo común: `{"codigo": "...", "mensaje": "..."}`.

| HTTP | código | Cuándo |
| --- | --- | --- |
| 400 | `SOLICITUD_INVALIDA` | Faltan campos o el formato no es válido |
| 400 | `CLAVE_INCORRECTA` | Clave actual incorrecta al cambiarla |
| 400 | `CLAVE_REUTILIZADA` | La clave nueva es igual a la vigente |
| 400 | `CLAVE_NO_CUMPLE_POLITICA` | La clave nueva no cumple la política: 8 a 16 caracteres, sin espacios, con mayúscula, minúscula y número |
| 401 | `CREDENCIALES_INVALIDAS` | Usuario/clave incorrectos, usuario inactivo o sin rol TEC (mensaje genérico, como la web) |
| 401 | `SESION_INVALIDA` | Token ausente, desconocido, vencido, inactivo, revocado o refresh reutilizado. Respuesta con `WWW-Authenticate: Bearer` |
| 403 | `CAMBIO_CLAVE_OBLIGATORIO` | Sesión restringida usada fuera del cambio de clave |
| 403 | `SIN_PERMISO` | El rol del usuario no autoriza la operación |
| 403 | `HTTPS_REQUERIDO` | Petición recibida sin HTTPS |
| 404 | `RECURSO_NO_ENCONTRADO` | Ruta o método inexistente |
| 404 | `SIN_MESA_ASIGNADA` / `SIN_IGLESIA_ASIGNADA` / `SIN_PROCESO_ACTIVO` | El usuario no tiene el alcance que la consulta requiere |
| 404 | `MIEMBRO_NO_DISPONIBLE` | El miembro no existe o ya no está activo |
| 409 | `EDICION_CERRADA` | El cronograma no permite cambiar la habilitación de miembros |
| 409 | `REGLA_ESCRUTINIO` | Una regla del escrutinio impide el paso (mensaje de TEC) |
| 413 | `ARCHIVO_DEMASIADO_GRANDE` | La foto del acta física supera 10 MB |
| 429 | `DEMASIADOS_INTENTOS` | Límite de intentos de login |
| 500 | `ERROR_INTERNO` | Error no controlado (el detalle queda solo en el log del servidor) |

## Endpoints

### `POST /auth/login` (público)

```json
{ "usuario": "0600000000", "clave": "••••", "dispositivo": "Android 15 · Pixel 8" }
```

200:

```json
{
  "accessToken": "…", "refreshToken": "…", "tipo": "Bearer",
  "expiraEnSegundos": 300, "cambioClaveObligatorio": false,
  "usuario": { "usuario": "0600000000", "nombre": "APELLIDOS NOMBRES",
               "roles": ["SITEC-IglesiaAdmin"], "iglesiaId": 12 }
}
```

Con `cambioClaveObligatorio: true` (usuario no permanente) la sesión **solo** permite
`/auth/cambiar-clave`, `/auth/yo` y `/auth/logout` (lo demás → 403 `CAMBIO_CLAVE_OBLIGATORIO`).

### `POST /auth/refresh` (público)

`{ "refreshToken": "…" }` → 200 con un par nuevo (mismo formato que login). El refresh anterior
deja de servir; presentarlo otra vez revoca la sesión.

### `GET /auth/yo` (Bearer)

200: `{ "usuario", "nombre", "roles", "iglesiaId", "cambioClaveObligatorio" }`.

### `POST /auth/cambiar-clave` (Bearer, también en sesión restringida)

`{ "claveActual": "…", "claveNueva": "…" }` → 200 con un par nuevo de sesión completa.
Revoca todas las sesiones móviles anteriores del usuario.

### `POST /auth/logout` (Bearer)

204. Revoca la sesión del token presentado.

## Módulo Tribunal (Bearer)

`ConsultaMovilService` autoriza cada método con `@RolesAllowed` y toma el alcance del
principal Elytron, nunca de la App: la mesa del Presidente sale de su designación JRV como
PRESIDENTE y la iglesia del IglesiaAdmin de su usuario. Todo se filtra por el proceso activo.
Los roles sin funciones en la V1 (Tecnico, Gerencial, Supervisor, Superadministrador) reciben
403 `SIN_PERMISO`. Un usuario con varios roles puede consultar la unión de sus endpoints.

| Endpoint | Roles | Respuesta |
|---|---|---|
| `GET /tribunal/presidente/mesa` | Presidente-mesa | Proceso, ubicación, estado del escrutinio, electores, junta; `resultados` solo con la mesa `CERRADO`. 404 `SIN_MESA_ASIGNADA` |
| `GET /tribunal/presidente/padron` | Presidente-mesa | `[{nombre, iglesia, sufrago}]` de su mesa. 404 `SIN_MESA_ASIGNADA` |
| `GET /tribunal/iglesia` | IglesiaAdmin | Datos de su iglesia, resumen de miembros y `permiteEdicion` (el cronograma permite cambiar la habilitación). 404 `SIN_IGLESIA_ASIGNADA` |
| `GET /tribunal/iglesia/miembros?busqueda=&habilitado=&revisado=&pagina=0&tamano=30` | IglesiaAdmin | `revisado=false`: pendientes de revisión (misma regla que el resumen). `{total, pagina, tamano, elementos:[{id, nombre, sexo, habilitado, revisado}]}` (máx. 100 por página) |
| `PUT /tribunal/iglesia/miembros/{id}/habilitacion` | IglesiaAdmin | Cuerpo `{habilitado: true\|false}` → 200 con el miembro. Ver abajo |
| `GET /tribunal/iglesia/miembros/{id}` | IglesiaAdmin | Datos editables de la persona: `{id, nombre, nombres, documento, sexo, habilitado, revisado, permiteEdicion}` (solo este endpoint envía la cédula). Ver abajo |
| `PUT /tribunal/iglesia/miembros/{id}/persona` | IglesiaAdmin | Cuerpo `{nombres, documento, sexo, habilitado?}` → 200 con el mismo detalle. Ver abajo |
| `POST /tribunal/iglesia/miembros` | IglesiaAdmin | Cuerpo `{nombres, documento, sexo, habilitado?}` → 201 con el detalle del miembro creado en la iglesia del usuario. Ver abajo |
| `DELETE /tribunal/iglesia/miembros/{id}` | IglesiaAdmin | Baja lógica del miembro → 204. Ver abajo |
| `GET /tribunal/proceso/resumen` | Administrador, Tribunal | Totales y `alertas` [{texto, cantidad, severidad}] del dashboard web. 404 `SIN_PROCESO_ACTIVO` |
| `GET /tribunal/proceso/mesas` | Administrador, Tribunal | `[{ubicacion:{mesaId, mesa, recinto, parroquia, canton}, estado, juntaRegistrada, fase}]`. Mismas mesas que la tabla de actaE: con padrón en el proceso o con escrutinio iniciado |
| `GET /tribunal/proceso/resultados` | Administrador, Tribunal | Consolidado de mesas cerradas (misma caché que la página de resultados) |
| `GET /tribunal/proceso/mesas/{mesaId}` | Administrador, Tribunal | Detalle: junta, estado, conteo por categoría, totales, cuadre, acta física vigente, `puedeRevisarActa` y `fase`. 404 `MESA_NO_ENCONTRADA` |
| `GET /tribunal/proceso/mesas/{mesaId}/acta-fisica` | Administrador, Tribunal | Foto JPEG del acta física vigente (auditada). 404 `ACTA_NO_DISPONIBLE` |
| `POST /tribunal/proceso/mesas/{mesaId}/acta-fisica/validacion` | Tribunal | `{cotejoConfirmado: true}` → datos oficiales (VALIDADA). Ver abajo |
| `POST /tribunal/proceso/mesas/{mesaId}/acta-fisica/devolucion` | Tribunal | `{estado: OBSERVADA|RECHAZADA, motivo}` → la mesa vuelve a REABIERTO. Ver abajo |

Las respuestas no incluyen cédulas, correos ni datos de contacto.

### Habilitación de miembros (escritura)

`GestionMovilService.cambiarHabilitacionMiembro` (solo `SITEC-IglesiaAdmin`) delega en
`IglesiaPersonaService.cambiarHabilitacion`, con las mismas reglas que la edición web
(`guardarDesdeDTO`): el cronograma debe permitir la edición del padrón, el miembro debe estar
activo y pertenecer a la iglesia del usuario, y el cambio lo marca como **revisado**
(`fechaActualiza`, cuenta para el avance y el acta de actualización). Envers registra el
cambio con el usuario autenticado. No modifica los datos de la persona.

| Estado | Código | Caso |
|---|---|---|
| 409 | `EDICION_CERRADA` | La fase vigente del cronograma no permite editar el padrón |
| 403 | `SIN_PERMISO` | El miembro es de otra iglesia, o el rol no es IglesiaAdmin |
| 404 | `MIEMBRO_NO_DISPONIBLE` | No existe o ya no está activo |
| 400 | `SOLICITUD_INVALIDA` | Falta `habilitado` |

### Edición de la persona de un miembro (escritura)

**«Datos de la persona» + «Habilitar para padrón».** `habilitado` es opcional (null = sin cambio). Cada cambio
usa el servicio que ya lo gestiona: si cambian datos de la persona (con o sin habilitación),
`guardarDesdeDTO` en una transacción, igual que el botón Guardar de la web; si solo cambia la
habilitación, `cambiarHabilitacion` (solo `tb_iglesia_persona`; Persona no se audita); sin cambios,
nada. Desde la App, la lista de miembros es de solo consulta: este es el único punto de edición.

`GestionMovilService.actualizarPersonaMiembro` (solo `SITEC-IglesiaAdmin`) cambia los mismos campos
que el formulario web de Personas: «Apellidos y Nombres» (`nombres`), cédula (`documento`) y sexo
(`M`, `F` o vacío). Parte del vínculo actual (alcance validado por
`IglesiaPersonaService.obtenerMiembroParaEdicion`) y conserva iglesia, fechas, apellidos,
tratamiento y habilitación; otros campos del cuerpo se ignoran. Formatos con `ValidadorPersona`
(compartido con la web) y reglas con `guardarDesdeDTO`: cronograma, alcance, cédula no usada por
otra persona y una sola iglesia activa. La cédula solo se valida si cambia (se conservan documentos
históricos especiales). **Sin cambios no se guarda nada.** Con cambios, Envers registra `Persona`
y `IglesiaPersona` (queda revisado, igual que en la web) con el usuario autenticado, y la bitácora
`tec.procesos` agrega la actividad.

| Estado | Código | Caso |
|---|---|---|
| 400 | `DATOS_INVALIDOS` | Nombres vacíos o de más de 100 caracteres, cédula inválida o sexo distinto de M/F |
| 409 | `DOCUMENTO_DUPLICADO` | La cédula pertenece a otra persona |
| 409 | `CONFLICTO_IGLESIA` | La persona tiene otra relación activa que impide guardar |
| 409 | `EDICION_CERRADA` | La fase vigente del cronograma no permite editar el padrón |
| 403 | `SIN_PERMISO` | El miembro es de otra iglesia, o el rol no es IglesiaAdmin |
| 404 | `MIEMBRO_NO_DISPONIBLE` | No existe o ya no está activo |

### Agregar y dar de baja miembros (escritura)

Replican «Nuevo» y la papelera de Personas en la web, con los mismos servicios (solo
`SITEC-IglesiaAdmin`; Administrador y Tribunal siguen en la web).

**Agregar** (`GestionMovilService.crearMiembro`): la iglesia es siempre la del usuario. Formatos con
`ValidadorPersona` y reglas con `IglesiaPersonaService.guardarDesdeDTO` en una transacción:
cronograma, alcance, cédula no usada por otra persona activa y una sola iglesia activa. Si la cédula
es de una persona activa sin iglesia se reutiliza (como `buscaPersonaPorCedula` en la web): se
actualizan nombres y sexo, y se conservan apellidos y tratamiento. Una persona dada de baja no se
reutiliza; la restauran Administrador o Tribunal desde «Ver eliminadas». Sin `habilitado`, queda
habilitada (igual que la web) y marcada como revisada. La App no consulta personas por cédula: TEC
responde al guardar.

**Dar de baja** (`GestionMovilService.darDeBajaMiembro`): `IglesiaPersonaService.eliminarPorId`
pone `estado=false` en el vínculo iglesia-persona y, si la persona no tiene otro vínculo activo,
también en la persona. **No se borra ninguna fila.** Padrón y JRV conservan su historial (apuntan al
vínculo); las consultas del padrón filtran vínculos activos, así que el miembro deja de aparecer,
igual que en la web. A diferencia de la web (que solo deshabilita el botón), aquí el cronograma se
comprueba en el servidor.

Envers registra `Persona` e `IglesiaPersona` con el usuario autenticado y la bitácora `tec.procesos`
anota `CREA` / `DESACTIVA`.

| Estado | Código | Caso |
|---|---|---|
| 400 | `DATOS_INVALIDOS` | Nombres vacíos o de más de 100 caracteres, cédula inválida o sexo distinto de M/F |
| 409 | `DOCUMENTO_DUPLICADO` | La cédula pertenece a otra persona activa |
| 409 | `CONFLICTO_IGLESIA` | Ya es miembro de esta iglesia, pertenece a otra iglesia o tiene varias iglesias activas |
| 409 | `EDICION_CERRADA` | La fase vigente del cronograma no permite editar el padrón |
| 403 | `SIN_PERMISO` | El miembro es de otra iglesia, o el rol no es IglesiaAdmin |
| 404 | `MIEMBRO_NO_DISPONIBLE` | El miembro no existe o ya fue dado de baja |

### Revisión del acta física (escritura)

`GestionMovilService.validarActaFisica` / `devolverActaFisica` (**solo Tribunal**; el Administrador
consulta el detalle y la foto, y en `puedeRevisarActa` recibe `false`)
reutilizan los servicios de actaE: solo una mesa **CERRADA** con acta física vigente aún no
validada. Validar exige que el revisor confirme el cotejo (`cotejoConfirmado: true`) y llama a
`ActaFisicaEscrutinioService.validarDatosRegistrados` (dato oficial, VALIDADA). Devolver exige
estado OBSERVADA o RECHAZADA y motivo, llama a `devolverActaFisica` y la mesa vuelve a
REABIERTO para que el Presidente corrija; se le notifica si las notificaciones están activas.
La bitácora registra `VALIDA ACTA FISICA SIN CAMBIOS`, `DEVUELVE ACTA FISICA …` y
`DESCARGA ACTA FISICA` con «(App móvil)». Errores: 409 `REGLA_REVISION`, 400 `COTEJO_REQUERIDO`.

### Escrutinio del Presidente (escritura)

`EscrutinioRecurso` → `GestionMovilService` (solo `SITEC-Presidente-mesa`). Solo en línea.
La mesa sale de la designación JRV como PRESIDENTE en el proceso activo
(`AccesoDocumentoMesaService.mesaPermitida`), nunca de la App. Las transiciones las valida
`EscrutinioService`, igual que en `actaE`; el servicio agrega lo que en la web comprueba
`ActaEController`: mesa propia, acta con **todas** sus categorías (ni faltantes ni ajenas) y
votos entre 0 y 100 000. Cada paso se registra en la bitácora (`APERTURA MESA`,
`GUARDA BORRADOR ACTA MESA`, `CIERRE MESA`, `CARGA ACTA FISICA MESA`, con «(App móvil)»).

| Endpoint | Cuerpo | Efecto |
|---|---|---|
| `GET /tribunal/presidente/acta` | — | Estado, sufragantes, categorías con votos, cuadre, `motivoBloqueoApertura`, `puedeAbrir/puedeEditarConteo/puedeCerrar/puedeSubirActaFisica`, acta física vigente y `fase` |
| `POST /tribunal/presidente/acta/apertura` | `{observacion?}` | PENDIENTE → ABIERTO (TEC valida junta, padrón y documentos) |
| `PUT /tribunal/presidente/acta/conteo` | `{votos:[{categoriaId, votos}]}` | Borrador: CONTEO_REGISTRADO si el cuadre es 0, si no EN_CONTEO |
| `POST /tribunal/presidente/acta/cierre` | `{votos:[…]}` | → CERRADO. Exige conteo guardado **y** acta física vigente no devuelta; luego no se modifica |
| `PUT /tribunal/presidente/acta/acta-fisica` | JPEG (`Content-Type: image/jpeg`, máx. 10 MB) | Foto del acta firmada → nueva versión PENDIENTE_REVISION. Con el conteo guardado (EN_CONTEO, CONTEO_REGISTRADO, REABIERTO) o en una mesa CERRADA sin acta validada (flujo anterior) |

Todas responden 200 con el acta actualizada. Errores: 409 `REGLA_ESCRUTINIO` (regla de
negocio, con el mensaje de TEC), 400 `SOLICITUD_INVALIDA` (categoría repetida o sin votos),
413 `ARCHIVO_DEMASIADO_GRANDE`, 403 `SIN_PERMISO`. El proxy permite 11 MB solo en el
endpoint del acta física (`tribunal.nginx.conf`).

### Orden oficial y fase del escrutinio

**Apertura → Conteo → Acta física → Cierre → Validación.** Las reglas están en
`ReglasFlujoEscrutinio` y las comparten TEC WEB (`actaE`) y la App:

- La carga del acta física se habilita con el conteo guardado; el conteo sigue editable
  (el Tribunal contrasta al validar).
- `EscrutinioService.guardarActaCompleta` rechaza el cierre sin un acta física vigente
  PENDIENTE_REVISION con archivo disponible (`escrutinio.cierre.actaFisica.requerida`).
  Si el Tribunal la devolvió (OBSERVADA/RECHAZADA) se exige una versión nueva.
- Una mesa CERRADA sin acta (flujo anterior) todavía puede cargarla.

`fase` es la misma proyección del avance de `actaE` (`ProgresoEscrutinioDTO`); no se guarda en la base
y no agrega estados. La App la muestra tal cual y no la reconstruye:

```json
{"fase": "ACTA_FISICA", "numero": 3, "etiqueta": "Acta física",
 "situacion": "Pendiente de carga", "incidencia": null, "incidenciaEtiqueta": null,
 "severidad": "info", "oficial": false}
```

| Campo | Valores |
|---|---|
| `fase` | `APERTURA`, `CONTEO`, `ACTA_FISICA`, `CIERRE`, `DATO_OFICIAL` |
| `numero` | 1–5 (orden oficial) |
| `incidencia` | `OBSERVADO`, `ANULADO`, `REABIERTO`, `OBSERVADA`, `RECHAZADA` o null |
| `severidad` | `success`, `info`, `warning`, `danger` (la misma de TEC WEB; la App la traduce a sus tokens semánticos) |

Observado, Anulado y Reabierto (estado del escrutinio) y Observada/Rechazada (revisión del
acta) **no son fases**: son la `incidencia` de la fase en que está la mesa. Validada es el
resultado de la validación (`fase=DATO_OFICIAL`, `oficial=true`). Una mesa que no inició está
en `APERTURA` · Pendiente.

«Avance de mesas» de la App replica la columna Situación de actaE: filtro exacto por `fase`
(Todas + las cinco fases) con el número de mesas de cada una, contado en el dispositivo sobre
esta misma respuesta (sin consultas adicionales), y una etiqueta por mesa con la fase, la
situación y la incidencia.

## Notificaciones push

Desactivadas por defecto. Con `-Dtec.push.enabled=true` TEC encola avisos en
`tec.notificacion_movil` **dentro de la transacción del evento** (si se revierte, no hay
aviso) y `EnvioNotificacionesProgramado` los envía cada minuto por Firebase Cloud Messaging
(API HTTP v1), con hasta 5 intentos. Un fallo de Firebase nunca afecta a la operación
electoral. Contenido mínimo, sin nombres ni datos de la mesa: el detalle se ve al abrir la App.

| Evento | Destinatarios | Origen |
|---|---|---|
| Acta física validada, observada o rechazada | Presidente de esa mesa | `EscrutinioService.validarResultadosFinalesDTO`, `ActaFisicaEscrutinioService.revisar` |
| Mesa cerrada | Tribunal y Administrador | `EscrutinioService.guardarActaCompleta` |
| Inicio de una fase del cronograma | IglesiaAdmin | Proceso programado (cronograma del proceso activo) |
| Inicio y fin del sufragio (fase SUFRAGIO) | Presidentes de mesa | Proceso programado |

Los eventos del cronograma se avisan una sola vez (`tec.notificacion_evento`) y solo si
ocurrieron hace menos de una hora: tras un apagado largo no se envían avisos viejos.

| Endpoint | Cuerpo | Efecto |
|---|---|---|
| `POST /dispositivos` | `{token, plataforma: "android"\|"ios"}` | 204. Registra o reasigna el token de FCM al usuario autenticado |
| `POST /dispositivos/baja` | `{token}` | 204. Desactiva el token si es del usuario (al cerrar sesión) |

Un token que FCM declara inválido (`UNREGISTERED`) se desactiva solo; cualquier otro error
se reintenta, para que un fallo del mensaje no desactive todos los dispositivos.

### Activar en un servidor

1. Aplicar `V12__notificaciones_push.sql`.
2. En Firebase (consola, cuenta institucional): crear el proyecto CONPOCIIECH, registrar las
   apps Android (`ec.org.conpociiech.app`) e iOS, y para iOS subir la clave APNs.
3. En Configuración del proyecto → Cuentas de servicio: generar una clave privada (JSON).
   **Es un secreto**: guardarla fuera del repositorio y del WAR, p. ej.
   `/etc/tec/firebase-push.json`, con permisos de lectura solo para el usuario de WildFly.
4. Propiedades de WildFly: `-Dtec.push.enabled=true -Dtec.push.credenciales=/etc/tec/firebase-push.json`.
5. Compilar la App con las opciones de Firebase (ver el README de la App).

## Despliegue

1. Aplicar `V7__sesion_movil_api.sql` con el procedimiento de Flyway (`docs/flyway.md`).
2. El proxy (`tribunal.nginx.conf`) debe publicar `/api/` por HTTPS y **no registrar** la
   cabecera `Authorization` ni los cuerpos de `/api/v1/auth/*`.
   La plantilla ya incluye `location /api/` con `no-store`, `limit_req` para login/refresh y
   cuerpo máximo de 16 KB.
3. Cambiar la clave (web, App o enlace de recuperación) revoca todas las sesiones móviles del
   usuario (`UsuarioService`).
4. Probar antes de usar la App (en Git Bash; `-k` solo si el certificado local no es de confianza):

```bash
# Sin token -> 401 SESION_INVALIDA
curl -ik https://tribunal.local/api/v1/auth/yo
# Login (escribir la clave cuando se pida; no queda en el historial)
read -rs -p "Clave: " CLAVE; echo
curl -sk https://tribunal.local/api/v1/auth/login -H "Content-Type: application/json" \
  -d "{\"usuario\":\"USUARIO\",\"clave\":\"$CLAVE\",\"dispositivo\":\"curl\"}" > /tmp/sesion.json; unset CLAVE
ACCESO=$(sed -E 's/.*"accessToken":"([^"]+)".*/\1/' /tmp/sesion.json)
REFRESH=$(sed -E 's/.*"refreshToken":"([^"]+)".*/\1/' /tmp/sesion.json)
curl -sk https://tribunal.local/api/v1/auth/yo -H "Authorization: Bearer $ACCESO"
curl -sk https://tribunal.local/api/v1/auth/refresh -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"
# Reusar el mismo refresh otra vez -> 401 y la sesión queda revocada
curl -sk https://tribunal.local/api/v1/auth/refresh -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"
rm -f /tmp/sesion.json
```
