# Auditoría de imágenes y recursos en los PDF (2026-10-02)

Medido fuera del servidor con el código real (`javac` sobre `src/main/java`, iText 5.5.13.3, JDK 21) y las
fuentes Montserrat del proyecto. Herramientas de medición: `BenchPlantilla` y `BenchCertificados` (scratchpad
de la sesión); 5 repeticiones por medida en plantilla, 1 en certificados, tras calentamiento.

## 1. Inventario

| PDF / clase generadora | Imagen | Tamaño | Páginas | Forma de carga | ¿Reutiliza o repite? |
|---|---|---|---|---|---|
| Actas e informes A4 vertical (`PdfInstitucional.crearA4`, `crearA4ConCodigoBarras`, `crearA4ActaParcial` → `PlantillaA4.Fondo`): ActaInscripcion, ActaActualizacionMiembros, acta parcial y reportes de `ReportePFD` | `resources/img/A4TEC.png` (2480×3508, RGBA, entrelazado) | 286 KB | todas (fondo, `onEndPage`) | bytes leídos 1 vez por JVM (classpath); `Image.getInstance` 1 vez por documento | **Reutiliza**: un solo XObject por PDF; las páginas siguientes lo referencian |
| Mismo | Código de barras Code128 | vectorial | todas | 1 vez por documento | Reutiliza |
| A4 apaisado (`PdfInstitucional.crearA4Horizontal` → `itext.HeaderFooterPageEvent`): `ReportePFD.nuevoPDFHorizontal` (ProcesoController) | `bannerHeader.png`, `logo_consejo_417x150.png`, `bannerFooter.png` | 0,3 + 24,4 + 0,3 KB | todas | `Image.getInstance(ruta)` **en cada página** (`onStartPage`/`onEndPage`), ruta física vía `FacesContext` | **Repite**: una copia del logotipo (~25 KB) por página (estimado; no medible fuera del servidor) |
| Certificados de votación (`CertificadosVotacionPDF`, 10 por hoja) | `cert-silueta.png`, `cert-onda.png`, `cert-logo.png` | 75 + 59 + 24 KB | todas, 10 veces por hoja | bytes en memoria 1 vez por documento; `Image.getInstance(bytes)` **en cada certificado** | **Repite**: 3 imágenes × 10 certificados por hoja |
| Certificados | QR de validación | por certificado | todas | 1 por certificado | Único por persona (correcto) |
| `ReportePFD.addImagen` | ruta recibida | — | — | — | Método sin uso |
| `util.HeaderFooterPageEvent`, `HeaderFooterPageEventDraft`, `HeaderFooterHorizontalPageEvent` | banners y logo | — | — | por página | Clases sin uso |

Fuentes: `Constantes.fuentePdf` registra Montserrat una vez por JVM (`FontFactory.isRegistered`) y la incrusta como
subconjunto — correcto. `ActaEController` (línea ~1613) vuelve a registrar `montsR` en cada acta (re-lee el TTF):
menor, se corrige con la misma guarda. `CertificadosVotacionPDF` crea sus `BaseFont` una vez por documento — correcto.

## 2. Mediciones de línea base

### Plantilla A4 (`PdfInstitucional.crearA4`)

| Páginas | Tamaño | Tiempo | Pico de heap |
|---|---|---|---|
| 1 | 239 KB | ~200 ms | +118 MB |
| 10 | 245 KB | ~360 ms | +183 MB |
| 50 | 267 KB | ~210 ms | +161 MB |
| 100 | 296 KB | ~210 ms | +162 MB |

La plantilla **no** multiplica el peso del archivo (+0,6 KB/página por texto y paginación). El costo real está en
cada documento: decodificar y recomprimir 8,7 Mpx (~200 ms y ~120-180 MB de heap por PDF, aunque tenga 1 página).

### Certificados de votación (10 por hoja)

| Hojas | Antes | Reutilizando las imágenes (prototipo) |
|---|---|---|
| 1 | 1 646 KB · 43 ms · +18 MB | 206 KB · 36 ms · +8 MB |
| 10 | 16 249 KB · 205 ms · +134 MB | 411 KB · 217 ms · +18 MB |
| 50 | 81 159 KB · 641 ms · +363 MB | 1 325 KB · 799 ms · +91 MB |
| 100 | 162 298 KB · 1 926 ms · +792 MB | 2 469 KB · 1 962 ms · +99 MB |

Mismas instrucciones de dibujo; solo cambia que las tres imágenes se crean una vez por documento.

## 3. Elementos de `A4TEC.png` (2480×3508 px = A4 a 300 ppp; 1 px = 0,24 pt)

| Elemento | Medida (px → pt) | Color medido | Conversión posible |
|---|---|---|---|
| Banda izquierda | x 0–209 → 0–50,2 pt, alto completo | `#003B8D` sólido hasta y=1802 px; degradado hasta `#3166AA` en el borde inferior (no lineal: muestras cada 100 px) | Primitiva: rectángulo + sombreado axial con paradas medidas |
| Línea vertical gris | x 239–249 → 57,4–59,8 pt, alto completo | `#131F27` al 25 % de opacidad | Primitiva: rectángulo con `PdfGState` 0,25 |
| Línea del encabezado | y 577–583, x 480–2288 → 1,4 pt de grosor | `#122028` opaco | Primitiva |
| Línea del pie | y 3372–3378, x 480–2288 | `#111F28` al 35 % | Primitiva con `PdfGState` 0,35 |
| «DOCUMENTO OFICIAL», «Elecciones 2026», «TRIBUNAL ELECTORAL», «CONPOCIIECH», pie «Tribunal Electoral Conpociiech • Transparencia y Democracia», «Contacto: 098 607 6345» | ver bbox en la sesión | `#004385` | Texto real — **tipografía por confirmar** (parece Poppins; el proyecto solo tiene Montserrat) |
| Logotipo TEC (letras con degradado y check) | x 449–1046, y 171–469 | degradados azules | Sin fuente vectorial en el repositorio: raster recortado y optimizado, o SVG si existe |
| Marca de agua (logotipo + texto) | x 504–2115, y 1380–2185 | colores del logotipo al 9 % (alfa 23/255) | La misma imagen del logotipo con `PdfGState` 0,09, o vectorial si hay SVG |

iText 5 no interpreta SVG: usarlo exige añadir Apache Batik o convertir los trazados a `PdfTemplate`.

## 4. Cambios aplicados (2026-10-02)

### Fase 1 — carga de recursos (sin cambio visual)

- `CertificadosVotacionPDF`: logotipo, onda y silueta se crean una vez por documento y se reutilizan en cada
  certificado. Comparación del contenido de dibujo página a página contra la versión anterior: 0 diferencias.
- `itext.HeaderFooterPageEvent` (A4 apaisado): banners y logotipo se cargan una vez por documento.
- `ActaEController`: la fuente `montsR` se registra solo si no lo está.

### Fase 2 — plantilla A4 vectorial (`PlantillaA4`)

- La plantilla se compone una vez por documento en un `PdfTemplate` que todas las páginas reutilizan.
- Banda: sombreado axial con 18 paradas medidas; líneas: rectángulos con la opacidad original; viñeta del pie:
  círculo vectorial.
- Textos reales en Montserrat (decisión del usuario): Light «TRIBUNAL ELECTORAL»; Medium «Elecciones 2026»,
  lema y contacto; Bold «DOCUMENTO OFICIAL», «CONPOCIIECH» e institución del pie. Cuerpo calculado por la altura de
  mayúscula medida y espaciado entre letras ajustado al ancho medido de cada texto. «Elecciones 2026» se mantiene fijo.
- Montserrat Bold (700): convertida a TTF desde la WOFF del proyecto (`docs/herramientas/woff-a-ttf.js`) en
  `src/main/resources/fonts/`, con su licencia OFL.
- Logotipos (sin versión vectorial): recortados de A4TEC.png sin pérdida (`docs/herramientas/recortar-plantilla-a4.js`):
  `plantilla-a4-logo.png` 586×194 px (21,5 KB) y `plantilla-a4-marca-agua.png` 1580×516 px (42,5 KB).
- Comparación renderizada con pdf.js a 300 ppp (diferencia media por canal, 0–255): banda 1,6; línea gris 1,7;
  línea del encabezado 3,0; logotipo 1,2; letras de la marca de agua 0,02; zonas vacías 0. Los textos difieren solo
  en el dibujo de las letras (Montserrat frente a la fuente original de la imagen); posición, tamaño, peso y color
  coinciden.

| Páginas | Antes (A4TEC.png) | Después (vectorial) |
|---|---|---|
| 1 | 239 KB · 196 ms · 169 MB asignados | 104 KB · 31 ms · 19 MB asignados |
| 10 | 245 KB · 418 ms · 169 MB | 109 KB · 33 ms · 19 MB |
| 50 | 267 KB · 575 ms · 171 MB | 131 KB · 40 ms · 21 MB |
| 100 | 295 KB · 543 ms · 173 MB | 159 KB · 36 ms · 23 MB |

«Memoria asignada» = bytes reservados por el hilo para generar un documento (medida estable; el pico de heap
depende del recolector).

## 5. Limpieza (2026-10-02)

- `A4TEC.png` pasa a `docs/diseno/A4TEC.png`: ya no se usa al generar PDF y deja de viajar en el WAR (−286 KB).
  Sigue siendo la fuente de `docs/herramientas/recortar-plantilla-a4.js` (verificado: recortes idénticos).
- Eliminadas las clases sin uso `util.HeaderFooterPageEvent`, `itext.HeaderFooterPageEventDraft`,
  `itext.HeaderFooterHorizontalPageEvent` y el método `ReportePFD.addImagen`.
- `MaquetacionPdfCheck` (src/test): omite la plantilla como Form XObject de página completa, igual que antes omitía
  la imagen de fondo. Resultado: 15 documentos, 36 páginas, 3 788 elementos dentro de la zona segura.

## 6. Ajuste de la línea vertical (2026-10-02)

- Línea gris vertical de la plantilla A4: de 2,64 pt a **1 pt**, centrada en la misma posición (58,68 pt), mismo
  color y opacidad.
- `PlantillaA4.MARGEN_IZQUIERDO` pasa de 60 pt a **63,18 pt**: el contenido inicia 4 pt después del borde derecho de
  la línea. Se calcula desde la línea (no es un número suelto), y `ANCHO_UTIL` se ajusta solo.
- Verificado: zona segura OK (15 documentos, 36 páginas) y actas parciales de 1 a 9 listas en una página.
