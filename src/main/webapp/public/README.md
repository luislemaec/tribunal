# Portal público de resultados

Esta carpeta contiene los archivos necesarios para publicar la página pública de resultados sin depender de JSF ni de recursos fuera de `public`.

## Fuente única

**Esta carpeta (`tec/src/main/webapp/public/`) es la fuente única del portal.** El proyecto del subdominio
(`resultados.conpociiech.org/public/`) es una copia: todo cambio se hace aquí y luego se copia allá.
No edite directamente la copia del subdominio.

Sincronización (desde `workspace_tec`):

```bash
cp -r tec/src/main/webapp/public/. resultados.conpociiech.org/public/
```

## Archivos

- `resultados.html`: página pública.
- `resultados.json`: endpoint relativo esperado. En WildFly lo atiende el servlet `/public/resultados.json` (`ResultadoPublicoJsonServlet`).
- `assets/css/resultados.css`: estilos propios.
- `assets/js/resultados.js`: lógica de la página.
- `assets/img/logo-tribunal.png`: logo horizontal del Tribunal (copia de `resources/ecuador-layout/images/ecuador-orange-logo.png`).
- `assets/img/favicon.ico`: icono local.

## Qué se publica

Solo mesas **cerradas con acta física VALIDADA** (datos oficiales) del proceso electoral activo. Si el
Tribunal revierte una validación, la mesa deja de publicarse en la siguiente actualización de la caché
(cada 30 segundos).

### Cuándo se publica

Los resultados se publican **desde el fin de la fase SUFRAGIO** del cronograma del proceso activo
(por ejemplo, votación el 13/12/2026 de 08:00 a 16:00: se publican desde las 16:00).

- Lo decide el servidor en cada consulta: antes de esa hora, `resultados.json` no incluye votos ni mesas
  (`results` y `tables` vacíos, resumen en cero). No basta con ocultarlo en la página.
- Mientras tanto, la página muestra una cuenta regresiva (días, horas, minutos y segundos) sincronizada
  con la hora del servidor (cabecera `Date`). Mientras el contador no llega a cero la página no consulta
  al servidor ni actualiza nada; al llegar a cero carga los resultados sola.
- Si el proceso no tiene fase SUFRAGIO activa con fecha de fin, **no se publica** y la página indica que
  la fecha de votación aún no está programada.

Contrato de `resultados.json`:

- `generatedAt`: hora en que se generaron los datos.
- `hasActiveProcess`, `process.id`, `process.name`.
- `publication`: `available` (si ya se pueden mostrar resultados), `votingStart` y `votingEnd` (fase SUFRAGIO).
- `summary`: `totalMesas` (mesas con padrón en el proceso), `mesasCerradas` (con acta validada),
  `mesasPendientes`, `totalVotosListas`, `validVotes` (suma de las listas), `blankVotes`, `nullVotes`,
  `totalVotes`, `porcentajeMesasCerradas`, `porcentajeMesasCerradasEntero`.
- `results[]`: solo listas (`tipo = LISTA`), en su orden oficial: `id`, `name` (categoría «LISTA N - Nombre»),
  `number`, `listName`, `slogan`, `order`, `votes`, `percentage` (sobre los votos válidos).
- `tables[]`: `id`, `province`, `canton`, `parroquia`, `recinto`, `mesa`, `sufragantes`,
  `votes` (votos emitidos: válidos, blancos y nulos; las papeletas no utilizadas no suman),
  `validVotes`, `blankVotes`, `nullVotes`, `closedAt` (cierre de la mesa), `validatedAt` (validación del acta física;
  es la fecha que muestra la tabla del portal), `listVotes` (votos de cada lista en la mesa: `{"id de results": votos}`).

Los votos en blanco y nulos son informativos: no pertenecen a ninguna lista ni entran en el porcentaje.

## Cómo presenta los datos el portal

- **¿Quién va ganando?**: la lista con más votos, su porcentaje y la ventaja sobre la segunda. Dice
  «Va ganando» con resultado parcial, «Lista más votada» con el 100 % de las mesas validadas y «Empate»
  si las primeras listas tienen los mismos votos. No usa la palabra «ganador»: la proclamación oficial
  corresponde al Tribunal. No publica nombres de candidatos.
- **Ver resultados de**: el filtro por cantón, parroquia y recinto recalcula el ganador, el ranking y los
  votos con las mesas validadas de esa zona (a partir de `listVotes`).
- Cada lista conserva siempre el mismo color, el de su orden oficial.

El `ETag` se calcula sobre los datos (sin `generatedAt`): mientras nada cambie, el servidor responde 304.

## Despliegue recomendado en subdominios

La página resuelve los resultados en este orden:

1. Meta tag `resultados-api-url` o `window.RESULTADOS_API_URL`, si existe.
2. `resultados.json` relativo al mismo origen.

Para `https://resultados.conpociiech.org`, la recomendación es publicar los archivos estáticos de `public` y exponer `https://resultados.conpociiech.org/resultados.json` con proxy hacia WildFly.

## Nginx recomendado

```nginx
location = /resultados.json {
    proxy_pass http://127.0.0.1:8080/public/resultados.json;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_cache_valid 200 15s;
}
```

Con este esquema el navegador siempre consulta `resultados.json` en el mismo subdominio y no depende de CORS.
