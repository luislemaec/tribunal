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

## Módulo Tribunal (solo lectura, Bearer)

`ConsultaMovilService` autoriza cada método con `@RolesAllowed` y toma el alcance del
principal Elytron, nunca de la App: la mesa del Presidente sale de su designación JRV como
PRESIDENTE y la iglesia del IglesiaAdmin de su usuario. Todo se filtra por el proceso activo.
Los roles sin funciones en la V1 (Tecnico, Gerencial, Supervisor, Superadministrador) reciben
403 `SIN_PERMISO`. Un usuario con varios roles puede consultar la unión de sus endpoints.

| Endpoint | Roles | Respuesta |
|---|---|---|
| `GET /tribunal/presidente/mesa` | Presidente-mesa | Proceso, ubicación, estado del escrutinio, electores, junta; `resultados` solo con la mesa `CERRADO`. 404 `SIN_MESA_ASIGNADA` |
| `GET /tribunal/presidente/padron` | Presidente-mesa | `[{nombre, iglesia, sufrago}]` de su mesa. 404 `SIN_MESA_ASIGNADA` |
| `GET /tribunal/iglesia` | IglesiaAdmin | Datos de su iglesia y resumen de miembros. 404 `SIN_IGLESIA_ASIGNADA` |
| `GET /tribunal/iglesia/miembros?busqueda=&habilitado=&pagina=0&tamano=30` | IglesiaAdmin | `{total, pagina, tamano, elementos:[{nombre, habilitado, revisado}]}` (máx. 100 por página) |
| `GET /tribunal/proceso/resumen` | Administrador, Tribunal | Totales del dashboard web. 404 `SIN_PROCESO_ACTIVO` |
| `GET /tribunal/proceso/mesas` | Administrador, Tribunal | `[{ubicacion:{mesaId, mesa, recinto, parroquia, canton}, estado, juntaRegistrada}]` |
| `GET /tribunal/proceso/resultados` | Administrador, Tribunal | Consolidado de mesas cerradas (misma caché que la página de resultados) |

Las respuestas no incluyen cédulas, correos ni datos de contacto.

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
