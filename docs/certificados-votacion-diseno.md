# Certificados de votacion: diseno e impresion

## Flujo existente

`reportesMesa.xhtml` (menu contextual) -> `ReporteMesaController` ->
`ReporteMesaService.generarCertificados` -> `ReportePFD.generarCertificadosVotacion`
-> `CertificadosVotacionPDF`. Los estilos y JavaScript de la pantalla no
componen el PDF. Se conserva la persistencia/versionado documental existente.

La consulta DTO del padron filtra proceso, mesa, persona, iglesia y relacion
activos y habilitacion electoral. La fecha sigue procediendo de SUFRAGIO en
el flujo de negocio, pero ya no se imprime en el certificado.
Las ubicaciones de CertificadoVotacionDTO pertenecen a la iglesia;
RecintoDTO ya aporta provincia, canton y parroquia del recinto.

## Composicion

- A4 vertical: dos columnas, cinco filas, tarjetas de 75 x 50 mm.
- Margenes de la grilla centrada: aproximadamente 30 mm horizontal y
  23.5 mm vertical. Corte discontinuo, borde interior a 3 pt.
- Doble cara con columnas invertidas en reverso para borde largo; escala 100%.
- Montserrat Regular/Medium/Bold incrustadas. Azul institucional, grises y blanco.
- Anverso: logo TEC completo y proporcional, titulo/proceso alineados a su
  altura, nombre en una linea, cedula (hasta diez caracteres), recinto, mesa
  y ubicacion electoral del recinto. Sin fecha de sufragio impresa.
- Reverso: iglesia/comunidad, texto de acreditacion, espacio para firma JRV
  y Code 128 sin identificador impreso como texto. No contiene QR.
- Logo fuente: `src/main/resources/img/logo-tec-azul.svg`, suministrado por el
  usuario (7640 bytes). `LogoTecVectorial` convierte sus trazados absolutos y
  degradado a PDF nativo: un Form XObject por documento, sin rasterizar.
  Incluye las letras institucionales del recurso oficial; no se repiten al lado.
  Adaptador especifico, no lector SVG general.
- Fondo: `plantilla-a4-marca-agua.png` de 67,5 pt de ancho y mapa vectorial
  `ecuador-continental.svg` de 43 pt de alto, lado a lado, con el conjunto
  centrado en ambos ejes. PNG reutilizado una vez por documento; su alfa
  original (23/255) se compensa para obtener 7 % efectivo. Mapa al 3,5 %.
  El reverso mantiene libre la firma y el codigo de barras, sin marca de agua.
- Seis curvas Bezier completas: dos familias de tres, de borde a borde,
  grosores 0,22/0,32 pt y separacion 2,4 pt. Degradado vectorial azul
  institucional -> azul claro -> azul institucional, con opacidad de trazo
  del 18 %. Usa AZUL/FONDO_CABECERA de TipografiaPdf, sin colores nuevos.
  Patron compuesto una vez por documento y reutilizado en ambas caras.
  Recortadas al borde interior, conservan libres las barras y su zona blanca.
  La unica separacion azul va inmediatamente bajo el proceso, desde el final
  del logo al margen derecho; la frase de certificacion sube a la posicion
  anterior de la linea. Se elimina la linea bajo la cedula. Sin amarillo ni rojo.
  Los colores usan los tokens existentes de TipografiaPdf.
- Curvas y pie son vectoriales. El encabezado no usa PNG; el unico raster
  del anverso es la marca de agua oficial solicitada.

## Codigos y limites

No cambia el algoritmo de 40 digitos. El Code 128 conserva modulo de 0.7 pt,
sin escalado horizontal. No se rota: su ancho supera la altura disponible
en una tarjeta de 50 mm. Altura de barras: 15 pt; verificar con impresora real.

El identificador SHA-256 truncado es estable, no una firma digital ni una
prueba de sufragio. No se anuncia verificacion automatica. No se inventaron firmas, nombres de
Secretaria, sellos ni fechas. La impresion desde padron no acredita por si
sola que el titular voto; se conserva el flujo de entrega/acreditacion manual.

## Validacion

CertificadosVotacionPdfCheck usa datos ficticios, proceso con nombre y
ubicaciones de iglesia/recinto diferentes. Verifica 1, 10, 11 y 21 personas,
ausencia de fecha impresa, nombres por palabras completas, fuentes, A4,
dimensiones, alineacion duplex y logo vectorial con degradado. El raster
del anverso se limita a la marca de agua oficial de 1580 x 516 px.
Admite un PNG del reverso a 300 dpi para comparar todos los modulos de barras.
La revision de PDF no sustituye una prueba de impresion/recorte en papel.

Revision visual realizada sobre el archivo vigente descargado de Achik Kausay,
mesa 1, y una muestra local con datos ficticios/nombres largos despues del cambio.
La muestra conserva diez tarjetas por hoja y caras separadas. No se regenero
el documento de la mesa ni se publico el cambio en WildFly.

Continuacion grafica: comparacion antes/despues con los mismos datos ficticios
en `target/certificado-review/comparacion.png`. Revision local adicional de diez
nombres/cedulas del archivo vigente de Achik Kausay: legibles y conservados;
la muestra utiliza IDs internos ficticios y no es un certificado emitido.
Se conservan 75 x 50 mm, grilla A4, tipografia y logica electoral.

Los nombres y apellidos se imprimen en una sola linea a 7 pt: si exceden el
ancho util se muestran solo las palabras completas que caben, sin puntos
suspensivos ni segunda linea. Se reduce el espacio entre nombre y cedula.
El DTO y los datos originales no se modifican.

Los datos usan cuatro filas con etiquetas separadas 20 pt: nombres, cedula,
recinto/mesa y provincia/canton/parroquia. Etiquetas encima de sus valores,
margenes comunes y tres columnas territoriales iguales. Recinto ocupa dos
columnas; mesa se alinea con parroquia. Se preserva espacio para dos lineas
en recinto y territorio, sin invadir el pie.

El adaptador del logo interpreta sus colores solidos como RGB opaco, evitando
el alfa cero del constructor ARGB de BaseColor. Asi se conserva el visto azul
y su contorno blanco exactamente como en el SVG oficial.
