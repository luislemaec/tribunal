# Estándar tipográfico Tribunal (Montserrat)

Fuente única de verdad: `src/main/webapp/resources/demo/css/tribunal-globals.css` (tokens `--tec-fs-*`,
`--tec-fw-*`, `--tec-lh-*`, `--tec-ls-caps`). Fuentes declaradas en `ecuador-layout/css/layout-tribunal.css`.

## 1. Unidades y base

- `html { font-size: 100% }`: 1rem = 16px por defecto. Respeta el tamaño de letra que el usuario configura en su navegador (WCAG 1.4.4).
- Todo tamaño de texto, espaciado y componente se expresa en **rem**. No se usan px para texto.
- Selector A-/A+ (`tribunal-fontsize.js`): 91,67 %, **100 %** (estándar), 116,67 % y 133,33 % de la base del navegador.

## 2. Breakpoints (PrimeFlex)

| Nombre | Rango | Media query |
|---|---|---|
| xs | < 576px | `max-width: 575.98px` |
| sm | ≥ 576px | `min-width: 576px` / `max-width: 767.98px` |
| md | ≥ 768px | `min-width: 768px` / `max-width: 991.98px` |
| lg | ≥ 992px | `min-width: 992px` / `max-width: 1199.98px` |
| xl | ≥ 1200px | `min-width: 1200px` |

- **Excepción documentada:** `max-width: 640px` / `min-width: 641px` (11 reglas). Coincide con el modo tarjetas (reflow) de `p:dataTable`, cuyo corte está fijado en el CSS de PrimeFaces.
- **Menú del layout:** pasa de modo móvil a escritorio en 992px (lg), tanto en `layout-tribunal.css` como en `layout.js`.

## 3. Escala

| Token | px (base 16) | rem | Uso |
|---|---|---|---|
| `--tec-fs-3xs` | 10 | .625 | Excepción: microtexto no esencial |
| `--tec-fs-2xs` | 11 | .6875 | Tags, hints, encabezados de tabla (mayúsculas) |
| `--tec-fs-xs` | 12 | .75 | Metadatos y labels (color secundario) |
| `--tec-fs-md` | 13 | .8125 | **Base**: texto, inputs, botones, datos principales |
| `--tec-fs-lg` | 15 | .9375 | Títulos de diálogo y panel |
| `--tec-fs-xl` | 17 | 1.0625 | Títulos superiores, cifras destacadas |
| `--tec-fs-2xl` | 21 | 1.3125 | Títulos de página |

PrimeFlex: `.text-xs` = 2xs, `.text-sm` = xs, `.text-base` = md, `.text-lg` = lg, `.text-xl` = xl, `.text-2xl`/`.text-3xl` = 2xl.

## 4. Pesos (únicos archivos cargados)

| Peso | Archivo | Uso |
|---|---|---|
| 300 Light | montserrat-v12 …-300 (woff2/woff) | **Solo** `.tec-display-light`: títulos ≥ 24px y desde md. En xs y sm muta a Regular. Hoy ningún texto llega a 24px |
| 400 Regular | montserrat-v12 …-regular (woff2/woff) | Todo el texto, incluidos metadatos y textos pequeños |
| 500 Medium | montserrat-latin-ext_latin-500 (woff2/woff, subconjunto latin + latin-ext generado desde resources/fonts/Montserrat-Medium.ttf) | Énfasis: títulos, encabezados, `font-medium`, `font-semibold` |
| 700 Bold | montserrat-v12 …-700 (woff2/woff) | `font-bold` y énfasis fuerte puntual |

- **Declaraciones:** sin `local()`, para que siempre se use la misma versión (7.200). Se precargan Regular y Medium (woff2).
- **Jerarquía en tablas:** el dato principal va en md (13px) y los metadatos en xs (12px) con color secundario, ambos en Regular.

## 4b. Clases de función en celdas y listas

- `.tec-dato`: dato principal (md 13px, Regular). El énfasis se añade con `font-medium` (500) o `font-bold` (700).
- `.tec-meta`: metadato (xs 12px, Regular, color secundario; un color `text-NNN` lo sobrescribe).
- No usar `text-sm` ni `text-xs` en celdas de tabla. Las reglas de compensación de `tribunal-globals.css` quedan solo como respaldo para texto antiguo.

## 5. Interlineado y mayúsculas

- `--tec-lh-title` 1.2 para títulos (`h1`–`h6`).
- `--tec-lh-tight` 1.3 para texto compacto de interfaz.
- `--tec-lh-base` 1.5 para párrafos (`p`), diálogos y avisos.
- `--tec-ls-caps` .04em para todo texto con `text-transform: uppercase` y para la utilidad `uppercase` de PrimeFlex.

## 6. Migración del 2026-10-01

- **Raíz de 12px a 16px:**
  - todos los rem del tema, el layout, PrimeFlex (copia local), los CSS de TEC y los `style` de las vistas se multiplicaron por 0,75, así que conservan su tamaño en pantalla;
  - los `font-size` pasaron a (12·x + 1)/16, es decir, la escala sube 1px;
  - script: `docs/herramientas/rem16.pl`, que conserva los nombres de clase como `.w-1rem`.
- **Pesos:** 100 y 300 pasan a 400, 600 a 500, y 800 y 900 a 700 fuera de `@font-face`; `--tec-fw-light` pasa a Regular en el CSS de TEC.
- **Excepciones:**
  - `components.css` de PrimeFaces (dentro del JAR) conserva 54 valores en rem: márgenes e iconos menores, que crecen hasta un 33 %.
  - El portal estático `public/` no forma parte de este estándar.

## 7. Limpieza de fuentes (2026-10-01)

- **Eliminados**, sin referencias en el código:
  - `Montserrat-Thin`, `-SemiBold`, `-ExtraBold`, `-Black` en woff;
  - `Montserrat-Medium.woff` (111 KB), reemplazado por el subconjunto woff2 de 43 KB;
  - los TTF `Thin`, `Regular`, `Bold`, `SemiBold`, `ExtraBold` y `Black`.
- **Se conservan** en `resources/fonts` los TTF `Montserrat-Light`, `-LightItalic` y `-Medium`, que usan los PDF (`Constantes.fuentePdf`, `CertificadosVotacionPDF`, `ActaEController`), y la licencia `OFL.txt`.
- **Regenerar Medium:** script `docs/herramientas/generar-medium-woff2.js` (requiere `npm install subset-font` en una carpeta temporal) con `subset-font` (HarfBuzz); toma los mismos caracteres (cmap) que el archivo Regular v12.

## 8. Revisión visual (2026-10-01, https://tribunal.local)

- **Verificado en vivo:**
  - raíz de 16px; cuerpo y datos en 13px Regular; metadatos en 12px Regular gris; encabezados de tabla en 11px Medium con espaciado de .04em;
  - Light y Bold no se descargan, porque no se usan;
  - el menú pasa a escritorio en 992px;
  - sin desbordamiento de página en 375, 576, 768, 991, 992, 1200, 1280 y 1440px;
  - con la letra «Grande» del navegador (raíz de 20px) todo escala sin desbordar.
- **Corregido después de la revisión** (`tribunal-globals.css`, `padron.css`):
  1. `p:tag` seguía sólido: la regla del tema por severidad era más específica que la regla base.
  2. Columnas de acciones aplastadas (10–22px): PrimeFaces usa `table-layout:fixed` por defecto; ahora las tablas sin scroll usan `auto`.
  3. Nombre partido letra a letra entre 992 y 1280px: la columna principal tiene un mínimo de 12rem y la tabla se desplaza dentro de su contenedor (desde 641px).
  4. Desbordamiento de página en mjrv y padrón: `p:selectOneMenu` imponía un `min-width` de 520px (la opción más larga).
  5. Etiqueta «Nombre» visible en las tarjetas móviles: la regla de PrimeFaces tenía la misma especificidad y se cargaba después. Defecto previo a esta migración.
  6. Recinto y Ubicación de padrón en ~50px: la tabla de mesas tiene un mínimo de 44rem.
  7. Interlineado de los títulos de página y diálogo: pasa a `--tec-lh-title` (1.2).
8. `p:treeTable` (menu, catálogos, permisos) con layout fijo: la columna de acciones medía 35px. Ahora usan layout automático y no tienen cuadrícula, igual que las demás tablas.

- **Segunda revisión** (992px, con las correcciones inyectadas):
  - sin desbordamiento de página ni celdas cortadas en dashboard, cronograma, candidatos, procesos, reportesMesa, roles, autoridades, plantillaCorreos, catálogos, permisos, reportePadron y escrutinios;
  - diálogo «Nueva iglesia»: 672px (md) en escritorio y 351px en móvil, sin desbordamiento; título 15px Medium, campos y botones 13px, texto auxiliar 11px.
  - Sin acceso con el usuario de prueba: listas, mesas, periodos, asignacionUsuarios y perfil.

- **Tercera revisión (2026-10-01):**
  - 20 pantallas en 375, 576, 768, 1200 y 1440px mediante iframes ocultos;
  - contraste WCAG de todo el texto visible en las mismas 20 pantallas (unos 1.800 textos);
  - selector A±: los cuatro pasos escalan en proporción.
- **Corregido:**
  9. **Contraste del gris secundario:** `--text-color-secondary` (#6c757d) no llegaba a 4,5:1 sobre fondos grises o tintados (migas de pan 4,45; tarjetas de fase 4,35; fila seleccionada 3,87). Pasa a #5f6870 en `theme.css`: 5,67 sobre blanco y 4,68 sobre la fila seleccionada.
  10. **Título del menú:** #828282 (3,84) → gris secundario en el menú claro; #656565 → #8f8f8f (6,3) en el oscuro.
  11. **Rol de la barra superior:** medía 8,7px y tenía 4,47:1. Ahora mide mínimo 10px (3xs) y el rol «Administrador» usa `--yellow-900` (7,1).
  12. **Colores de la paleta en vistas:** 84 utilidades (`text-red-500`, `text-orange-500`, `text-green-600`…) pasan a `tec-text-danger/warn/success/info/primary`. Por ejemplo, las cifras naranjas de mjrv y padrón tenían 2,70:1. Excluidas las páginas sin CSS global (`resultados`, `claveActualizada`, `requisitosClave`).
  13. **Barras de progreso:** PrimeFaces escribe `display:block` inline en la etiqueta cuando hay `labelTemplate`, y el «3 %» de padrón quedaba sobre una barra de 6px. Ahora `display:none !important` en `.tec-progress`. Escrutinios y dashboard usan `tec-progress`; en escrutinios el porcentaje pasa a texto visible.
  14. **SunEditor (plantillaCorreos, 375px):** un tooltip oculto ocupaba espacio y desbordaba la página; ahora solo se muestra al pasar el cursor.
  15. **Botón de perfil:** el layout imponía 20px de padding con la misma especificidad; ahora se aplica el estándar.
16. **Barra superior sin buscador (pedido del usuario):**
    - Se eliminó el buscador del menú (`toopbar.xhtml`) y sus estilos.
    - En pantallas menores de 992px la barra pasa de dos filas (120px) a una (60px): el perfil sube junto al logo y el desplegable de usuario, el menú lateral móvil y el panel derecho se reubican desde 60px (`layout-tribunal.css`).
    - Verificado en 375 y 768px.
    - Se eliminó también el código del filtro, que no se usaba en otro lugar: en `LoginBean`, 4 campos, 10 métodos y 8 imports (`inicializarMenuAutorizado`, que llama `LoginController`, se conserva simplificado); el aviso «sin resultados» de `WEB-INF/menu.xhtml` y sus estilos; y las claves `topbar.search.*` y `menu.busqueda.sinResultados`.
