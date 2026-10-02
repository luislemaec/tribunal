# Auditoría tipográfica de los PDF (2026-10-02)

Fuentes disponibles hoy para PDF: Montserrat **Light 300**, **Light Italic**, **Medium 500** (TTF en
`webapp/resources/fonts`) y **Bold 700** (TTF en `resources/fonts`, solo plantilla A4). En la web existen además
WOFF 400 y 700 (`ecuador-layout/fonts`); **no existe 600 (SemiBold)** en el proyecto.

Hallazgo sistémico: `Constantes.fuentePdf` asigna **Light 300** a todo texto `NORMAL` y **Medium 500** a todo
`BOLD`. Por tanto todos los datos, valores, celdas, párrafos, notas y metadatos de los PDF se imprimen en Light,
y no hay forma de expresar 400, 600 ni 700 en el contenido.

Colores: el contenido usa azul `#185285` (24,82,133) repetido en 5 clases; la plantilla A4 usa `#004385`
(medido en A4TEC.png). Secundarios distintos por clase: `#5A646E`, `#46525F`, `#69768A`, `#1E242A`.

## Inventario

Abreviaturas: L = Light 300, M = Medium 500, B = Bold 700, LI = Light Italic. Recomendación según la
referencia del usuario: texto/datos 400 · etiquetas y datos destacados 500 · secciones 600 · títulos 600–700 ·
secundario ≥400.

### Certificado de votación (`CertificadosVotacionPDF`, tarjeta 75 × 50 mm, 10 por hoja)

| Elemento | Fuente | Tamaño | Peso | Color | Función | Recomendación |
|---|---|---|---|---|---|---|
| «TRIBUNAL ELECTORAL» / «CONPOCIIECH» | Montserrat | 5 | M | #185285 | Institución | 5 · 600 |
| «CERTIFICADO DE VOTACIÓN» | Montserrat | 9,4 | M | #185285 | Tipo de documento | 9,4 · 700 |
| Proceso («ELECCIONES 2026») | Montserrat | 5,6 (se reduce) | L | #69768A | Subtítulo | 5,6 · 500 |
| «El Tribunal… certifica que:» | Montserrat | 4,7 | L | #69768A | Fórmula | 5 · 400 |
| Etiquetas (NOMBRES:, CÉDULA:, …) | Montserrat | 4,8 | M | #185285 | Etiqueta | 4,8 · 500 (sin cambio) |
| Nombre y cédula | Montserrat | 5 (≥4,8) | M | #1E242A | Dato destacado | 5,4 · 600 |
| Provincia, cantón, parroquia, recinto | Montserrat | 5 (≥4,8) | L | #1E242A | Valor | 5 · 400 |
| MESA / FECHA DE SUFRAGIO (etiqueta) | Montserrat | 4,7 | M | #185285 | Etiqueta | 4,8 · 500 |
| Mesa / fecha (valor) | Montserrat | 5 | M | #1E242A | Dato destacado | 5,4 · 600 |
| QR + «CONSULTA QR» | — / Montserrat | 32 pt / 4 | M | #185285 | Consulta externa | **Eliminar** (pedido) |
| Reverso: «INFORMACIÓN ELECTORAL» | Montserrat | 6,3 | M | #185285 | Sección | 6,3 · 600 |
| Reverso: Iglesia, Comunidad (etiqueta) | Montserrat | 4,7 | M | #185285 | Etiqueta | 4,8 · 500 |
| Reverso: iglesia / comunidad (valor) | Montserrat | 5,8 | L | #1E242A | Valor | 5,8 · 400 |
| Reverso: «Este documento acredita…» | Montserrat | 5,4 | L | #1E242A | Texto | 5,4 · 400 |
| Reverso: «F. PRESIDENTA/E DE LA JRV» | Montserrat | 5,1 | M | #185285 | Firma | 5,1 · 500 (sin cambio) |
| Reverso: Code 128 | — | — | — | negro | Identificador | Sin cambio |

### Plantilla A4 (`PlantillaA4`): actas, padrones y reportes verticales

| Elemento | Fuente | Tamaño | Peso | Color | Función | Recomendación |
|---|---|---|---|---|---|---|
| «TRIBUNAL ELECTORAL» (bajo logotipo) | Montserrat | 9,9 (espaciado) | L | #004385 | Institución (gráfica) | Mantener (réplica del diseño, 9,9 pt) |
| «CONPOCIIECH» / «DOCUMENTO OFICIAL» | Montserrat | 9–10 | B | #004385 | Institución | Mantener |
| «Elecciones 2026» / lema / contacto | Montserrat | 6–10 | M | #004385 | Institución | Mantener |
| Título del documento (bajo la línea) | Montserrat | 9,5 | M | #46525F | Tipo de documento | 9,5 · 600 |
| Código y fecha (derecha) | Montserrat | 7,5 | L | #46525F | Metadato | 7,5 · 400 |
| Paginación y código de validación | Montserrat | 7 | L | #46525F | Pie / metadato | 7 · 400 |

### Acta parcial de escrutinio (`ReportePFD.generarFormularioActaParcial`)

| Elemento | Tamaño | Peso | Color | Función | Recomendación |
|---|---|---|---|---|---|
| «ACTA PARCIAL DE ESCRUTINIO» | 15 | M | #185285 | Tipo de documento | 15 · 700 |
| Proceso | 9 | L | #5A646E | Subtítulo | 9 · 500 |
| Párrafo introductorio | 9 | L / M destacados | negro | Texto + datos | 9 · 400 / 600 |
| «RESULTADOS DE VOTACIÓN» | 10 | M | #185285 | Sección | 10 · 600 |
| Cabeceras de tabla | 8 | M | #185285 | Encabezado de tabla | 8 · 600 |
| Celdas (listas, votos) | 8 | L / M | negro | Valor / total | 8 · 400 / 600 |
| Etiquetas con línea («Observaciones:») | 8,5 | M | #185285 | Etiqueta | 8,5 · 500 (sin cambio) |
| Fórmula de cierre | 9 | L | negro | Texto | 9 · 400 |
| Firmas: línea / nombre / cargo | 8 | L / M | negro | Firma | nombre 8 · 600, cargo 8 · 400 |

### Acta de inscripción / Acta de actualización de miembros

| Elemento | Tamaño | Peso | Color | Función | Recomendación |
|---|---|---|---|---|---|
| Título | 15 / 14 | M | #185285 | Tipo de documento | 15 / 14 · 700 |
| Subtítulos de sección | 10 / 9 | M | negro | Sección | 10 / 9 · 600, azul institucional |
| Texto y celdas | 9 / 8 | L | negro | Texto / valor | 9 / 8 · 400 |
| Texto pequeño | 8 | L | gris oscuro | Nota | 8 · 400 |

### Reportes genéricos (`ReportePFD.nuevoPDF`, padrón de personas, listados; `HeaderFooterPageEvent` apaisado)

| Elemento | Tamaño | Peso | Color | Función | Recomendación |
|---|---|---|---|---|---|
| Barra de título de tabla | 9 | M | blanco | Encabezado de tabla | 9 · 600 |
| Encabezados de columna | 8 / 9–10 | M | #185285 / negro | Encabezado de tabla | 8 · 600 |
| Celdas | 8–9 | L | negro | Valor | 8 · 400 |
| Título de sección | 11 | M | #185285 | Sección | 11 · 600 |
| Párrafos | 9 | L | negro | Texto | 9 · 400 |
| «Documento generado por… Fecha… Hora…» | 8 | LI | negro | Metadato | 8 · 400 itálica → 400 (sin itálica Light) |
| Observación | 8 | LI | rojo | Nota | 8 · 400 |
| Apaisado: institución / título | 11–13 | M | #185285 | Institución / documento | 600–700 |
| Apaisado: sistema, código, fecha | 8 | L | #5A646E | Metadato | 8 · 400 |
| Apaisado: pie | 7 | L | #5A646E | Pie | 7 · 400 |

### Acta de escrutinio por HTML (`ActaEController` + XMLWorker, `Constantes.getHojaEstilo`)

| Elemento | Fuente declarada | Problema |
|---|---|---|
| `p` | montsR (Light) 10 pt | Light en texto oficial |
| `h1`, `h3` | **montsB** | Alias nunca registrado → se imprime en **Helvetica** |
| `#url` | **montsSB** | Alias nunca registrado → Helvetica |

## Rasterizado y vectorial

- `A4TEC.png`: convertido (bandas y líneas vectoriales, textos reales); solo los logotipos TEC quedan como imagen.
- `bannerHeader.png` / `bannerFooter.png` (apaisado): franjas de color de 298 / 292 bytes → convertibles a
  rectángulos vectoriales (pendiente de medir colores).
- `cert-onda.png`, `cert-silueta.png`, `cert-logo.png`: ilustraciones/logotipo sin versión vectorial → imagen.
- `logo_consejo_417x150.png`: logotipo → imagen.

## Incrustación y reutilización de fuentes

- `Constantes.fuentePdf`: registro una vez por JVM, subconjunto incrustado — correcto.
- `CertificadosVotacionPDF`: `BaseFont` una vez por documento — correcto.
- `PlantillaA4`: Bold creada por documento desde bytes en memoria — correcto, se puede cachear por JVM.
- XMLWorker (acta HTML): alias montsB/montsSB inexistentes → Helvetica no incrustada.

## Cambios aplicados (2026-10-02)

Decisiones del usuario: azul unificado a `#004385`; el peso 600 se resuelve con Medium 500; mover Mesa y Fecha
al quitar el QR; aplicar tokens, jerarquía, corrección de Helvetica y banners vectoriales.

- **`itext/TipografiaPdf`**: única fuente de verdad de fuentes (Light/Regular/Medium/Bold desde
  `src/main/resources/fonts`, una `BaseFont` por peso y JVM, subconjunto incrustado), colores (`AZUL #004385`,
  `TEXTO_PRINCIPAL`, `SECUNDARIO`, `FONDO_CABECERA`, `BORDE`, `ALERTA`), tamaños base y estilos por función
  (`INSTITUCION`, `TITULO_DOCUMENTO`, `SUBTITULO`, `SECCION`, `ETIQUETA`, `VALOR`, `VALOR_DESTACADO`, `TEXTO`,
  `ENCABEZADO_TABLA`, `CELDA`, `METADATO`, `NOTA`, `FIRMA`, `PIE`) y proveedor de fuentes para el XMLWorker.
- **`Constantes.fuentePdf`** delega en `TipografiaPdf`: normal → Regular 400 (antes Light 300), énfasis → Medium
  500; la cursiva Light deja de usarse. Montserrat Regular convertida desde la WOFF del proyecto.
- **Certificado**: sin QR (imagen, leyenda «CONSULTA QR», recuadro y URL; claves `consulta`, `verificacion.url`,
  `verifica` y `sitio` retiradas). El código del certificado se conserva porque alimenta el Code 128 del reverso.
  Mesa y Fecha alineadas con la columna de etiquetas. Jerarquía: título 700; institución, proceso, etiquetas,
  sección, nombre, cédula, mesa y fecha 500; valores y textos 400; etiquetas alineadas a la línea base del valor.
- **Actas y reportes**: títulos de documento 700; secciones, etiquetas, encabezados de tabla, datos destacados y
  nombre de quien firma 500; texto, celdas, cargo, metadatos y notas 400.
- **Acta HTML**: `montsR`/`montsSB`/`montsB` → Regular/Medium/Bold; ya no hay Helvetica en ningún PDF.
- **Apaisado**: franjas del encabezado y del pie dibujadas como rectángulos con los colores medidos;
  `bannerHeader.png` y `bannerFooter.png` retirados. Se eliminó la variante «acta parcial», sin uso.
- Light 300 queda solo en la réplica gráfica de la plantilla A4 («TRIBUNAL ELECTORAL»).

Verificación: `MaquetacionPdfCheck` (15 documentos, 36 páginas, 3 903 elementos en zona segura; actas parciales
de 1 a 9 listas en una página) y `CertificadosVotacionPdfCheck` (1, 10, 11 y 21 personas; dos aserciones que ya
fallaban antes se actualizaron al diseño vigente). Fuentes de todos los PDF: solo Montserrat.

| Documento | Antes | Después |
|---|---|---|
| Plantilla A4, 1 / 100 páginas | 104 / 159 KB | 110 / 164 KB (+ Regular 400) |
| Certificados, 1 / 100 hojas | 206 / 2 469 KB | 196 / 1 229 KB (sin QR) |

Pendiente de decisión: `cert-onda.png` («TRANSPARENCIA | PARTICIPACIÓN | DEMOCRACIA») y `cert-silueta.png`
(«TU VOTO FORTALECE EL ECUADOR») llevan texto dentro de la ilustración; pasarlo a texto real requiere versiones de
las ilustraciones sin texto.
