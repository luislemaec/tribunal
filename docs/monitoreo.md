# Monitoreo de usuarios en línea

`GET /monitoreo/en-linea` devuelve cuántos usuarios están en línea y por qué canal, **sin
iniciar sesión en TEC**. Solo totales, sin nombres ni datos personales:

```json
{"web": 5, "app": 3, "usuarios": 7, "actualizado": "2026-10-15T18:30:00Z"}
```

| Campo | Significado |
|---|---|
| `web` | Usuarios con sesión web autenticada vigente (mismo dato que Rep. Registros → Actividad) |
| `app` | Usuarios con sesión de la App vigente y usada en los últimos 15 minutos |
| `usuarios` | Personas distintas: quien usa web y App a la vez cuenta una sola vez |
| `actualizado` | Momento de la consulta (UTC) |

Implementación: `MonitoreoEnLineaServlet` (no crea sesión HTTP ni usa la identidad Elytron).

## Activar

Está **desactivado por defecto**: sin clave responde `404`, como si no existiera. Se activa con
la propiedad del sistema `tec.monitoreo.clave` (mínimo 32 caracteres). Por ejemplo, desde la CLI de
WildFly, escribiendo la clave en lugar de `<clave>` (no queda en el código ni en el WAR):

```bash
jboss-cli.bat --connect --command="/system-property=tec.monitoreo.clave:add(value=<clave>)"
```

Para generar una clave aleatoria en PowerShell (Windows PowerShell 5.1 o PowerShell 7): 64 caracteres
hexadecimales, sin `+`, `/` ni `=` que compliquen la CLI de WildFly:

```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); -join ($b | ForEach-Object { $_.ToString('x2') })
```

Para desactivarlo: `/system-property=tec.monitoreo.clave:remove()`.

## Consultar

La clave va **solo en la cabecera** `X-Monitoreo-Clave`, nunca en la URL (quedaría en los logs):

```bash
curl -s -H "X-Monitoreo-Clave: <clave>" https://tribunal.conpociiech.org/monitoreo/en-linea
```

| Estado | Código | Caso |
|---|---|---|
| 200 | — | Totales |
| 404 | — | Monitoreo desactivado (sin clave o clave de menos de 32 caracteres) |
| 403 | `HTTPS_REQUERIDO` | Petición sin HTTPS |
| 401 | `CLAVE_INVALIDA` | Falta la cabecera o la clave no coincide (se registra la IP en el log) |
| 429 | `DEMASIADOS_INTENTOS` | Más de 10 peticiones por minuto desde la misma IP |

## Notas

- La clave se compara en tiempo constante. `LoginFilter` excluye solo la ruta exacta.
- `web` cuenta las sesiones de este nodo de WildFly (en clúster, cada nodo las suyas) y se reinicia
  con el servidor, igual que las sesiones.
- Opcional: en Nginx se puede restringir además por IP con un `location = /monitoreo/en-linea`
  que incluya `allow <IP del monitor>; deny all;` antes de `location /`.
