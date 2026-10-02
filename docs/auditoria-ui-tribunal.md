# Auditoría de interfaz — identidad única Tribunal

Fecha: 2026-10-01. Alcance: `tribunal-globals.css`, `template.xhtml`/layout, los 11 CSS de página
(`resources/css/*.css`) y las vistas XHTML de `src/main/webapp` (sin `public/`, que es el portal estático).

Tema oficial único: `primefaces-tribunal/theme.css` + `ecuador-layout/css/layout-tribunal.css`
(`GuestPreferences` solo admite "tribunal"). Fuente: Montserrat. Raíz del layout: 12px.

## 1. Cifras

| Medida | Valor |
|---|---|
| `style="..."` en XHTML | 295 en 30 vistas. ~200 son anchos/alturas de columnas, diálogos y gráficos (layout, válidos inline); ~95 son visuales (color, fondo, borde, tamaño de letra) |
| Colores HEX/RGB en XHTML | 6 (`#fff` en 4 blockUI, `#f0fdf4`/`#fffbeb` en cronograma) |
| Colores de paleta para estados en XHTML (`--blue-50`, `--orange-500`…) | ~70 |
| `&nbsp;` | 0 |
| CSS de página con HEX/RGB | padron (4 respaldos en `var()`), actaE (1 respaldo), personas (1 sombra), resultados (2 sombras) |
| `!important` en CSS de página | 34 (dashboard 10, iglesias 8, acta-fisica 6, mjrv 5, personas 5) |
| Declaraciones `font-weight` numéricas en CSS de página | ~95 (iglesias 37, personas 27, actaE 12, procesos 11…) |
| `p:tag` | 125: 53 con `tec-status` (fondo suave), 72 con color sólido del tema |
| `p:dialog` | 31: 12 con `tec-dialog`, 18 con `width` fijo en px (12 valores distintos: 380–1240) |
| `p:dataTable` | 39 en dos familias: `tec-table` (11) y `ui-datatable-striped ui-datatable-sm` (24); 15 con `showGridlines` |
| Botones | 239; 14 con `ui-button-sm` y 1 con `ui-button-rounded`, clases que **no existen** en el tema |

## 2. Inventario

Leyenda de acción: **Hecho** (aplicado en esta fase) · **Propuesto** (pendiente, sin decisión abierta) · **Pregunta** (requiere su decisión).

| Elemento | Estilo actual | Origen | Inconsistencia | Variable / clase global propuesta | Acción |
|---|---|---|---|---|---|
| Colores de estado | Paleta directa (`--green-50/500/600/700`, `--orange-*`, `--blue-*`, `--red-*`) | globals, XHTML, CSS de página | El tema Tribunal ya define success/info/warning/danger (`--success-color`, `--success-text`…); la paleta no sigue esa semántica | `--tec-<sev>`, `--tec-<sev>-text`, `--tec-<sev>-bg` para primary, secondary, success, info, warn, danger (fondos con contraste AA verificado) | **Hecho** (tokens creados) |
| Pesos tipográficos | Números 300/400/500 repetidos | globals y CSS de página | Sin token; se repiten ~95 veces | `--tec-fw-light/regular/medium` | **Hecho** (tokens y reemplazo en todos los CSS salvo resultados.css) |
| Avisos (callout) | `.callout` global (fondo -100, borde -600 3px, texto de color) y 11 avisos inline (fondo -50, borde -500 4px, texto neutro) | globals; actaE, actaE-devolucion, mjrv, asignacionUsuarios | Dos estilos para la misma función; `callout--danger` solo en iglesias.css | `.callout` + `callout--info/success/warning/danger/neutral` con tokens | **Hecho** |
| Bloqueo de carga (`p:blockUI`) | Spinner 2rem/2.5rem en `--primary-700` o `--blue-500`; texto `#fff` inline | cronograma, escrutinios, plantillaCorreos, reportePadron | 3 tamaños/colores; spinner oscuro sobre velo oscuro | `.tec-overlay-spinner`, `.tec-overlay-text` | **Hecho** |
| Notificaciones (`p:growl`) | Borde izquierdo con paleta | globals | Igual que estados | Tokens `--tec-<sev>` | **Hecho** |
| Etiquetas `tec-status` | Paleta 50/700 | globals | Igual que estados | Tokens `--tec-<sev>-bg/-text` | **Hecho** |
| Clases de botón inexistentes | `ui-button-sm` (14), `ui-button-rounded` (1) | actaE, autoridades, candidatos, toopbar | No hacen nada | Eliminar | **Hecho** |
| Comentarios de multi-tema | "cada tema", "tema ecuador", "Yellow, Amber…" | globals | El sistema tiene un solo tema | Texto actualizado | **Hecho** |
| Escala tipográfica | xs 10px · sm 11px (= metadato) · md 12px | globals | Su indicación dice "metadatos usan xs = 11px" | Ver pregunta 1 | **Hecho**: renombrada (3xs 9 · 2xs 10 · xs 11 = metadatos · md 12…); ningún tamaño cambió |
| `p:tag` | 72 sólidos (tema) y 53 suaves (`tec-status`) | XHTML | La misma función (estado) con dos aspectos | Estilo suave global para todo `p:tag` | **Hecho**: estilo suave global para los 125 |
| `p:dataTable` | `tec-table` (densidad normal, sin rayado) vs `striped sm` (compacta, rayada); 15 con cuadrícula | XHTML | Dos aspectos de tabla | Un solo estándar | **Hecho**: las 39 compactas y rayadas, sin cuadrícula |
| `p:dialog` | 18 con `width` en px (380…1240) | XHTML | 12 anchos distintos, no responsivos | `tec-dialog` (40rem), `tec-dialog--sm` (34rem, hoy solo en personas.css), `tec-dialog--lg` (72rem) | **Hecho**: cuatro tamaños globales (sm 34 · base 40 · md 56 · lg 72rem); excepción: visor de acta física |
| Tarjetas KPI | 6 implementaciones: `iglesias-kpi`, `kpi-card`/`personas-kpi`, y tarjetas inline con `border-left:4px` en dashboard (14), escrutinios (4), cronograma (4), asignacionUsuarios (4) | CSS de página y XHTML | Misma función, aspecto distinto en cada pantalla | `.tec-kpi`, `__icon`, `__label`, `__value` y modificadores `--primary/success/warn/danger` | **Hecho** en dashboard, escrutinios, cronograma, asignacionUsuarios y recintos (30 tarjetas); personas e iglesias conservan su variante compacta con tokens |
| Iconos en caja de color | `style="width:2.75rem;height:2.75rem;background:var(--…)"` | asignacionUsuarios, escrutinios, cronograma, usuarios | Tamaños 2rem/2.25rem/2.75rem/2.8rem | `.tec-icon-badge` (+ `--sm`) con tokens | **Hecho** (`.tec-icon-badge`, `.tec-avatar`) |
| Iconos grandes decorativos | `font-size:1.75rem/2rem/2.5rem` inline | actaE, candidatos, mjrv, cronograma, claveActualizada, recuperaClaveCorrecto | 3 tamaños | `.tec-icon-hero` | **Hecho** (`.tec-icon-hero`); claveActualizada y recuperaClaveCorrecto quedan inline porque no cargan el CSS global |
| Tamaños de letra inline | `font-size:var(--tec-fs-xl)` (recintos ×4), `--tec-fs-lg`, `--tec-fs-2xs` | recintos, permisos, cronograma | Correcto en valor, pero inline | Clases `text-*` de la escala | **Hecho** |
| Monoespaciado | `style="font-family:monospace"` | usuarios, asignacionUsuarios | Existe `.tec-mono` | `.tec-mono` | **Hecho** |
| Cronograma | `#f0fdf4`, `#fffbeb`, fondos -50 y bordes -100 inline | cronograma.xhtml | HEX y paleta directa | Tokens `--tec-<sev>-bg` | **Hecho** (avisos → callout, iconos → tec-icon-badge) |
| Respaldos HEX en `var()` | `var(--surface-card, #fff)`, `var(--surface-border, #ddd)` | padron.css, actaE.css | El tema siempre define la variable | Quitar respaldo | **Hecho** |
| Sombras RGBA | `rgba(15,23,42,.05…)` | globals (`tec-card`, blockUI), personas.css, resultados.css | Valores sueltos | `--tec-shadow-sm` y `--tec-shadow-float` | **Hecho** (`--tec-shadow-card`, `--tec-overlay-bg`); resultados.css es excepción (no carga el CSS global) |
| `!important` en CSS de página | 34 | dashboard, iglesias, acta-fisica, mjrv, personas | Revisar uno a uno cuáles superan utilidades PrimeFlex | Mantener solo los necesarios | **Revisado**: los restantes son necesarios (anchos inline de PrimeFaces, reflow, speeddial); se eliminó el bloque duplicado de mjrv.css |
| Columnas ACCIONES | `tec-acciones` en 17 vistas; `tec-col-actions` solo en 4 | XHTML | Ancho y alineación distintos | `tec-col-actions` + `tec-acciones` en todas | **Hecho**: tec-col-actions en las 20 |
| Textos sin traducir | "Asignación deshabilitada", "Fase activa:", "Calculando escrutinio...", "Procesando cronograma..." | asignacionUsuarios, escrutinios, cronograma | Fuera de `messages_es.properties` | Claves de mensaje | **Propuesto** (fuera del alcance visual) |

## 3. Aplicado

- **Fase 1:**
  - tokens de severidad y pesos;
  - avisos `callout` (11 inline → clase);
  - bloqueos de carga;
  - growl;
  - clases de botón inexistentes;
  - comentarios de multi-tema.
- **Fase 2** (según las decisiones del 2026-10-01):
  - Escala renombrada: `--tec-fs-3xs` 9px, `--tec-fs-2xs` 10px, `--tec-fs-xs` 11px (metadatos), `md` 12px, `lg`, `xl`, `2xl`. Ningún tamaño cambió.
  - `p:tag`: estilo suave global con contraste AA; `secondary` ahora es neutro (antes salía azul sólido).
  - `p:dataTable`: las 39 en `ui-datatable-striped ui-datatable-sm`, sin `showGridlines` ni `size="small"`.
  - Tarjetas KPI, iconos en caja, avatares, iconos destacados, rótulo de sección del dashboard y barra de avance como clases globales (`tec-kpi`, `tec-icon-badge`, `tec-avatar`, `tec-icon-hero`, `tec-section-label`, `tec-progress`).
  - CSS de página: colores de paleta → tokens; pesos → `--tec-fw-*`; respaldos HEX eliminados; sombras con token; selección de filas de mjrv → `tec-table-seleccionable` global.
- **Verificación:**
  - XML bien formado en las 24 vistas modificadas; llaves balanceadas en todos los CSS.
  - Ningún atributo funcional (`rendered`, `process`, `update`, `action`, `value`, `id`…) cambió.
  - Finales de línea CRLF conservados.

## 4. Decisiones aplicadas (fase 3)

1. **Diálogos:** cuatro anchos globales: `tec-dialog--sm` 34rem, `tec-dialog` 40rem, `tec-dialog--md` 56rem y `tec-dialog--lg` 72rem.
   - Las reglas locales de iglesias.css y personas.css se eliminaron.
   - Los 17 diálogos con `width` fijo pasaron al tamaño más cercano: 380px a sm; de 480 a 560px a base; de 620 a 760px a md.
   - Excepción: el visor de acta física (reportesMesa, 1240px), que muestra imagen y datos lado a lado y tiene sus propias reglas responsivas.
2. **Columnas ACCIONES:** las 20 usan `tec-col-actions`, alineadas a la derecha y ajustadas al contenido, sin ancho fijo ni `text-center`.
3. **escrutinios.xhtml:** la tabla de votos vuelve al tamaño estándar (12px).

Verificación: las 27 vistas modificadas son XML válido, y los 48 atributos funcionales presentes en líneas cambiadas son idénticos antes y después.

## 5. Excepciones justificadas

- **Fuera de la plantilla:** `claveActualizada.xhtml`, `recuperaClaveCorrecto.xhtml`, `login.xhtml` y `resultados.xhtml` (con `resultados.css`) no cargan `tribunal-globals.css`; sus estilos inline o locales se mantienen.
- **Anchos inline:** anchos y alturas de columnas, diálogos y gráficos (~200 `style`) son de layout y se mantienen.
- **`!important` restantes:** son necesarios para imponerse a anchos que PrimeFaces escribe inline, al reflow móvil y al speeddial del dashboard.
