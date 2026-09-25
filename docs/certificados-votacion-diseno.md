# Certificados de votacion: diseno e impresion

## Flujo existente

`reportesMesa.xhtml` (menu contextual) -> `ReporteMesaController` ->
`ReporteMesaService.generarCertificados` -> `ReportePFD.generarCertificadosVotacion`
-> `CertificadosVotacionPDF`. Los estilos y JavaScript de la pantalla no
componen el PDF. Se conserva la persistencia/versionado documental existente.

La consulta DTO del padron filtra proceso, mesa, persona, iglesia y relacion
activos y habilitacion electoral. La fecha procede de SUFRAGIO, no del reloj.
Las ubicaciones de CertificadoVotacionDTO pertenecen a la iglesia;
RecintoDTO ya aporta provincia, canton y parroquia del recinto.

## Composicion

- A4 vertical: dos columnas, cinco filas, tarjetas de 75 x 50 mm.
- Margenes de la grilla centrada: aproximadamente 30 mm horizontal y
  23.5 mm vertical. Corte discontinuo, borde interior a 3 pt.
- Doble cara con columnas invertidas en reverso para borde largo; escala 100%.
- Montserrat Regular/Bold incrustadas. Azul institucional, grises y blanco.
- Anverso: logo TEC proporcional, institucion, titulo, proceso, nombre completo,
  identificacion, recinto, mesa, fecha explicita, QR y espacio de Secretaria.
- Reverso: ubicacion electoral diferenciada de iglesia/comunidad, texto de
  acreditacion, espacio para firma JRV, Code 128 e identificador legible.
- Recursos existentes: cert-logo.png, cert-silueta.png y cert-onda.png.
  Marca de agua atenuada, curvas vectoriales suaves y pie fino sin tapar texto.

## Codigos y limites

No cambia el algoritmo de 40 digitos ni el payload de QR. El QR pasa de 20 a
52 pt (18.3 mm), con espacio blanco. El Code 128 conserva modulo de 0.7 pt,
sin escalado horizontal. No se rota: su ancho supera la altura disponible
en una tarjeta de 50 mm. Altura de barras: 15 pt; verificar con impresora real.

El identificador SHA-256 truncado es estable, no una firma digital ni una
prueba de sufragio. El QR mantiene la URL configurada en
reportesMesa.certificados.verificacion.url. No se encontro en TEC un endpoint
que valide esos certificados: la verificacion externa debe confirmarse antes
de anunciar autenticidad automatica. No se inventaron firmas, nombres de
Secretaria, sellos ni fechas. La impresion desde padron no acredita por si
sola que el titular voto; se conserva el flujo de entrega/acreditacion manual.

## Validacion

CertificadosVotacionPdfCheck usa datos ficticios, proceso con nombre y
ubicaciones de iglesia/recinto diferentes. Verifica 1, 10, 11 y 21 personas,
fecha, codigos visibles, fuentes, A4, dimensiones y alineacion duplex.
Admite un PNG del reverso a 300 dpi para comparar todos los modulos de barras.
La revision de PDF no sustituye una prueba de impresion/recorte en papel.
